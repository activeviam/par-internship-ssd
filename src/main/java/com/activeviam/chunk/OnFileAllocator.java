package com.activeviam.chunk;

import com.activeviam.MemoryAllocator;

/**
 * This allocator allocates chunk on file
 *
 * @author ActiveViam
 */
public class OnFileAllocator implements IChunkAllocator {

  protected final MemoryAllocator allocator;

  public OnFileAllocator(MemoryAllocator allocator) {
    this.allocator = allocator;
  }

  @Override
  public IntegerChunk allocateIntegerChunk(int size) {
    return new FileIntegerChunk(this.allocator, size);
  }

  @Override
  public DoubleChunk allocateDoubleChunk(int size) {
    return new FileDoubleChunk(this.allocator, size);
  }
}
