/*
 * (C) ActiveViam 2013-2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.chunk.IVectorChunk;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class in charge of cleaning optionally abandoned {@link IVectorChunk}s.
 * <p>
 * In situations like partition dropping in a transaction, it is possible to lose reference to living blocks of
 * vectors. Although the blocks will eventually be collected by GC, it does not release the acquired off-heap
 * memory.
 * <p>
 * One solution could be to provide every block vectors with a finalize method. However, this does not necessarily
 * play nice with manual counting.
 * <p>
 * The chosen solution is to track all allocated {@link IVectorChunk}s and ensure a proper cleaning in case they
 * disappear from reachable scope.
 *
 * @author ActiveViam
 */
public class VectorFinalizer extends WeakReference<IVectorChunk> {

	/*
	 * This reuses the pattern of java.lang.ref.Finalizer. It keeps a list of all chunks, to be able to clean them
	 * if needed on GC. To that purpose, a WeakReference will contain every registered ChunkVector, using a
	 * dedicated queue to detect when a chunk is GCed. To keep that WeakReference alive, we need a strong reference
	 * to it. To that effect, we create a chain of strong references as a linked list of all WeakReferences created
	 * in this class. Finally, as soon as the chunk is GCed, the WeakReference points to null, preventing us to
	 * access the chunk to clean. Thus, we have to somehow keep track on the objects to clean with retaining a
	 * strong reference to the chunk. This is achieved by keeping only ChunkVector#objects, as it is what must be
	 * cleaned. To simplify the whole design: - VectorFinalizer extends WeakReference - VectorFinalizer instance
	 * has a strong reference to the chunk inner elements to clean through the finalize action - VectorFinalizer
	 * instance is a linked entry to build the strong chain described above - VectorFinalizer class owns the
	 * reference queue
	 */

	/** Logger. */
	protected static final Logger logger = Logger.getLogger(VectorFinalizer.class.getName());

	/**
	 * Queue containing references to discarded {@link IVectorChunk}s.
	 * <p>
	 * Those references should only be of type {@VectorFinalizer}.
	 */
	protected static ReferenceQueue<IVectorChunk> vectorReferenceQueue = new ReferenceQueue<>();

	/**
	 * Static head of a strong chain of {@link VectorFinalizer} references.
	 */
	protected static VectorFinalizer head = null;

	/**
	 * Class lock for all operations on the finalizer linked list through {@link #head}.
	 */
	protected static final Object lock = new Object();

	/** Flag marking if the Vector finalizing thread is started. */
	protected static final AtomicBoolean started = new AtomicBoolean(false);

	/** Main thread running finalization of vector chunks. */
	protected static Thread finalizerThread;

	/**
	 * Pointer to the next element in the chain of finalizers.
	 * <p>
	 * Tail of the chain has no next element.
	 */
	protected VectorFinalizer nextFinalizer = null;

	/**
	 * Pointer to the previous element in the chain of finalizers.
	 * <p>
	 * Head of the chain has no previous element.
	 */
	protected VectorFinalizer previousFinalizer = null;

	/** Vector array of a chunk to clean on GC. */
	protected final Runnable finalizeAction;

	/**
	 * Gets if a finalizer has already been executed.
	 *
	 * @return {@code true} if already finalized, {@code false} otherwise
	 */
	protected boolean hasBeenFinalized() {
		return nextFinalizer == this;
	}

	/**
	 * Full constructor.
	 *
	 * @param chunk chunk to watch for cleaning
	 * @param finalizeAction vectors to clean on GC
	 */
	protected VectorFinalizer(IVectorChunk chunk, Runnable finalizeAction) {
		// Create a weak reference with this chunk so it goes to the queue on GC
		super(chunk, vectorReferenceQueue);
		this.finalizeAction = finalizeAction;
		// Store a strong reference to the weak reference to force it to live
		add();
	}

	/**
	 * Adds this finalizer to the chain.
	 */
	protected void add() {
		synchronized (lock) {
			if (head != null) {
				this.nextFinalizer = head;
				head.previousFinalizer = this;
			}
			head = this;
		}
	}

	/**
	 * Removes this finalizer from the chain.
	 */
	protected void remove() {
		synchronized (lock) {
			if (head == this) {
				if (nextFinalizer != null) {
					head = nextFinalizer;
				} else {
					assert previousFinalizer == null : "Head of chain has a previous member";
					head = previousFinalizer;
				}
			}
			if (nextFinalizer != null) {
				nextFinalizer.previousFinalizer = previousFinalizer;
			}
			if (previousFinalizer != null) {
				previousFinalizer.nextFinalizer = nextFinalizer;
			}
			this.nextFinalizer = this; // Indicate that this has been finalized
			this.previousFinalizer = this;
		}
	}

