/*
 * (C) ActiveViam 2016
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.block.IBlock;

/**
 * {@link IVector vectors} that takes note of how many times its data is written to the datastore. When the counter
 * reaches zero, the vector will be marked as destroyed and any attempt to read its data will throw an exception.
 * This mechanism guarantees that reading a vector's content will either return the
 * expected data or throw an exception but will never crash the JVM if the vector is destroyed during the
 * reading of its content.
 * <p>
 * This interface is voluntarily introduced to hide the mechanism of maintaining the counter from final users who
 * export a vector from the datastore to read its content.
 *
 * @author ActiveViam
 */
public interface IReleasableVector extends IVector {

	/**
	 * Marks that the vector is referenced (i.e. it is stored in a chunk) to prevent it from being destroyed.
	 * <p>
	 * When the vector is removed from a chunk, {@link #releaseReference()} should be called to help determining if
	 * it is safe to destroy the vector.
	 */
	void acquireReference();

	/**
	 * Marks that the vector is no longer referenced in a chunk.
	 * <p>
	 * When the vector appears in no chunk, it will be considered as dead. It will call {@link IBlock#release(int)}
	 * to drop its acquire on its block
	 */
	void releaseReference();

	/**
	 * Signals the vector to try to collect itself.
	 */
	void collect();

}
