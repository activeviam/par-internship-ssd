/*
 * (C) ActiveViam 2014-2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
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
	public AllocationType getAllocation() {
		return this.block.getAllocation();
	}

	@Override
	public ITransientVector sort() {
		final double[] a = toDoubleArray();
		Arrays.sort(a);
		return new ArrayDoubleVector(a);
	}

	@Override
	public Types getComponentType() {
		return Types.DOUBLE;
	}

	@Override
	public void copyFrom(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final IBlock rightBlock = ((DoubleFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos(rightBlock, 3);
		long lftPos = getPos(leftBlock, 3);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 3);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 3, 32);
		for (; rghtPos < maxUnroll; rghtPos += 32, lftPos += 32) {
			UNSAFE.putDouble(
					lftPos,
					UNSAFE.getDouble(rghtPos));
			UNSAFE.putDouble(
					lftPos + (1 * (1 << 3)),
					UNSAFE.getDouble(rghtPos + (1 * (1 << 3))));
			UNSAFE.putDouble(
					lftPos + (2 * (1 << 3)),
					UNSAFE.getDouble(rghtPos + (2 * (1 << 3))));
			UNSAFE.putDouble(
					lftPos + (3 * (1 << 3)),
					UNSAFE.getDouble(rghtPos + (3 * (1 << 3))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 3, lftPos += 1 << 3) {
			UNSAFE.putDouble(
					lftPos,
					UNSAFE.getDouble(rghtPos));
		}
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

	@Override
	public void plus(IVector vector) {

	}

	@Override
	public void plusPositiveValues(IVector vector) {

	}

	@Override
	public void plusNegativeValues(IVector vector) {

	}

	@Override
	public void minus(IVector vector) {

	}

	@Override
	public void minusPositiveValues(IVector vector) {

	}

	@Override
	public void minusNegativeValues(IVector vector) {

	}

}
