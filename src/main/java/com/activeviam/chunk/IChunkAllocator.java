package com.activeviam.chunk;

/**
 * @author ActiveViam
 */
public interface IChunkAllocator {

	IntegerChunk allocateIntergerChunk(int size);

	DoubleChunk allocateDoubleChunk(int size);

}
