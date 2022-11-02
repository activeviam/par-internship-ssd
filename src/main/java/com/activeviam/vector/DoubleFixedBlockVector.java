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
 * Basic implementation for a double fixed block vector.
 *
 * @author ActiveViam
 */
public class DoubleFixedBlockVector extends AFixedBlockVector {

	/**
	 * Constructor.
	 *
	 * @param block the block on which the vector is based: where it's components are stored
	 * @param position the position in the block at which one can find the first component of the vector
	 * @param length the length of the vector
	 */
	public DoubleFixedBlockVector(IBlock block, int position, int length) {
		super(block, position, length);
	}

	@Override
	public ITransientVector sort() {
		final double[] a = toDoubleArray();
		Arrays.sort(a);
		return new ArrayDoubleVector(a);
	}

	@Override
	public Double read(final int index) {
		// readDouble will check for fencing
		return readDouble(index);
	}

	@Override
	public double readDouble(final int index) {
		checkIndex(index);
		// getBlock will check for fencing
		return this.block.readDouble(this.position + index);
	}

	@Override
	public void write(final int index, final Object value) {
		if (value instanceof Number) {
			writeDouble(index, ((Number) value).doubleValue());
		}
	}

	@Override
	public void writeInt(final int index, final int value) {
		// writeDouble will check for fencing
		writeDouble(index, value);
	}

	@Override
	public void writeLong(final int index, final long value) {
		// writeDouble will check for fencing
		writeDouble(index, value);
	}

	@Override
	public void writeFloat(final int index, final float value) {
		// writeDouble will check for fencing
		writeDouble(index, value);
	}

	@Override
	public void addFloat(final int position, final float addedValue) {
		// writeDouble will check for fencing
		writeDouble(position, readDouble(position) + addedValue);
	}

	@Override
	public void addDouble(final int position, final double addedValue) {
		// writeDouble will check for fencing
		writeDouble(position, readDouble(position) + addedValue);
	}

	@Override
	protected DoubleFixedBlockVector createVector(final IBlock block, final int position, final int length) {

		return new DoubleFixedBlockVector(block, position, length);
	}

	@Override
	public double average() {
		return sumDouble() / length;
	}

	@Override
	protected double squaredEuclideanDistance(final IBlock block, final int position) {
		double s = 0d;
		for (int i = 0; i < length; i++) {
			final double a = block.readDouble(position + i);
			s += a * a;
		}
		return s;
	}

	@Override
	public IVector cloneOnHeap() {
		return new ArrayDoubleVector(toDoubleArray());
	}

}
