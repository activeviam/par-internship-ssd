/*
 * (C) ActiveViam 2014-2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.block.IBlock;
import java.util.Arrays;

/**
 * Basic implementation for an integer fixed block vector.
 *
 * @author ActiveViam
 */
public class IntegerFixedBlockVector extends AFixedBlockVector {

	/**
	 * Constructor.
	 *
	 * @param block the block on which to create the vector
	 * @param position the position in the block
	 * @param length the length of the vector
	 */
	public IntegerFixedBlockVector(IBlock block, int position, int length) {
		super(block, position, length);
	}

	@Override
	public ITransientVector sort() {
		final int[] a = toIntArray();
		Arrays.sort(a);
		return new ArrayIntegerVector(a);
	}

	@Override
	public Integer read(final int index) {
		// readInt will check for fencing
		return readInt(index);
	}

	@Override
	public int readInt(final int index) {
		checkIndex(index);
		// getBlock will check for fencing
		return this.block.readInt(this.position + index);
	}

	@Override
	public long readLong(final int index) {
		// readInt will check for fencing
		return readInt(index);
	}

	@Override
	public float readFloat(final int index) {
		// readInt will check for fencing
		return readInt(index);
	}

	@Override
	public double readDouble(final int index) {
		// readInt will check for fencing
		return readInt(index);
	}

	@Override
	public void write(final int index, final Object value) {
		if (value instanceof Number) {
			writeInt(index, ((Number) value).intValue());
		}
	}

	@Override
	public void writeInt(final int index, final int value) {
		checkIndex(index);
		// getBlock will check for fencing
		this.block.writeInt(this.position + index, value);
	}

	@Override
	public void addInt(final int position, final int addedValue) {
		writeInt(position, readInt(position) + addedValue);
	}

	@Override
	protected IntegerFixedBlockVector createVector(final IBlock block, final int position, final int length) {

		return new IntegerFixedBlockVector(block, position, length);
	}

	@Override
	public double average() {
		return (double) sumInt() / this.length;
	}

	@Override
	protected double squaredEuclideanDistance(final IBlock block, final int position) {
		int s = 0;
		for (int i = 0; i < this.length; i++) {
			final int a = block.readInt(position + i);
			s += a * a;
		}
		return s;
	}

	@Override
	public IVector cloneOnHeap() {
		return new ArrayIntegerVector(toIntArray());
	}

}
