/*
 * (C) ActiveViam 2007-2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

/**
 * Interface to write into a structure with an array layout.
 *
 * @author ActiveViam
 */
public interface IArrayWriter {

	/**
	 * Writes a piece of data at a position in the array.
	 * <p>
	 * For better performance, use the primitive type operations.
	 *
	 * @param position 0-based index in the array
	 * @param value the value to write
	 */
	void write(int position, Object value);

	/**
	 * Writes a boolean primitive value in the array.
	 *
	 * @param position 0-based index in the array
	 * @param value the value to write
	 */
	default void writeBoolean(final int position, final boolean value) {
		throw new UnsupportedOperationException(
				"Cannot write 'boolean' value into an instance of " + getClass().getSimpleName());
	}

	/**
	 * Writes an int primitive value in the array.
	 *
	 * @param position 0-based index in the array
	 * @param value the value to write
	 */
	default void writeInt(final int position, final int value) {
		throw new UnsupportedOperationException(
				"Cannot write 'int' value into an instance of " + getClass().getSimpleName());
	}

	/**
	 * Writes a long primitive value in the array.
	 *
	 * @param position 0-based index in the array
	 * @param value the value to write
	 */
	default void writeLong(final int position, final long value) {
		throw new UnsupportedOperationException(
				"Cannot write 'long' value into an instance of " + getClass().getSimpleName());
	}

	/**
	 * Writes a double primitive value in the array.
	 *
	 * @param position 0-based index in the array
	 * @param value the value to write
	 */
	default void writeDouble(final int position, final double value) {
		throw new UnsupportedOperationException(
				"Cannot write 'double' value into an instance of " + getClass().getSimpleName());
	}

	/**
	 * Writes a float primitive value in the array.
	 *
	 * @param position 0-based index in the array
	 * @param value the value to write
	 */
	default void writeFloat(final int position, final float value) {
		throw new UnsupportedOperationException(
				"Cannot write 'float' value into an instance of " + getClass().getSimpleName());
	}

	// AGGREGATION SUPPORT

	/**
	 * Adds a int primitive value to an element of the array.
	 *
	 * @param position 0-based index in the array
	 * @param addedValue the value to add
	 */
	default void addLong(final int position, final long addedValue) {
		throw new UnsupportedOperationException(
				"Cannot add 'long' value into an instance of " + getClass().getSimpleName());
	}

	/**
	 * Adds a long primitive value to an element of the array.
	 *
	 * @param position 0-based index in the array
	 * @param addedValue the value to add
	 */
	default void addInt(final int position, final int addedValue) {
		throw new UnsupportedOperationException(
				"Cannot add 'int' value into an instance of " + getClass().getSimpleName());
	}

	/**
	 * Adds a double primitive value to an element of the array.
	 *
	 * @param position 0-based index in the array
	 * @param addedValue the value to add
	 */
	default void addDouble(final int position, final double addedValue) {
		throw new UnsupportedOperationException(
				"Cannot add 'double' value into an instance of " + getClass().getSimpleName());
	}

	/**
	 * Adds a float primitive value to an element of the array.
	 *
	 * @param position 0-based index in the array
	 * @param addedValue the value to add
	 */
	default void addFloat(final int position, final float addedValue) {
		throw new UnsupportedOperationException(
				"Cannot add 'float' value into an instance of " + getClass().getSimpleName());
	}

}
