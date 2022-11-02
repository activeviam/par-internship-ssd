/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.vector.IVector;

/**
 * Interface for chunk of vectors.
 *
 * @author ActiveViam
 */
public interface IVectorChunk extends IChunk<IVector> {

	/**
	 * Writes an object of type vector at the given position.
	 * <p>
	 * The provided vector might not be the written one, for instance it can be cloned off-heap before being
	 * written.
	 *
	 * @param position the position at which to write the vector
	 * @param vector the vector to write
	 */
	void writeVector(int position, IVector vector);

	@Override
	default int readInt(int position)  {
		throw new UnsupportedOperationException("Cannot read double as int.");
	}

	@Override
	default double readDouble(int position)  {
		throw new UnsupportedOperationException("Cannot read double as int.");
	}

	@Override
	default void writeInt(int position, int value)  {
		throw new UnsupportedOperationException("Cannot read double as int.");
	}

	@Override
	default void writeDouble(int position, double value)  {
		throw new UnsupportedOperationException("Cannot read double as int.");
	}
}
