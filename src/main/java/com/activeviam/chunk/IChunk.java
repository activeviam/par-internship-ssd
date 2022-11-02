/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

/**
 * A chunk of data within a column. The column delegates read and write operations to its chunks.
 * <p>
 * The implementation of a chunk must allow multiple concurrent readers to read the data while one single writer
 * writes in the chunk. Multiple concurrent writers is not by default supported by chunk implementations.
 *
 * @author ActiveViam
 */
public interface IChunk extends IArrayReader, IArrayWriter {

	Runnable EMPTY_DESTORYER = () -> {};

	/** Returns the (fixed) capacity of the chunk (number of elements it can store). */
	int capacity();

	/**
	 * Read an int primitive value from the array.
	 *
	 * <p>An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 */
	int readInt(int position);

	/**
	 * Read a double primitive value from the array.
	 *
	 * <p>An exception is thrown if the value is not of the right type.
	 *
	 * @param position 0-based index in an array
	 */
	double readDouble(int position);

	/**
	 * Write an int primitive value in the array.
	 *
	 * @param position 0-based index in an array
	 */
	void writeInt(int position, int value);

	/**
	 * Write a double primitive value in the array.
	 *
	 * @param position 0-based index in an array
	 */
	void writeDouble(int position, double value);

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
	 * Equivalent to a finalizer for this chunk, but that can be called by the application when it is certain that
	 * the chunk will not be used or even reached anymore.
	 * <p>
	 * Chunk implementations can use this opportunity to dereference objects, or even free memory in the case of
	 * direct memory chunks. For that reason it is unsafe to call destroy() and it is possible to crash the JVM if
	 * the chunk is accessed after its memory has been reclaimed.
	 * <p>
	 * Use {@link IChunk#EMPTY_DESTORYER} for chunks that doesn't need destroy action.
	 *
	 * @return the {@link Runnable} to run to actually free the data
	 */
	Runnable destroy();
}
