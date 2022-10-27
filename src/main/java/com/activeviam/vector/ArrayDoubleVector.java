/*
 * (C) ActiveViam 2014-2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.Type;
import com.activeviam.heap.MaxHeapDouble;
import com.activeviam.heap.MaxHeapDoubleWithIndices;
import com.activeviam.heap.MinHeapDouble;
import com.activeviam.heap.MinHeapDoubleWithIndices;
import com.activeviam.iterator.IPrimitiveIterator;
import java.util.Arrays;
import java.util.Objects;

/**
 * Implementation of an {@link IVector} that stores doubles on heap.
 * <p>
 * This is the safest implementation, and the only one where bounds are guaranteed to be checked.
 *
 * @author ActiveViam
 */
public class ArrayDoubleVector extends AArrayVector {


	/** The underlying array backing the structure. */
	protected final double[] underlying;

	/**
	 * Constructor.
	 *
	 * @param underlying the array to wrap in a vector, it is kept as is, so any modification done to the array
	 *        will be seen by the vector, and reciprocally
	 */
	public ArrayDoubleVector(double[] underlying) {
		this.underlying = Objects.requireNonNull(underlying, "Null value cannot be wrapped.");
	}

	/**
	 * Static constructor to build a vector of double values.
	 */
	public static ArrayDoubleVector of(final double... values) {
		return new ArrayDoubleVector(values);
	}

	/**
	 * Returns the underlying array.
	 *
	 * @return the underlying array
	 */
	public double[] getUnderlying() {
		return this.underlying;
	}

	@Override
	public ITransientVector sort() {
		final double[] a = toDoubleArray();
		Arrays.sort(a);
		return new ArrayDoubleVector(a);
	}

	@Override
	public void sortInPlace() {
		Arrays.sort(this.underlying);
	}

	@Override
	public int size() {
		return this.underlying.length;
	}

	@Override
	public Type getComponentType() {
		return Type.DOUBLE;
	}

	@Override
	public Double read(final int index) {
		return readDouble(index);
	}

	@Override
	public double readDouble(final int index) {
		return this.underlying[index];
	}