	/**
	 * Registers a chunk to watch for cleaning on GC.
	 * <p>
	 * This accepts a Runnable containing the logic to finalize a chunk. In return, it gives another Runnable
	 * containing the action that this finalizer will run. Calling the action allows another piece of code to run
	 * safely the finalization, without conflicting with the {@link VectorFinalizer}.
	 *
	 * @param chunk chunk to watch for cleaning
	 * @param finalizer action to run to finalize a chunk
	 * @return action that will be run when the chunk is eligible for finalization
	 */
	public static Runnable register(final IVectorChunk chunk, final Runnable finalizer) {
		final VectorFinalizer v = new VectorFinalizer(chunk, finalizer);
		startFinalizer();

		return v::finalizeChunk;
	}

	/**
	 * Executes finalization for a chunk that has been GCed.
	 */
	protected void finalizeChunk() {
		if (hasBeenFinalized()) {
			return;
		}

		synchronized (lock) {
			if (hasBeenFinalized()) {
				return;
			}

			remove();
		}

		finalizeAction.run();

		super.clear();
	}

	/**
	 * Starts the finalizer thread taking care of cleaning chunks on GC.
	 */
	public static void startFinalizer() {
		if (started.compareAndSet(false, true)) {
			// TODO should we make this part of an object, to be able to stop it on shutdown,
			// like epoch manager does
			finalizerThread = new Thread(new CleanChunkVectorTask(), "qfs-vector-finalizer");
			finalizerThread.setDaemon(true);
			finalizerThread.start();
		}
	}

	/**
	 * Stops the finalizing process and tries to collect all remaining chunks in a given time.
	 *
	 * @param timeout duration to complete the chunk collection
	 * @param unit duration unit
	 * @throws InterruptedException if this process is interrupted
	 * @throws TimeoutException if there are remaining chunks to collect after the given duration
	 */
	public static void stopAndAwaitCollection(final long timeout, final TimeUnit unit)
			throws InterruptedException,
				TimeoutException {

		if (!started.compareAndSet(true, false)) {
			return;
		}

		// Save the start time
		final long start = System.nanoTime();

		final Thread thread = finalizerThread;
		finalizerThread = null;
		thread.interrupt();
		thread.join();

		// Wait for all the previous fences to be collected
		final long nanoTimeout = unit.toNanos(timeout);
		boolean timedOut = false;
		boolean isComplete;
		synchronized (VectorFinalizer.lock) {
			isComplete = VectorFinalizer.head == null;
		}
		while (!isComplete) {
			if ((System.nanoTime() - start) < nanoTimeout) {
				System.gc();
				System.runFinalization();

				runFinalization();
				Thread.sleep(100);

				synchronized (VectorFinalizer.lock) {
					isComplete = VectorFinalizer.head == null;
				}
			} else {
				timedOut = true;
				break;
			}
		}

		// Throw a timeout exception if the chunks have not been collected in time
		if (timedOut) {
			throw new TimeoutException("Could not complete collection in less than " + timeout + " " + unit);
		}
	}

	/**
	 * Runs manually finalization of chunks marked as deleted.
	 */
	public static void runFinalization() {
		final ReferenceQueue<IVectorChunk> queue = VectorFinalizer.vectorReferenceQueue;
		VectorFinalizer finalizer;
		while ((finalizer = (VectorFinalizer) queue.poll()) != null) {
			finalizer.finalizeChunk();
		}
	}

	protected static class CleanChunkVectorTask implements Runnable {

		@Override
		public void run() {
			final ReferenceQueue<IVectorChunk> queue = VectorFinalizer.vectorReferenceQueue;
			final long timeOut = TimeUnit.HOURS.toMillis(1);
			while (!Thread.currentThread().isInterrupted()) {
				try {
					VectorFinalizer finalizer;
					while ((finalizer = (VectorFinalizer) queue.remove(timeOut)) != null) {
						finalizer.finalizeChunk();
					}
				} catch (InterruptedException e) {
					logger.info("Thread " + this + " is interrupted!");
					Thread.currentThread().interrupt();
					break;
				} catch (Throwable e) {
					// We don't want this thread to stop
					logger.log(
							Level.SEVERE,
							"An exception occurred during the off-heap cleaning, we might leak!",
							e);
				}
			}

			started.compareAndSet(true, false);
		}

	}

}
