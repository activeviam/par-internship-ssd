/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.allocator.MemoryAllocator;
import com.activeviam.allocator.UnsafeNativeMemoryAllocator;
import com.activeviam.block.IBlock;
import com.activeviam.vector.AFixedBlockVector;
import com.activeviam.vector.DoubleFixedBlockVector;
import com.activeviam.vector.EmptyVector;
import com.activeviam.vector.IVector;
import com.activeviam.vector.IVectorAllocator;

/**
 * A memory allocator that allocates direct memory (off-heap) everything via Unsafe.
 *
 * @author ActiveViam
 */
public class DirectMemoryAllocator implements IChunkAllocator {

	private final MemoryAllocator allocator = new UnsafeNativeMemoryAllocator();

	@Override
	public IntegerChunk allocateIntegerChunk(int size) {
		return null;
	}

	@Override
	public DoubleChunk allocateDoubleChunk(int size) {
		return null;
	}

	@Override
	public IVectorChunk allocateVectorChunk(int size, Types type) {
		return new ChunkVector(size, type, this);
	}

	@Override
	public IVectorAllocator getVectorAllocator(Types type) {
		switch (type) {
			case DOUBLE:
				return new DirectDoubleVectorAllocator();
			case INTEGER:
				return new DirectIntegerVectorAllocator();
			default:
				throw new IllegalStateException(
						"Unexpected type: " + type.name());
		}
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	/**
	 * Basic implementation of an {@link IVectorAllocator}. This implementation is based on the block paradigm: each
	 * block includes exactly the same number of elements the vector has i.e. the block size is equal to the vector length.
	 * <p>
	 * The reference to its blocks are only kept by the allocated vectors, meaning that once all the vectors within a
	 * block are garbage collected, the block will be garbage collected as well.
	 *
	 * @author ActiveViam
	 * @param <B> the type of the blocks backing the pool (containing the data)
	 */
	public abstract static class ABlockVectorAllocator<B extends IBlock> implements IVectorAllocator {

		@Override
		public IVector allocateNewVector(int length) {
			if (length == 0) {
				return EmptyVector.emptyVector(getComponentType());
			}
			final var block = allocateBlock(length);
			// we always start the allocation at the start of the block
			final IVector vector = createVector(block, length);
			block.acquire();
			return vector;
		}

		/**
		 * Creates a new vector on a given block.
		 *
		 * @param block the block on which to create the vector
		 * @param length the length of the vector, in number of components
		 * @return the newly created vector
		 */
		protected abstract AFixedBlockVector createVector(B block, int length);

		/**
		 * Allocates a new block.
		 * <p>
		 * This method also decrements the reference count of the given {@code lastBlock} making it as destroyable if
		 * all vectors stored in it are all dead.
		 *
		 * @param capacity the block capacity, in number of components
		 * @return the newly allocated block
		 */
		protected abstract B allocateBlock(int capacity);

		@Override
		public IVector copy(IVector toCopy) {
			if (toCopy == null) {
				return null;
			}
			final IVector clone = allocateNewVector(toCopy.size());
			clone.copyFrom(toCopy);
			return clone;
		}
	}

	/**
	 * An implementation of an {@link IVectorAllocator} that allocates off-heap vectors of doubles via Unsafe.
	 *
	 * @author ActiveViam
	 */
	public class DirectDoubleVectorAllocator extends ABlockVectorAllocator<DirectDoubleVectorBlock> implements IVectorAllocator {

		@Override
		public void reallocateVector(IVector vector) {

		}

		@Override
		public Types getComponentType() {
			return null;
		}

		@Override
		protected AFixedBlockVector createVector(DirectDoubleVectorBlock block, int length) {
			return new DoubleFixedBlockVector(block, 0, length);
		}

		@Override
		protected DirectDoubleVectorBlock allocateBlock(int capacity) {
			return new DirectDoubleVectorBlock(DirectMemoryAllocator.this.allocator, capacity);
		}
	}
}
