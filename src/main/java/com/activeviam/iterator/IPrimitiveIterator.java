/*
 * (C) ActiveViam 2016
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.iterator;


import com.activeviam.vector.ITransientVector;

/**
 * A primitive iterator interface.
 *
 * @author ActiveViam
 */
public interface IPrimitiveIterator {

	/**
	 * Returns whether or not the iterator has more elements.
	 *
	 * @return {@code true} if the iterator has more elements, {@code false} otherwise
	 */
	boolean hasNext();

	/**
	 * Returns the next integer.
	 *
	 * @return the next integer
	 */
	int nextInt();

	/**
	 * Returns the next long.
	 *
	 * @return the next long
	 */
	long nextLong();

	/**
	 * Returns the next float.
	 *
	 * @return the next float
	 */
	float nextFloat();

	/**
	 * Returns the next double.
	 *
	 * @return the next double
	 */
	double nextDouble();

	/**
	 * Gets a vector based on this iterator.
	 *
	 * @return a vector
	 */
	ITransientVector getUnderlyingArray();

}
