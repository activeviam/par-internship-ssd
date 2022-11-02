/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.allocator.AllocationType;
import com.activeviam.block.IBlock;
import com.activeviam.iterator.IPrimitiveIterator;

/**
 * Base class for on heap vectors.
 *
 * @author ActiveViam
 */
public abstract class AArrayVector extends AVector implements IBlock, ITransientVector {

	@Override
	public int capacity() {
		return size();
	}

	@Override
	public void acquire() {
		/* Do nothing for array-based vector */
	}

	@Override
	public void release(final int v) {
		/* Do nothing for array-based vector */
	}

	@Override
	public void acquireReference() {
		/* Do nothing for array-based vector */
	}

	@Override
	public void releaseReference() {
		/* Do nothing for array-based vector */
	}

	@Override
	public void transfer(final int position, final double[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void transfer(final int position, final float[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void transfer(final int position, final long[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void transfer(final int position, final int[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public AllocationType getAllocation() {
		return AllocationType.ON_HEAP;
	}

	@Override
	public void write(final int index, final Object value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void write(final int position, final double[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void write(final int position, final float[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void write(final int position, final long[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void write(final int position, final int[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillInt(int value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillInt(int position, int lgth, int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillLong(long value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillLong(int position, int lgth, long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillFloat(float value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillFloat(int position, int lgth, float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillDouble(double value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void fillDouble(int position, int lgth, double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void divide(final int position, final int lgth, final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void divide(final int position, final int lgth, final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public IPrimitiveIterator topK(final int position, final int lgth, final int k) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public int[] topKIndices(final int k) {
		if (k == 0) {
			return new int[0];
		}
		return topKIndices(0, size(), k);
	}

	@Override
	public IPrimitiveIterator bottomK(final int position, final int lgth, final int k) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public int[] bottomKIndices(final int k) {
		if (k == 0) {
			return new int[0];
		}
		return bottomKIndices(0, size(), k);
	}

	@Override
	public double variance() {
		double average = average();
		return squaredEuclideanNorm() / size() - average * average;
	}

	/**
	 * Computes the dot product of the vector and itself.
	 *
	 * @return the value of the squared Euclidean norm of the vector
	 */
	protected abstract double squaredEuclideanNorm();

	@Override
	public double quantileDouble(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public float quantileFloat(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public long quantileLong(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}

	@Override
	public int quantileInt(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getName());
	}


	@Override
	public int quantileIndex(final double r) {
		return quantileIndex(0, size(), r);
	}

	/**
	 * Convenient method for quantile using the Nearest Rank definition of quantile.
	 *
	 * @param length the length of the vector
	 * @param r the radix, as a number between 0 and 1
	 * @return the nearest rank
	 */
	protected int nearestRank(final int length, final double r) {
		return (int) Math.ceil(length * r);
	}

}
