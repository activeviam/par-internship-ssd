/*
 * (C) ActiveViam 2007-2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.block;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.chunk.IArrayReader;
import com.activeviam.chunk.IArrayWriter;
import com.activeviam.iterator.IPrimitiveIterator;
import com.activeviam.vector.IVector;
import java.util.Arrays;

/**
 * An atomic group of data that servers as backing structure for {@link IVector vectors}. A block can hold the
 * underlying data of several vectors.
 *
 * @author ActiveViam
 */
public interface IBlock extends IArrayReader, IArrayWriter {

	/**
	 * Returns the capacity of the block (i.e. the number of vectors components of type {@link #getComponentType()}
	 * that can be stored in this block.
	 *
	 * @return the capacity of the block
	 */
	int capacity();

	/**
	 * Returns the component type of the vectors whose data can be stored in this block.
	 *
	 * @return the component type of the vectors whose data can be stored in this block
	 */
	Types getComponentType();

	/**
	 * Transfers the content of the block into the given array.
	 *
	 * @param position the position at which to start
	 * @param dest the destination array
	 */
	void transfer(int position, double[] dest);

	/**
	 * Transfers the content of the block into the given array.
	 *
	 * @param position the position at which to start
	 * @param dest the destination array
	 */
	void transfer(int position, float[] dest);

	/**
	 * Transfers the content of the block into the given array.
	 *
	 * @param position the position at which to start
	 * @param dest the destination array
	 */
	void transfer(int position, long[] dest);

	/**
	 * Transfers the content of the block into the given array.
	 *
	 * @param position the position at which to start
	 * @param dest the destination array
	 */
	void transfer(int position, int[] dest);

	/**
	 * Writes the content of the array into the block.
	 *
	 * @param position the position at which to start
	 * @param src the source array
	 */
	void write(int position, double[] src);

	/**
	 * Writes the content of the array into the block.
	 *
	 * @param position the position at which to start
	 * @param src the source array
	 */
	void write(int position, float[] src);

	/**
	 * Writes the content of the array into the block.
	 *
	 * @param position the position at which to start
	 * @param src the source array
	 */
	void write(int position, long[] src);

	/**
	 * Writes the content of the array into the block.
	 *
	 * @param position the position at which to start
	 * @param src the source array
	 */
	void write(int position, int[] src);

	/**
	 * Fills the components of the block with the given value.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to fill
	 * @param v the value
	 */
	void fillDouble(int position, int lgth, double v);

	/**
	 * Fills the components of the block with the given value.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to fill
	 * @param v the value
	 */
	void fillFloat(int position, int lgth, float v);

	/**
	 * Fills the components of the block with the given value.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to fill
	 * @param v the value
	 */
	void fillLong(int position, int lgth, long v);

	/**
	 * Fills the components of the block with the given value.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to fill
	 * @param v the value
	 */
	void fillInt(int position, int lgth, int v);

	/**
	 * Multiplies the components of the block by the given factor.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to multiply
	 * @param v the factor
	 */
	void scale(int position, int lgth, double v);

	/**
	 * Multiplies the components of the block by the given factor.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to multiply
	 * @param v the factor
	 */
	void scale(int position, int lgth, float v);

	/**
	 * Multiplies the components of the block by the given factor.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to multiply
	 * @param v the factor
	 */
	void scale(int position, int lgth, long v);

	/**
	 * Multiplies the components of the block by the given factor.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to multiply
	 * @param v the factor
	 */
	void scale(int position, int lgth, int v);

	/**
	 * Multiplies the components of the block by the given factor.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to multiply
	 * @param v the factor
	 */
	void divide(int position, int lgth, long v);

	/**
	 * Multiplies the components of the block by the given factor.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to multiply
	 * @param v the factor
	 */
	void divide(int position, int lgth, int v);

	/**
	 * Adds the given value to the components of the block.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to sum
	 * @param v the added value
	 */
	void translate(int position, int lgth, double v);

	/**
	 * Adds the given value to the components of the block.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to sum
	 * @param v the added value
	 */
	void translate(int position, int lgth, float v);

	/**
	 * Adds the given value to the components of the block.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to sum
	 * @param v the added value
	 */
	void translate(int position, int lgth, long v);

	/**
	 * Adds the given value to the components of the block.
	 *
	 * @param position the position at which to start
	 * @param lgth the number of components to sum
	 * @param v the added value
	 */
	void translate(int position, int lgth, int v);

