/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.vector.IVector;

/**
 * @author ActiveViam
 */
public interface IVectorChunk extends IChunk<IVector> {

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
