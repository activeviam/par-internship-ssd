/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.chunk.IArrayReader;
import com.activeviam.chunk.IArrayWriter;
import com.activeviam.iterator.IPrimitiveIterator;

/**
 * A vector is equivalent to a standard Java array in terms of interface. It has a fixed size and can be read or
 * written to.
 *
 * @author ActiveViam
 */
public interface IVector extends IArrayReader, IArrayWriter {

	/**
	 * Returns where this vector is allocated.
	 */
	AllocationType getAllocation();

	/**
	 * Returns a restricted view of this vector.
	 * <p>
	 * Modifications made to the returned vector will be seen by this vector, and reciprocally.
	 *
	 * @param start the index at which to start
	 * @param length the length of the view
	 * @return a restricted view of this vector
	 */
	IVector subVector(int start, int length);

	/**
	 * Builds a <strong>transient</strong> clone of the vector, sorts it in ascending order then returns it.
	 *
	 * @return a sorted, transient, clone of the vector
	 */
	ITransientVector sort();

	/**
	 * Returns an {@link IPrimitiveIterator iterator} of a collection composed of the k biggest elements of the
	 * vector. The iterator iterates over the k biggest elements from the smallest element to the biggest element.
	 *
	 * @param k the number of elements to return
	 * @return an iterator over the k biggest elements
	 */
	IPrimitiveIterator topK(int k);

	/**
	 * Returns the k indices of the k biggest elements of the vector sorted in ascending order.
	 *
	 * @param k the number of elements to return
	 * @return the k indices of the k biggest elements
	 */
	int[] topKIndices(int k);

	/**
	 * Returns an {@link IPrimitiveIterator iterator} of a collection composed of the k smallest elements of the
	 * vector. The iterator iterates over the k smallest elements from the biggest element to the smallest element.
	 *
	 * @param k the number of elements to return
	 * @return an iterator over the k smallest elements
	 */
	IPrimitiveIterator bottomK(int k);

	/**
	 * Returns the k indices of the k smallest elements of the vector sorted in ascending order.
	 *
	 * @param k the number of elements to return
	 * @return the k indices of the k smallest elements
	 */
	int[] bottomKIndices(int k);

	/**
	 * Returns the number of elements in the vector.
	 *
	 * @return the size of this vector
	 */
	int size();

	/**
	 * Returns the component type of this vector as a {@link Types type}.
	 *
	 * @return the component type of this vector
	 */
	Types getComponentType();

	/**
	 * Transfers the first {@code dst.length} elements of this vector into the given array.
	 * <p>
	 * For better performance, use the primitive type operations.
	 *
	 * @param dst the array to transfer the content of the vector into
	 */
	void copyTo(Object[] dst);

	/**
	 * Transfers the first {@code dst.length} elements of this vector into the given array.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as doubles without information
	 * loss.
	 *
	 * @param dst the array to transfer the content of the vector into
	 */
	void copyTo(double[] dst);

	/**
	 * Transfers the first {@code dst.length} elements of this vector into the given array.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as floats without information
	 * loss.
	 *
	 * @param dst the array to transfer the content of the vector into
	 */
	void copyTo(float[] dst);

	/**
	 * Transfers the first {@code dst.length} elements of this vector into the given array.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as longs without information loss.
	 *
	 * @param dst the array to transfer the content of the vector into
	 */
	void copyTo(long[] dst);

	/**
	 * Transfers the first {@code dst.length} elements of this vector into the given array.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as ints without information loss.
	 *
	 * @param dst the array to transfer the content of the vector into
	 */
	void copyTo(int[] dst);

	/**
	 * Copies the content of the given vector into this vector, overriding the existing data.
	 *
	 * @param vector the vector to copy into this instance, if it is smaller than this instance, values are
	 *        transfered only up to the size of the vector
	 */
	void copyFrom(IVector vector);

	/**
	 * Writes elements of the given array into the first {@code src.length} elements of this vector.
	 * <p>
	 * For better performance, use the primitive type operations.
	 *
	 * @param src the array to write into the vector
	 */
	void copyFrom(Object[] src);

