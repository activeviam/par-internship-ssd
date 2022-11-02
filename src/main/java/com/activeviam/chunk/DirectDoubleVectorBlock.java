/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.allocator.MemoryAllocator;
import com.activeviam.iterator.IPrimitiveIterator;
import com.activeviam.vector.IVector;

/**
 * @author ActiveViam
 */
public class DirectDoubleVectorBlock extends ADirectVectorBlock {

	public DirectDoubleVectorBlock(final MemoryAllocator allocator, final int capacity) {
		super(allocator, capacity, Types.DOUBLE);
	}

	@Override
	public Types getComponentType() {
		return null;
	}

	@Override
	public int hashCode(int position, int length) {
		return 0;
	}

	@Override
	public IPrimitiveIterator topK(int position, int lgth, int k) {
		return null;
	}

	@Override
	public IPrimitiveIterator bottomK(int position, int lgth, int k) {
		return null;
	}

	@Override
	public int[] topKIndices(int position, int lgth, int k) {
		return new int[0];
	}

	@Override
	public int[] bottomKIndices(int position, int lgth, int k) {
		return new int[0];
	}

	@Override
	public int quantileIndex(int position, int lgth, double r) {
		return 0;
	}

	@Override
	public IVector readVector(int position) {
		return null;
	}

	@Override
	public Runnable destroy() {
		return null;
	}

	@Override
	public void writeVector(int position, IVector vector) {

	}
}
