package com.activeviam.chunk;

/**
 * This allocator allocate chunk on the java Heap.
 *
 * @author ActiveViam
 */
public class OnHeapAllocator implements IChunkAllocator {

  @Override
  public IntegerChunk allocateIntegerChunk(int size) {
    return new HeapIntegerChunk(size);
  }

  @Override
  public DoubleChunk allocateDoubleChunk(int size) {
    return new HeapDoubleChunk(size);
  }
}