	/**
	 * Writes elements of the given array into the first {@code src.length} elements of this vector.
	 * <p>
	 * This method is only supported for vectors that can write a double without information loss.
	 *
	 * @param src the array to write into the vector
	 */
	void copyFrom(double[] src);

	/**
	 * Writes elements of the given array into the first {@code src.length} elements of this vector.
	 * <p>
	 * This method is only supported for vectors that can write a float without information loss.
	 *
	 * @param src the array to write into the vector
	 */
	void copyFrom(float[] src);

	/**
	 * Writes elements of the given array into the first {@code src.length} elements of this vector.
	 * <p>
	 * This method is only supported for vectors that can write a long without information loss.
	 *
	 * @param src the array to write into the vector
	 */
	void copyFrom(long[] src);

	/**
	 * Writes elements of the given array into the first {@code src.length} elements of this vector.
	 * <p>
	 * This method is only supported for vectors that can write an int without information loss.
	 *
	 * @param src the array to write into the vector
	 */
	void copyFrom(int[] src);

	/**
	 * Reads all the elements of the vector and returns them in a standard java array.
	 * <p>
	 * The returned array can be safely written to without modifying the vector.
	 * <p>
	 * For better performance, use the primitive type operations.
	 *
	 * @return all the elements of the vector
	 */
	Object[] toArray();

	/**
	 * Reads all the elements of the vector and returns them in a double array.
	 * <p>
	 * The returned array can be safely written to without modifying the vector.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as doubles without information
	 * loss.
	 *
	 * @return all the elements of the vector
	 */
	double[] toDoubleArray();

	/**
	 * Reads all the elements of the vector and returns them in a float array.
	 * <p>
	 * The returned array can be safely written to without modifying the vector.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as floats without information
	 * loss.
	 *
	 * @return all the elements of the vector
	 */
	float[] toFloatArray();

	/**
	 * Reads all the elements of the vector and returns them in a long array.
	 * <p>
	 * The returned array can be safely written to without modifying the vector.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as longs without information loss.
	 *
	 * @return all the elements of the vector
	 */
	long[] toLongArray();

	/**
	 * Reads all the elements of the vector and returns them in an int array.
	 * <p>
	 * The returned array can be safely written to without modifying the vector.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as ints without information loss.
	 *
	 * @return all the elements of the vector
	 */
	int[] toIntArray();

	/**
	 * Clones this vector. The clone is guaranteed to live inside the Java heap.
	 * <p>
	 * This method always returns a new instance, even if this vector is already on-heap.
	 *
	 * @return an on-heap clone of this instance
	 */
	IVector cloneOnHeap();

	/**
	 * Adds the content of the given vector to this vector, overriding the existing data. This is equivalent to the
	 * standard primitive operation {@code +=}, this function effectively doing {@code this += vector}.
	 * <p>
	 * If the given vector is smaller than the destination vector ({@code this}), the sum operator will be applied
	 * at all indexes up to {@code vector.size()}.
	 * <p>
	 * It is illegal to pass a vector that is larger than the destination vector ({@code this}).
	 *
	 * @param vector the vector to add to this instance
	 */
	void plus(IVector vector);

	/**
	 * Adds the positive values of the given vector to this vector, overriding the existing data.
	 * <p>
	 * This is the vector equivalent to doing:
	 *
	 * <pre>
	 * for (int i = 0; i &lt; size(); ++i) {
	 * 	to[i] += Math.max(0, from[i]);
	 * }
	 * </pre>
	 * <p>
	 * If the given vector is smaller than the destination vector ({@code this}), the sum operator will be applied
	 * at all indexes up to {@code vector.size()}.
	 * <p>
	 * It is illegal to pass a vector that is larger than the destination vector ({@code this}).
	 *
	 * @param vector the vector to add to this instance
	 */
	void plusPositiveValues(IVector vector);

