/*
 * (C) ActiveViam 2018
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.allocator;

import com.activeviam.UnsafeUtil;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.Buffer;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class regrouping operations that must be executed on the host VM and are potential hacks.
 *
 * @author ActiveViam
 */
public class PlatformOperations {

	/** Page size. */
	protected static final int PAGE_SIZE = UnsafeUtil.pageSize();

	/** Logger. */
	private static final Logger logger = Logger.getLogger(PlatformOperations.class.getName());

	/** Configured policy for memory alignment with page size. */
	protected static final boolean isMemoryAligned;

	/** The memory allocation to reserve and unreserve memory. */
	protected static DirectMemoryTracker directMemoryTracker;

	static {
		final boolean considerPageAlignment = false;
		isMemoryAligned = considerPageAlignment && computeMemoryPageAlignmentPolicy();
	}

	private PlatformOperations() {
	}

	/** Done as Arrow's Memory Util. */
	// HACK
	@Deprecated(forRemoval = true, since = "5.12.0")
	private static final long BYTE_BUFFER_ADDRESS_OFFSET = 16;

	/**
	 * Gets the off-heap memory address where the data of a direct buffer is stored.
	 *
	 * @param buffer buffer to inspect
	 * @return the memory address
	 */
	public static long getOffHeapMemoryAddress(final Buffer buffer) {
		assert buffer.isDirect();
		try {
			return UnsafeUtil.getUnsafe().getLong(buffer, BYTE_BUFFER_ADDRESS_OFFSET);
		} catch (Throwable throwable) {
			throw new RuntimeException("Cannot access buffer address of " + buffer, throwable);
		}
	}

	/**
	 * Get the value set by: -XX:MaxDirectMemorySize parameter.
	 *
	 * @return the maximal direct memory available in bytes
	 */
	public static long getMaxDirectMemory() {
		return memoryTracker().getMaxDirectMemory();
	}

	/**
	 * Increase the amount of direct memory allocated.
	 *
	 * @param bytes the number of bytes to be allocated.
	 */
	public static void reserveDirectMemory(int bytes) {
		long size = Math.max(1L, (long) bytes + (isDirectMemoryPageAligned() ? PAGE_SIZE : 0));
		try {
			memoryTracker().reserveMemory(size, bytes);
		} catch (Throwable e) {
			logger.log(Level.WARNING, "Failed reserving " + bytes + " bytes of memory.", e);
			anyMatch(e, t -> {
				if (t instanceof OutOfMemoryError) {
					throw (OutOfMemoryError) t;
				}
				return false;
			});
		}
	}

	private static boolean anyMatch(final Throwable error, final Predicate<Throwable> predicate) {
		Throwable cause = error;
		do {
			if (predicate.test(cause)) {
				return true;
			}
		} while ((cause = getCause(cause)) != null);
		return false;
	}

	private static Throwable getCause(final Throwable error) {
		final Throwable cause = error.getCause();
		return cause == error ? null : cause;
	}

	/**
	 * Decrease the amount of direct memory allocated.
	 *
	 * @param bytes the number of bytes to be freed.
	 */
	public static void unreserveDirectMemory(int bytes) {
		long size = Math.max(1L, (long) bytes + (isDirectMemoryPageAligned() ? PAGE_SIZE : 0));
		try {
			memoryTracker().unreserveMemory(size, bytes);
		} catch (Throwable e) {
			logger.log(Level.WARNING, "Failed unreserving  " + bytes + " bytes of memory.", e);
		}
	}

	/**
	 * Returns the memory allocator.
	 *
	 * @return the {@link DirectMemoryTracker} lazily instantiated.
	 */
	public static DirectMemoryTracker memoryTracker() {
		if (directMemoryTracker == null) {
			synchronized (PlatformOperations.class) {
				if (directMemoryTracker == null) {
					final DirectMemoryTracker accessor = new DirectMemoryTracker();
					directMemoryTracker = accessor;
					return accessor;
				}
			}
		}
		return directMemoryTracker;
	}

	/**
	 * Returns {@code true} if the direct buffers should be page aligned, as defined by -XX:+PageAlignDirectMemory.
	 *
	 * @return true if aligned
	 */
	public static boolean isDirectMemoryPageAligned() {
		return isMemoryAligned;
	}

	/**
	 * Computes the configured value for memory alignment on page size.
	 *
	 * @return the value configured using {@code -XX:+PageAlignDirectMemory}
	 */
	private static boolean computeMemoryPageAlignmentPolicy() {
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = runtimeMxBean.getInputArguments();

		return arguments.contains("-XX:+PageAlignDirectMemory");
	}

	/**
	 * "Destroys" a buffer by freeing the resources the buffer was using.
	 * <p>
	 * Before calling this method you MUST be absolutely certain that the buffer is used or can be reached by the
	 * application. Failure to do that can lead to bugs or even crash the JVM.
	 * <p>
	 * Free the memory is already done by the buffer itself. We just anticipate its cleaning before it is
	 * discovered by the GC.
	 *
	 * @param buffer buffer to destroy
	 */
	public static void destroyBuffer(Buffer buffer) {
		buffer.clear();
	}

}
