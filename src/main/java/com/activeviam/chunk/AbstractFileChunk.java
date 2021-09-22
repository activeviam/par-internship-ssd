/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.reference.MemoryAllocatorWithMmap;

import java.io.Closeable;

public abstract class AbstractFileChunk<K> implements Chunk<K>, Closeable {

	private final int capacity;
	private final MemoryAllocatorWithMmap allocator;
	private final long blockSize;

	long ptr;

	public AbstractFileChunk(
			final MemoryAllocatorWithMmap allocator, final int capacity, final long blockSize) {
		this.capacity = capacity;
		this.allocator = allocator;
		this.blockSize = blockSize;
		this.ptr = allocator.allocateMemory(this.blockSize);
	}

	@Override
	public int capacity() {
		return this.capacity;
	}

	protected final long offset(final long offset) {
		return this.ptr + offset;
	}

	@Override
	public void close() {
		if (this.ptr >= 0) {
			this.allocator.freeMemory(this.ptr, this.blockSize);
			this.ptr = -1;
		} else {
			throw new IllegalStateException("Cannot free twice the same block");
		}
	}
}