	/**
	 * Adds the negative values of the given vector to this vector, overriding the existing data.
	 * <p>
	 * This is the vector equivalent to doing:
	 *
	 * <pre>
	 * for (int i = 0; i &lt; size(); ++i) {
	 * 	to[i] += Math.min(0, from[i]);
	 * }
	 * </pre>
	 * <p>
	 * If the given vector is smaller than the destination vector ({@code this}), the sum operator will be applied
	 * at all indexes up to {@code vector.size()}.
	 * <p>
	 * It is illegal to pass a vector that is larger than the destination vector ({@code this}).
	 *
	 * @param vector the vector to add to this instance
	 */
	void plusNegativeValues(IVector vector);

	/**
	 * Subtracts the content of the given vector to this vector, overriding the existing data. This is equivalent
	 * to the standard primitive operation {@code -=}, this function effectively doing {@code this -= vector}.
	 * <p>
	 * If the given vector is smaller than the destination vector ({@code this}), the subtraction operator will be
	 * applied at all indexes up to {@code vector.size()}.
	 * <p>
	 * It is illegal to pass a vector that is larger than the destination vector ({@code this}).
	 *
	 * @param vector the vector to subtract to this instance
	 */
	void minus(IVector vector);

	/**
	 * Subtracts the positive values of the given vector to this vector, overriding the existing data.
	 * <p>
	 * This is the vector equivalent to doing:
	 *
	 * <pre>
	 * for (int i = 0; i &lt; size(); ++i) {
	 * 	to[i] -= Math.max(0, from[i]);
	 * }
	 * </pre>
	 * <p>
	 * If the given vector is smaller than the destination vector ({@code this}), the subtraction operator will be
	 * applied at all indexes up to {@code vector.size()}.
	 * <p>
	 * It is illegal to pass a vector that is larger than the destination vector ({@code this}).
	 *
	 * @param vector the vector to subtract to this instance
	 */
	void minusPositiveValues(IVector vector);

	/**
	 * Subtracts the negative values of the given vector to this vector, overriding the existing data.
	 * <p>
	 * This is the vector equivalent to doing:
	 *
	 * <pre>
	 * for (int i = 0; i &lt; size(); ++i) {
	 * 	to[i] -= Math.min(0, from[i]);
	 * }
	 * </pre>
	 * <p>
	 * If the given vector is smaller than the destination vector ({@code this}), the subtraction operator will be
	 * applied at all indexes up to {@code vector.size()}.
	 * <p>
	 * It is illegal to pass a vector that is larger than the destination vector ({@code this}).
	 *
	 * @param vector the vector to subtract to this instance
	 */
	void minusNegativeValues(IVector vector);

	/**
	 * Fills the vector with the given value.
	 *
	 * @param value the value used to fill the vector
	 */
	void fillDouble(double value);

	/**
	 * Fills the vector with the given value.
	 *
	 * @param value the value used to fill the vector
	 */
	void fillFloat(float value);

	/**
	 * Fills the vector with the given value.
	 *
	 * @param value the value used to fill the vector
	 */
	void fillLong(long value);

	/**
	 * Fills the vector with the given value.
	 *
	 * @param value the value used to fill the vector
	 */
	void fillInt(int value);

	/**
	 * Scales the vector by a factor f: multiplies all its components by the given factor.
	 *
	 * @param f the factor by which to scale the vector
	 */
	void scale(double f);

	/**
	 * Scales the vector by a factor f: multiplies all its components by the given factor.
	 *
	 * @param f the factor by which to scale the vector
	 */
	void scale(float f);

	/**
	 * Scales the vector by a factor f: multiplies all its components by the given factor.
	 *
	 * @param f the factor by which to scale the vector
	 */
	void scale(long f);

	/**
	 * Scales the vector by a factor f: multiplies all its components by the given factor.
	 *
	 * @param f the factor by which to scale the vector
	 */
	void scale(int f);

	/**
	 * Divides the vector by a factor f: divides all its components by the given factor.
	 * <p>
	 * This method is only supported in long vectors.
	 *
	 * @param f the divisor by which to divide the vector
	 */
	void divide(long f);

	/**
	 * Divides the vector by a factor f: divides all its components by the given factor.
	 * <p>
	 * This method is only supported in long and int vectors.
	 *
	 * @param f the divisor by which to divide the vector
	 */
	void divide(int f);

