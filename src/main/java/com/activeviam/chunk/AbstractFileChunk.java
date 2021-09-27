/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.MemoryAllocator;
import com.activeviam.reference.MemoryAllocatorWithMmap;

import java.io.Closeable;

public abstract class AbstractFileChunk<K> implements Chunk<K>, Closeable {

	private final int capacity;
	private final long blockSize;
	private MemoryAllocator.ReturnValue allocatorValue;

	public AbstractFileChunk(
			final MemoryAllocatorWithMmap allocator, final int capacity, final long blockSize) {
		this.capacity = capacity;
		this.blockSize = blockSize;
		this.allocatorValue = allocator.allocateMemory(this.blockSize);
	}

	@Override
	public int capacity() {
		return this.capacity;
	}

	protected final long offset(final long offset) {
		return this.allocatorValue.getBlockAddress() + offset;
	}

	@Override
	public void close() {
		if (this.allocatorValue.getBlockAddress() >= 0) {
			this.allocatorValue.getBlockAllocator().free(this.allocatorValue);
		} else {
			throw new IllegalStateException("Cannot free twice the same block");
		}
	}
}
