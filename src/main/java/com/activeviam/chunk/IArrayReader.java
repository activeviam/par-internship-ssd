/*
 * (C) ActiveViam 2007-2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.vector.IVector;

/**
 * Elements of data available following the array layout.
 *
 * @author ActiveViam
 */
public interface IArrayReader {

	/**
	 * Gets whether {@code null} is stored in a given row at a given position or not.
	 *
	 * @param position 0-based index in an array
	 * @return {@code true} if the element stored at that position is {@code null}, {@code false} otherwise
	 */
	boolean isNull(int position);

	/**
	 * Reads a value at some index in an array.
	 *
	 * @param position 0-based index in an array
	 * @return the data stored at that position in the array
	 */
	Object read(int position);

	/**
	 * Reads a boolean primitive value from the array.
	 * <p>
	 * An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 * @return boolean value
	 */
	default boolean readBoolean(final int position) {
		throw new UnsupportedOperationException(
				"Cannot read 'boolean' value from an instance of " + getClass().getSimpleName());
	}

	/**
	 * Reads an int primitive value from the array.
	 * <p>
	 * An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 * @return int value
	 */
	default int readInt(final int position) {
		throw new UnsupportedOperationException(
				"Cannot read 'int' value from an instance of " + getClass().getSimpleName());
	}

	/**
	 * Reads a long primitive value from the array.
	 * <p>
	 * An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 * @return long value
	 */
	default long readLong(final int position) {
		throw new UnsupportedOperationException(
				"Cannot read 'long' value from an instance of " + getClass().getSimpleName());
	}

	/**
	 * Reads a double primitive value from the array.
	 * <p>
	 * An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 * @return double value
	 */
	default double readDouble(final int position) {
		throw new UnsupportedOperationException(
				"Cannot read 'double' value from an instance of " + getClass().getSimpleName());
	}

	/**
	 * Reads a float primitive value from the array.
	 * <p>
	 * An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 * @return float value
	 */
	default float readFloat(final int position) {
		throw new UnsupportedOperationException(
				"Cannot read 'float' value from an instance of " + getClass().getSimpleName());
	}

	/**
	 * Reads an {@link IVector} value from the array.
	 * <p>
	 * An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 * @return vector instance
	 */
	default IVector readVector(final int position) {
		return (IVector) read(position);
	}

}
