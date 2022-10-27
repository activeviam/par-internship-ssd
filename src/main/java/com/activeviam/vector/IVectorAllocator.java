/*
 * (C) ActiveViam 2014
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.Types;

/**
 * A vector allocator is responsible for allocating vectors of a given length. The type of vector to allocate is
 * determined by the implementation, but must be unique: all vectors allocated by the same pool must be of the same
 * type.
 *
 * @author ActiveViam
 */
public interface IVectorAllocator {

	/**
	 * Allocates a vector of the given length. The component type of the vector will be the one retrieved by
	 * {@link #getComponentType()}.
	 *
	 * @param length the length of the vector to allocate
	 * @return the allocated vector
	 */
	IVector allocateNewVector(final int length);

	/**
	 * Attempts to reallocate the underlying content of a vector. If the underlying data of this vector is not the
	 * same as the pools one, nothing happens.
	 *
	 * @param vector the vector for which to reallocate the underlying data
	 */
	void reallocateVector(final IVector vector);

	/**
	 * Copies the given vector into this pool. This method reallocates a vector of the same size using its own
	 * {@link #allocateNewVector(int)} method, with the same content as the given vector.
	 *
	 * @param toCopy the vector to copy
	 * @return a copy of the vector
	 */
	IVector copy(IVector toCopy);

	/**
	 * Returns the component type of the vectors allocated by this pool.
	 *
	 * @return the component type of the vectors allocated by this pool
	 * @see IVector#getComponentType()
	 */
	Types getComponentType();

	/**
	 * Releases this vector allocator, removing all references to whatever resources it holds.
	 */
	default void release() {
	}

}