	@Override
	public void write(final int position, final int[] src) {
		final int length = src.length;
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] = src[i];
		}
	}

	@Override
	public void write(final int position, final long[] src) {
		final int length = src.length;
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] = src[i];
		}
	}

	@Override
	public void write(final int position, final float[] src) {
		final int length = src.length;
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] = src[i];
		}
	}

	@Override
	public void write(final int position, final double[] src) {
		final int length = src.length;
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] = src[i];
		}
	}

	@Override
	public void write(final int index, final Object value) {
		if (value instanceof Number) {
			writeDouble(index, ((Number) value).doubleValue());
		}
	}

	@Override
	public void writeInt(final int index, final int value) {
		this.underlying[index] = value;
	}

	@Override
	public void writeLong(final int index, final long value) {
		this.underlying[index] = value;
	}

	@Override
	public void writeFloat(final int index, final float value) {
		this.underlying[index] = value;
	}

	@Override
	public void writeDouble(final int index, final double value) {
		this.underlying[index] = value;
	}

	@Override
	public boolean isNull(final int position) {
		return false;
	}

	@Override
	public void addInt(final int index, final int value) {
		this.underlying[index] += value;
	}

	@Override
	public void addLong(final int index, final long value) {
		this.underlying[index] += value;
	}

	@Override
	public void addFloat(final int index, final float value) {
		this.underlying[index] += value;
	}

	@Override
	public void addDouble(final int index, final double value) {
		this.underlying[index] += value;
	}

	@Override
	public void copyTo(final double[] dst) {
		System.arraycopy(this.underlying, 0, dst, 0, dst.length);
	}

	@Override
	public void copyFrom(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final double[] rightArray = ((ArrayDoubleVector) vector).getUnderlying();
		final double[] leftArray = getUnderlying();
		for (int i = 0; i < length; ++i) {
			leftArray[i] = rightArray[i];
		}

	}

	@Override
	public void copyFrom(final int[] src) {
		final int length = src.length;
		for (int i = 0; i < length; ++i) {
			this.underlying[i] = src[i];
		}
	}

	@Override
	public void copyFrom(final long[] src) {
		final int length = src.length;
		for (int i = 0; i < length; ++i) {
			this.underlying[i] = src[i];
		}
	}

	@Override
	public void copyFrom(final float[] src) {
		final int length = src.length;
		for (int i = 0; i < length; ++i) {
			this.underlying[i] = src[i];
		}
	}

	@Override
	public void copyFrom(final double[] src) {
		System.arraycopy(src, 0, this.underlying, 0, src.length);
	}

	@Override
	public void fillInt(int value) {
		Arrays.fill(this.underlying, value);
	}

	@Override
	public void fillInt(int position, int lgth, int v) {
		Arrays.fill(this.underlying, position, position + lgth, v);
	}

	@Override
	public void fillLong(long value) {
		Arrays.fill(this.underlying, value);
	}

	@Override
	public void fillLong(int position, int lgth, long v) {
		Arrays.fill(this.underlying, position, position + lgth, v);
	}

	@Override
	public void fillFloat(float value) {
		Arrays.fill(this.underlying, value);
	}

	@Override
	public void fillFloat(int position, int lgth, float v) {
		Arrays.fill(this.underlying, position, position + lgth, v);
	}

	@Override
	public void fillDouble(double value) {
		Arrays.fill(this.underlying, value);
	}

	@Override
	public void fillDouble(int position, int lgth, double v) {
		Arrays.fill(this.underlying, position, position + lgth, v);
	}

	@Override
	public void scale(final int v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] *= v;
		}
	}

	@Override
	public void scale(final long v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] *= v;
		}
	}

	@Override
	public void scale(final float v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] *= v;
		}
	}

	@Override
	public void scale(final double v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] *= v;
		}
	}

	@Override
	public void scale(final int position, final int length, final int v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] *= v;
		}
	}

	@Override
	public void scale(final int position, final int length, final long v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] *= v;
		}
	}

	@Override
	public void scale(final int position, final int length, final float v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] *= v;
		}
	}

	@Override
	public void scale(final int position, final int length, final double v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] *= v;
		}
	}

	@Override
	public void translate(final int v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] += v;
		}
	}

	@Override
	public void translate(final long v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] += v;
		}
	}

	@Override
	public void translate(final float v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] += v;
		}
	}

	@Override
	public void translate(final double v) {
		final int end = size();
		for (int i = 0; i < end; ++i) {
			this.underlying[i] += v;
		}
	}

	@Override
	public void translate(final int position, final int length, final int v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] += v;
		}
	}

	@Override
	public void translate(final int position, final int length, final long v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] += v;
		}
	}

	@Override
	public void translate(final int position, final int length, final float v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] += v;
		}
	}

	@Override
	public void translate(final int position, final int length, final double v) {
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			this.underlying[pos] += v;
		}
	}

	@Override
	public int hashCode() {
		return hashCode(0, this.underlying.length);
	}

	@Override
	public int hashCode(final int position, final int length) {
		int result = 1;
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			long bits = Double.doubleToLongBits(underlying[pos]);
			result = 31 * result + (int) (bits ^ (bits >>> 32));
		}
		return result;
	}

	@Override
	public IVector subVector(final int start, final int length) {
		final double[] subArray = new double[length];
	  System.arraycopy(this.underlying, start, subArray, 0, length);
		return new ArrayDoubleVector(subArray);
	}

	@Override
	public IPrimitiveIterator topK(final int k) {
		checkIndex(0, k);
		return topK(0, size(), k);
	}

	@Override
	public IPrimitiveIterator topK(final int position, final int length, final int k) {
		if (k == 0) {
			return (IPrimitiveIterator) EmptyVector.emptyVector(getComponentType());
		}

		return topKMinHeapDouble(position, length, k);
	}

	@Override
	public IPrimitiveIterator bottomK(final int k) {
		checkIndex(0, k);
		return bottomK(0, size(), k);
	}

	@Override
	public IPrimitiveIterator bottomK(final int position, final int length, final int k) {
		if (k == 0) {
			return (IPrimitiveIterator) EmptyVector.emptyVector(getComponentType());
		}

		return bottomKMaxHeapDouble(position, length, k);
	}

	@Override
	public void reverse() {
		reverse(underlying);
	}

	public static void reverse(final double[] a) {
		final int l = a.length;
		final int max = l >> 1;
		for (int i = 0; i < max; i++) {
			final double temp = a[i];
			a[i] = a[l - i - 1];
			a[l - i - 1] = temp;
		}
	}

	@Override
	public IVector cloneOnHeap() {
		return new ArrayDoubleVector(toDoubleArray());
	}

	@Override
	public void plus(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final double[] rightArray = ((ArrayDoubleVector) vector).getUnderlying();
		final double[] leftArray = getUnderlying();
		for (int i = 0; i < length; ++i) {
			leftArray[i] += rightArray[i];
		}

	}

	@Override
	public void plusPositiveValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final double[] rightArray = ((ArrayDoubleVector) vector).getUnderlying();
		final double[] leftArray = getUnderlying();
		for (int i = 0; i < length; ++i) {
			leftArray[i] += Math.max(0, rightArray[i]);
		}

	}

	@Override
	public void plusNegativeValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final double[] rightArray = ((ArrayDoubleVector) vector).getUnderlying();
		final double[] leftArray = getUnderlying();
		for (int i = 0; i < length; ++i) {
			leftArray[i] += Math.min(0, rightArray[i]);
		}

	}

	@Override
	public void minus(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final double[] rightArray = ((ArrayDoubleVector) vector).getUnderlying();
		final double[] leftArray = getUnderlying();
		for (int i = 0; i < length; ++i) {
			leftArray[i] -= rightArray[i];
		}

	}

	@Override
	public void minusPositiveValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final double[] rightArray = ((ArrayDoubleVector) vector).getUnderlying();
		final double[] leftArray = getUnderlying();
		for (int i = 0; i < length; ++i) {
			leftArray[i] -= Math.max(0, rightArray[i]);
		}

	}

	@Override
	public void minusNegativeValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final double[] rightArray = ((ArrayDoubleVector) vector).getUnderlying();
		final double[] leftArray = getUnderlying();
		for (int i = 0; i < length; ++i) {
			leftArray[i] -= Math.min(0, rightArray[i]);
		}

	}

	@Override
	public void transfer(final int position, final double[] dest) {
		final int length = dest.length;
		int pos = position;
		for (int i = 0; i < length; ++i, ++pos) {
			dest[i] = underlying[pos];
		}
	}

	/**
	 * A min heap containing the smallest k elements.
	 *
	 * @param position the position in the block
	 * @param length the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MinHeapDouble topKMinHeapDouble(final int position, final int length, final int k) {
		final MinHeapDouble h = new MinHeapDouble(k);

		final int end = position + length;
		for (int i = position; i < end; ++i) {
			final int s = h.size();
			final double item = underlying[i];
			if (s < k || item > h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item);
			}
		}

		return h;
	}

	/**
	 * A min heap containing the smallest k elements and their indices.
	 *
	 * @param position the position in the block
	 * @param length the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MinHeapDoubleWithIndices topKMinHeapWithIndicesDouble(
			final int position,
			final int length,
			final int k) {

		final MinHeapDoubleWithIndices h = new MinHeapDoubleWithIndices(k);

		final int end = position + length;
		for (int i = position; i < end; ++i) {
			final int s = h.size();
			final double item = underlying[i];
			if (s < k || item > h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item, i - position);
			}
		}

		return h;
	}

	/**
	 * A max heap containing the smallest k elements.
	 *
	 * @param position the position in the block
	 * @param length the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MaxHeapDouble bottomKMaxHeapDouble(final int position, final int length, final int k) {
		final MaxHeapDouble h = new MaxHeapDouble(k);
		final int end = position + length;
		for (int i = position; i < end; ++i) {
			final int s = h.size();
			final double item = underlying[i];
			if (s < k || item < h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item);
			}
		}

		return h;
	}

	/**
	 * A max heap containing the smallest k elements and their indices.
	 *
	 * @param position the position in the block
	 * @param length the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MaxHeapDoubleWithIndices bottomKMaxHeapWithIndicesDouble(
			final int position,
			final int length,
			final int k) {

		final MaxHeapDoubleWithIndices h = new MaxHeapDoubleWithIndices(k);
		final int end = position + length;
		for (int i = position; i < end; ++i) {
			final int s = h.size();
			final double item = underlying[i];
			if (s < k || item < h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item, i - position);
			}
		}

		return h;
	}

	@Override
	public int[] topKIndices(final int position, final int length, final int k) {
		checkIndex(0, k);
		final MinHeapDoubleWithIndices h = topKMinHeapWithIndicesDouble(position, length, k);
		h.sort();
		return h.getArrayIndices();
	}

	@Override
	public int[] bottomKIndices(final int position, final int length, final int k) {
		checkIndex(0, k);
		final MaxHeapDoubleWithIndices h = bottomKMaxHeapWithIndicesDouble(position, length, k);
		h.sort();
		return h.getArrayIndices();
	}

	@Override
	public double average() {
		return sumDouble() / size();
	}

	@Override
	protected double squaredEuclideanNorm() {
		double s = 0d;
		for (int i = 0; i < size(); i++) {
			double a = readDouble(i);
			s += a * a;
		}
		return s;
	}

	@Override
	public double quantileDouble(final int position, final int length, final double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException(
					"Order of the quantile should be greater than zero and less than 1.");
		}
		if (r >= 0.5) {
			return topKMinHeapDouble(position, length, length - nearestRank(length, r) + 1).peek();
		} else {
			return bottomKMaxHeapDouble(position, length, nearestRank(length, r)).peek();
		}
	}

	@Override
	public double quantileDouble(final double r) {
		return quantileDouble(0, size(), r);
	}

	@Override
	public int quantileIndex(final int position, final int length, final double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException(
					"Order of the quantile should be greater than zero and less than 1.");
		}
		if (r >= 0.5) {
			return topKMinHeapWithIndicesDouble(position, length, length - nearestRank(length, r) + 1).peekIndex();
		} else {
			return bottomKMaxHeapWithIndicesDouble(position, length, nearestRank(length, r)).peekIndex();
		}
	}

}
