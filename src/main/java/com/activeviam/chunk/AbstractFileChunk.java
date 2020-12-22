/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.MemoryAllocator;
import java.io.Closeable;

abstract class AbstractFileChunk<K> implements Chunk<K>, Closeable {

  private final int capacity;

  private final MemoryAllocator allocator;
  long ptr;
  private final long blockSize;

  public AbstractFileChunk(
      final MemoryAllocator allocator, final int capacity, final long blockSize) {
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
