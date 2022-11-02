/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.vector.IVector;
import java.util.BitSet;

public interface IntegerChunk extends IChunk {

	@Override
	default double readDouble(int position) {
		return readInt(position);
	}

	@Override
	default IVector readVector(int position) {
		throw new UnsupportedOperationException("Cannot read vector as integer");
	}

	@Override
	default void writeDouble(int position, double value) {
		throw new UnsupportedOperationException("Cannot write double into ints");
	}

	@Override
	default void write(int position, Object value) {
		writeInt(position, (Integer) value);
	}

	/**
	 * Finds the rows containing the given value.
	 *
	 * @param limit the rows after this limit will be ignored
	 * @return the rows containing the given value.
	 */
	default BitSet findRows(int value, int limit) {
		BitSet result = null;
		for (int i = 0; i < limit; i++) {
			if (readInt(i) == value) {
				if (result == null) {
					result = new BitSet();
				}
				result.set(i);
			}
		}
		return result;
	}
}
