/*
 * (C) ActiveViam 2016
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

/**
 * A special interface for transient vectors. Those vectors are stored on-heap, and are typically not held by long
 * living data structures like columns.
 * <p>
 * They can be safely sorted in place.
 *
 * @author ActiveViam
 */
public interface ITransientVector extends IReleasableVector {

	/**
	 * Sorts the content of this vector in place.
	 * <p>
	 * This typically has a better performance than {@link IVector#sort()} that needs to involve cloning.
	 */
	void sortInPlace();

	/**
	 * Reverses this vector, exchanging element {@code k} with element {@code size() - k - 1}.
	 */
	void reverse();

}