	/**
	 * Translates the vector by a value v: adds the given value to all its components.
	 *
	 * @param v the value by which to translate the vector
	 */
	void translate(double v);

	/**
	 * Translate the vector by a value v: adds the given value to all its components.
	 *
	 * @param v the value by which to translate the vector
	 */
	void translate(float v);

	/**
	 * Translate the vector by a value v: adds the given value to all its components.
	 *
	 * @param v the value by which to translate the vector
	 */
	void translate(long v);

	/**
	 * Translate the vector by a value v: adds the given value to all its components.
	 *
	 * @param v the value by which to translate the vector
	 */
	void translate(int v);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Implementation of equals corresponds to the vector equivalent of java.util.Arrays.equals(...[], ...[]),
	 * where ... is the component type of the {@link IVector vectors}. Two {@link IVector vectors} that do not have
	 * the same component type are never equals.
	 */
	@Override
	boolean equals(Object obj);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Implementation of hashCode corresponds to the vector equivalent of java.util.Arrays.hashCode(...[]), where
	 * ... is the component type of the {@link IVector vectors}.
	 */
	@Override
	int hashCode();

	/**
	 * Computes the mean of a vector, i.e. the arithmetic average of its components.
	 *
	 * @return the mean of the vector
	 */
	double average();

	/**
	 * Sums all elements of the vector and returns the value of the sum. This method is only supported for vectors
	 * that can retrieve their content as <b>{@code double}</b> without information loss.
	 *
	 * @return sum of all elements
	 */
	double sumDouble();

	/**
	 * Sums all elements of the vector and returns the value of the sum. This method is only supported for vectors
	 * that can retrieve their content as <b>{@code float}</b> without information loss.
	 *
	 * @return sum of all elements
	 */
	float sumFloat();

	/**
	 * Sums all elements of the vector and returns the value of the sum. This method is only supported for vectors
	 * that can retrieve their content as <b>{@code long}</b> without information loss.
	 *
	 * @return sum of all elements
	 */
	long sumLong();

	/**
	 * Sums all elements of the vector and returns the value of the sum. This method is only supported for vectors
	 * that can retrieve their content as <b>{@code int}</b> without information loss.
	 *
	 * @return sum of the elements of the vector
	 */
	int sumInt();

	/**
	 * Returns the sample variance of the vector elements (the amount of variation or dispersion from the average).
	 * <p>
	 * The variance returned is the naive <b>biased estimator</b> of the population variance based on the vector
	 * data.
	 * <p>
	 * In order to obtain the <b>unbiased estimator</b> of the population variance based on the vector data, one
	 * should perform Bessel's correction on the result of this method.
	 * <p>
	 * Considering S_n as the biased sample variance obtained from an n-sized vector, one can obtain S the unbiased
	 * estimator of the variance by computing:
	 *
	 * <pre>
	 * S = sqrt((n * S_n ^ 2) / (n - 1))
	 * </pre>
	 *
	 * @return variance of the vector
	 * @see <a href="http://mathworld.wolfram.com/SampleVariance.html">Sample variance on Wolfram Mathworld</a>
	 * @see <a href=""> Upton, G.; Cook, I. (2008) Oxford Dictionary of Statistics, entry "Variance"</a>
	 */
	double variance();

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {@code r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code double}</b> without
	 * information loss.
	 *
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the quantile of order {@code r}
	 */
	double quantileDouble(double r);

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {@code r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code float}</b> without
	 * information loss.
	 *
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the quantile of order {@code r}
	 */
	float quantileFloat(double r);

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {@code r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code long}</b> without
	 * information loss.
	 *
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the quantile of order {@code r}
	 */
	long quantileLong(double r);

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {@code r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code int}</b> without
	 * information loss.
	 *
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the quantile of order {@code r}
	 */
	int quantileInt(double r);

	/**
	 * Returns the index of the quantile of order {@code r}.
	 *
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the index of quantile of order {@code r}
	 */
	int quantileIndex(double r);

}
