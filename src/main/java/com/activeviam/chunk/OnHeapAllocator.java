package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.chunk.OnHeapAllocator.AArrayVectorAllocator.ArrayDoubleVectorAllocator;
import com.activeviam.chunk.OnHeapAllocator.AArrayVectorAllocator.ArrayIntegerVectorAllocator;
import com.activeviam.vector.ArrayDoubleVector;
import com.activeviam.vector.ArrayIntegerVector;
import com.activeviam.vector.EmptyVector;
import com.activeviam.vector.IVector;
import com.activeviam.vector.IVectorAllocator;

/**
 * This allocator allocates array-based chunks stored on the java Heap.
 *
 * @author ActiveViam
 */
public class OnHeapAllocator implements IChunkAllocator {

	final
	@Override
	public IntegerChunk allocateIntegerChunk(int size) {
		return new HeapIntegerChunk(size);
	}

	@Override
	public DoubleChunk allocateDoubleChunk(int size) {
		return new HeapDoubleChunk(size);
	}

	@Override
	public IVectorChunk allocateVectorChunk(int size, Types type) {
		return new ChunkVector(size, type, this);
	}

	@Override
	public IVectorAllocator getVectorAllocator(Types type) {
		switch (type) {
			case DOUBLE:
				return new ArrayDoubleVectorAllocator();
			case INTEGER:
				return new ArrayIntegerVectorAllocator();
			default:
				throw new IllegalStateException(
						"Unexpected type: " + type.name());
		}
	}

	@Override
	public boolean isTransient() {
		return true;
	}

	public abstract static class AArrayVectorAllocator implements IVectorAllocator {

		/**
		 * Constructor.
		 */
		protected AArrayVectorAllocator() {
		}

		@Override
		public void reallocateVector(IVector vector) {
		}

		/**
		 * An implementation of an {@link IVectorAllocator} that allocates on-heap vectors of doubles that relies on
		 * standard java arrays.
		 *
		 * @author ActiveViam
		 */
		public static class ArrayDoubleVectorAllocator extends AArrayVectorAllocator {

			@Override
			public IVector allocateNewVector(final int length) {
				if (length == 0) {
					return EmptyVector.emptyVector(getComponentType());
				}
				return new ArrayDoubleVector(new double[length]);
			}

			@Override
			public IVector copy(final IVector toCopy) {
				if (toCopy.size() == 0) {
					return EmptyVector.emptyVector(getComponentType());
				}
				return new ArrayDoubleVector(toCopy.toDoubleArray());
			}

			@Override
			public Types getComponentType() {
				return Types.DOUBLE;
			}

		}

		/**
		 * An implementation of an {@link IVectorAllocator} that allocates on-heap vectors of ints that relies on
		 * standard java arrays.
		 *
		 * @author ActiveViam
		 */
		public static class ArrayIntegerVectorAllocator extends AArrayVectorAllocator {

			@Override
			public IVector allocateNewVector(final int length) {
				if (length == 0) {
					return EmptyVector.emptyVector(getComponentType());
				}
				return new ArrayIntegerVector(new int[length]);
			}

			@Override
			public IVector copy(final IVector toCopy) {
				if (toCopy.size() == 0) {
					return EmptyVector.emptyVector(getComponentType());
				}
				return new ArrayIntegerVector(toCopy.toIntArray());
			}

			@Override
			public Types getComponentType() {
				return Types.INTEGER;
			}

		}

	}
}