	/**
	 * Calculates the hashCode of the given portion of the block, as per {@link Arrays#hashCode(Object[])}.
	 *
	 * @param position the position at which to start
	 * @param length the number of components to calculate the hash-code of
	 * @return the hash-code
	 * @see IVector#hashCode()
	 */
	int hashCode(int position, int length);

	/**
	 * Returns an {@link IPrimitiveIterator iterator} of a collection composed of the k biggest elements in the
	 * block between {@code position} and {@code position + lgth}. The iterator iterates over the k biggest
	 * elements from the smallest element to the biggest element.
	 *
	 * @param position the starting position
	 * @param lgth the number of elements
	 * @param k the number of elements to return
	 * @return an iterator over the k biggest elements
	 */
	IPrimitiveIterator topK(int position, int lgth, int k);

	/**
	 * Returns an {@link IPrimitiveIterator iterator} of a collection composed of the k smallest elements in the
	 * block between {@code position} and {@code position + lgth}. The iterator iterates over the k smallest
	 * elements from the biggest element to the smallest element.
	 *
	 * @param position the starting position
	 * @param lgth the number of elements
	 * @param k the number of elements to return
	 * @return an iterator over the k smallest elements
	 */
	IPrimitiveIterator bottomK(int position, int lgth, int k);

	/**
	 * Returns the k indices of the k biggest elements of the vector sorted in ascending order.
	 * <p>
	 * The vector is represented by the elements between {@code position} (inclusive) and {@code position + lgth}
	 * (exclusive).
	 *
	 * @param position the position of the vector in the block
	 * @param lgth the number of components in the vector
	 * @param k the number of elements to return
	 * @return the k indices of the k biggest elements
	 */
	int[] topKIndices(int position, int lgth, int k);

	/**
	 * Returns the k indices of the k smallest elements of the vector sorted in ascending order.
	 * <p>
	 * The vector is represented by the elements between {@code position} (inclusive) and {@code position + lgth}
	 * (exclusive).
	 *
	 * @param position the position of the vector in the block
	 * @param lgth the number of components in the vector
	 * @param k the number of elements to return
	 * @return the k indices of the k smallest elements
	 */
	int[] bottomKIndices(int position, int lgth, int k);

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code double}</b> without
	 * information loss.
	 * <p>
	 * The vector is represented by the elements between {@code position} (inclusive) and {@code position + lgth}
	 * (exclusive).
	 *
	 * @param position the position of the vector in the block
	 * @param lgth the number of components in the vector
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the k indices of the k biggest elements
	 */
	double quantileDouble(int position, int lgth, double r);

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code float}</b> without
	 * information loss.
	 * <p>
	 * The vector is represented by the elements between {@code position} (inclusive) and {@code position + lgth}
	 * (exclusive).
	 *
	 * @param position the position of the vector in the block
	 * @param lgth the number of components in the vector
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the k indices of the k biggest elements
	 */
	float quantileFloat(int position, int lgth, double r);

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code long}</b> without
	 * information loss.
	 * <p>
	 * The vector is represented by the elements between {@code position} (inclusive) and {@code position + lgth}
	 * (exclusive).
	 *
	 * @param position the position of the vector in the block
	 * @param lgth the number of components in the vector
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the k indices of the k biggest elements
	 */
	long quantileLong(int position, int lgth, double r);

	/**
	 * Computes the quantile of order {@code r} using the Nearest Rank definition of quantile with rounding.
	 * <p>
	 * For instance, if {@code r = 1/2}, the function returns the median. For {@code r = 1/4}, the first quantile.
	 * For {r = 3/4}, the third quantile.
	 * <p>
	 * This method is only supported for vectors that can retrieve their content as <b>{@code int}</b> without
	 * information loss.
	 * <p>
	 * The vector is represented by the elements between {@code position} (inclusive) and {@code position + lgth}
	 * (exclusive).
	 *
	 * @param position the position of the vector in the block
	 * @param lgth the number of components in the vector
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the k indices of the k biggest elements
	 */
	int quantileInt(int position, int lgth, double r);

	/**
	 * Returns the index of the quantile of order {@code r}.
	 * <p>
	 * The vector is represented by the elements between {@code position} (inclusive) and {@code position + lgth}
	 * (exclusive).
	 *
	 * @param position the position of the vector in the block
	 * @param lgth the number of components in the vector
	 * @param r the order of the quantile, a double in {@code ]0.0, 1.0]}
	 * @return the index of quantile of order {@code r}
	 */
	int quantileIndex(int position, int lgth, double r);

	AllocationType getAllocation();
}
