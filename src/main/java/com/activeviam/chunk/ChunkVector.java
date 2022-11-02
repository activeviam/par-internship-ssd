/*
 * (C) ActiveViam 2007-2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.vector.IVector;
import com.activeviam.vector.VectorFinalizer;
import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * The base implementation for a chunk that is able to store vectors.
 *
 * @author ActiveViam
 */
public class ChunkVector implements IVectorChunk, Closeable {

	private final transient IChunkAllocator allocator;
	private final IVector[] vectors;
	private final Runnable destroyAction;
	private final Types type;

	/**
	 * Constructor.
	 *
	 * @param size the size of the chunk
	 * @param type the vector type
	 * @param allocator the allocator to use to allocate new vectors
	 */
	public ChunkVector(int size, Types type, IChunkAllocator allocator) {
		this.type = type;
		this.allocator = allocator;
		this.vectors = new IVector[size];
		if (allocator.isTransient()) {
			this.destroyAction = VectorFinalizer.register(this, directDestroyer(this.vectors));
		} else {
			this.destroyAction = transientDestroyer(this);
		}
	}

	private static Runnable directDestroyer(final Object[] objects) {
		return () -> Arrays.fill(objects, null);
	}

	static Runnable transientDestroyer(final ChunkVector chunk) {
		/* This method is static so that each new Runnable does not capture "this".
		 * This was initially a source of memory leak. */
		final WeakReference<?> objs = new WeakReference<>(chunk.vectors);
		return () -> {
			final Object o;
			if ((o = objs.get()) != null) {
				Arrays.fill((Object[]) o, null);
			}
		};
	}

	@Override
	public int capacity() {
		return this.vectors.length;
	}

	@Override
	public boolean isNull(int position) {
		return this.vectors[position] == null;
	}

	@Override
	public Object read(int position) {
		return this.vectors[position];
	}

	@Override
	public void write(final int position, final Object value) {
		if (value instanceof IVector || value == null) {
			writeVector(position, (IVector) value);
		} else {
			writeNotVector(position, value);
		}
	}

	/**
	 * Clones the vector. If this chunk is off heap the vector will be cloned off heap.
	 *
	 * @param vector the vector to clone
	 * @return the new cloned vector.
	 */
	protected IVector cloneVector(final IVector vector) {
		final IVector newVector = allocateVector(vector.size());
		newVector.copyFrom(vector);
		return newVector;
	}

	@Override
	public void writeVector(final int position, final IVector vector) {
		// If the allocator is transient, no need to count the references: they
		// will never be released.
		IVector vectorToWrite = vector;
		if (this.allocator.isTransient()) {
			if (vectorToWrite != null) {
				if (vectorToWrite.getAllocation() != AllocationType.DIRECT) {
					// reallocate on-heap vectors to direct memory if the chunk is off-heap
					vectorToWrite = cloneVector(vectorToWrite);
				}
			}
		}
		this.vectors[position] = vectorToWrite;
	}

	/**
	 * Writes a value that does not implement {@link IVector} at the given position in this chunk.
	 * <p>
	 * The only accepted type for the value are standard java arrays of the same component types as the vector.
	 * They are going to be converted to vectors.
	 *
	 * @param position the position at which to write the value
	 * @param value the value to write
	 * @throws IllegalArgumentException failure
	 */
	protected void writeNotVector(final int position, final Object value) throws IllegalArgumentException {

		// It wasn't a vector. This must be an array.
		final IVector vector;
		if (value instanceof double[] && getComponentType() == Types.DOUBLE) {
			final double[] v = ((double[]) value);
			vector = allocateVector(v.length);
			vector.copyFrom(v);
		} else if (value instanceof int[] && getComponentType() == Types.INTEGER) {
			final int[] v = ((int[]) value);
			vector = allocateVector(v.length);
			vector.copyFrom(v);
		} else {
			throw new IllegalArgumentException(
					"The object " + value + " is of an unexpected type, and cannot be converted to an IVector["
							+ getComponentType().name() + "]."
							+ (value.getClass().isArray()
									? " The given array is not a " + getComponentType().name() + " array."
									: "")
							+ (value instanceof IVector
									? " The given vector is not a " + getComponentType().name()
											+ " vector"
									: ""));
		}
		writeVector(position, vector);
	}

	private IVector allocateVector(final int size) {
		return this.allocator.getVectorAllocator(this.type).allocateNewVector(size);
	}

	/**
	 * Gets the component type of the vectors stored in this chunk.
	 *
	 * @return the component type of the vectors stored in this chunk
	 */
	protected Types getComponentType() {
		return this.type;
	}

	@Override
	public void close() {
		this.destroyAction.run();
	}
}
