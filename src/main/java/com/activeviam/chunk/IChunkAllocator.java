package com.activeviam.chunk;

/** @author ActiveViam */
public interface IChunkAllocator {

  IntegerChunk allocateIntegerChunk(int size);

  DoubleChunk allocateDoubleChunk(int size);
}
