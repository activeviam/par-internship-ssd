/*
 * (C) ActiveViam 2007-2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.vector.IReleasableVector;
import com.activeviam.vector.IVector;
import com.activeviam.vector.IVectorAllocator;

/**
 * The base implementation for a chunk that is able to store vectors.
 *
 * @author ActiveViam
 */
public class ChunkVector implements IVectorChunk {

	protected final transient IVectorAllocator allocator;

	/**
	 * Constructor.
	 *
	 * @param chunkSize the size of the chunk
	 * @param allocator the allocator to use to allocate new vectors
	 */
	public ChunkVector(int chunkSize, IVectorAllocator allocator) {
		this.allocator = allocator;
	}

	@Override
	public int capacity() {
		return 0;
	}

	@Override
	public IVector readVector(int position) {
		return null;
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
		if (!isTransient()) {
			if (vectorToWrite != null) {
				if (vectorToWrite.getAllocation() != AllocationType.DIRECT) {
					// reallocate on-heap vectors to direct memory if the chunk is off-heap
					vectorToWrite = cloneVector(vectorToWrite);
				}
				// Increase the reference counter of the new value
				// We don't check with instanceof here because we assume that all vectors should
				// implement IReleasableVector, including custom wrapper vectors
				((IReleasableVector) vectorToWrite).acquireReference();
			}
			final IVector oldValue = super.read(position);
			if (oldValue != null) {
				// Decrease the reference counter of the old value
				((IReleasableVector) oldValue).releaseReference();
			}
		}
		super.write(position, vectorToWrite);
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
		return allocator.allocateNewVector(size);
	}

	/**
	 * Gets the component type of the vectors stored in this chunk.
	 *
	 * @return the component type of the vectors stored in this chunk
	 */
	protected Types getComponentType() {
		return this.allocator.getComponentType();
	}

}
