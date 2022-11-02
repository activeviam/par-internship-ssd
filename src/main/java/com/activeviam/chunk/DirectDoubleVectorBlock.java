/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.allocator.MemoryAllocator;
import com.activeviam.heap.MaxHeapDouble;
import com.activeviam.heap.MaxHeapDoubleWithIndices;
import com.activeviam.heap.MinHeapDouble;
import com.activeviam.heap.MinHeapDoubleWithIndices;
import com.activeviam.iterator.IPrimitiveIterator;

/**
 * @author ActiveViam
 */
public class DirectDoubleVectorBlock extends ADirectVectorBlock {

	public DirectDoubleVectorBlock(final MemoryAllocator allocator, final int capacity) {
		super(allocator, capacity, Types.DOUBLE);
	}

	@Override
	public void transfer(final int position, final double[] dest) {
		// Don't use Unsafe.copyMemory : there is no reason for the GC to not move things while
		// we are reading.
		final int lgth = dest.length;
		long ptr = getAddress() + (position << 3);
		for (int i = 0; i < lgth; ++i, ptr += 8) {
			dest[i] = UNSAFE.getDouble(ptr);
		}
	}

	@Override
	public double readDouble(final int position) {
		return UNSAFE.getDouble(getAddress() + (position << 3));
	}

	@Override
	public void write(final int position, final double[] src) {
		// Don't use Unsafe.copyMemory : there is no reason for the GC to not move things while
		// we are reading.
		final int lgth = src.length;
		long ptr = getAddress() + (position << 3);
		for (int i = 0; i < lgth; ++i, ptr += 8) {
			UNSAFE.putDouble(ptr, src[i]);
		}

	}

	@Override
	public void write(final int position, final float[] src) {
		// Don't use Unsafe.copyMemory : there is no reason for the GC to not move things while
		// we are reading.
		final int lgth = src.length;
		long ptr = getAddress() + (position << 3);
		for (int i = 0; i < lgth; ++i, ptr += 8) {
			UNSAFE.putDouble(ptr, src[i]);
		}
	}

	@Override
	public void write(final int position, final long[] src) {
		// Don't use Unsafe.copyMemory : there is no reason for the GC to not move things while
		// we are reading.
		final int lgth = src.length;
		long ptr = getAddress() + (position << 3);
		for (int i = 0; i < lgth; ++i, ptr += 8) {
			UNSAFE.putDouble(ptr, src[i]);
		}
	}

	@Override
	public void write(final int position, final int[] src) {
		// Don't use Unsafe.copyMemory : there is no reason for the GC to not move things while
		// we are reading.
		final int lgth = src.length;
		long ptr = getAddress() + (position << 3);
		for (int i = 0; i < lgth; ++i, ptr += 8) {
			UNSAFE.putDouble(ptr, src[i]);
		}
	}

	@Override
	public void writeDouble(final int position, final double v) {
		UNSAFE.putDouble(getAddress() + (position << 3), v);
	}

	@Override
	public void writeFloat(final int position, final float v) {
		writeDouble(position, v);
	}

	@Override
	public void addDouble(final int position, final double addedValue) {
		final long ptr = getAddress() + (position << 3);
		UNSAFE.putDouble(ptr, UNSAFE.getDouble(ptr) + addedValue);
	}

	@Override
	public void addFloat(final int position, final float addedValue) {
		addDouble(position, addedValue);
	}

	@Override
	public void fillDouble(final int position, final int lgth, final double v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v);
		}
	}

	@Override
	public void fillFloat(final int position, final int lgth, final float v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v);
		}
	}

	@Override
	public void fillLong(final int position, final int lgth, final long v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v);
		}
	}

	@Override
	public void fillInt(final int position, final int lgth, final int v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v);
		}
	}

	@Override
	public void scale(final int position, final int lgth, final double v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v * UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public void scale(final int position, final int lgth, final float v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v * UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public void scale(final int position, final int lgth, final long v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v * UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public void scale(final int position, final int lgth, final int v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v * UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public void translate(final int position, final int lgth, final double v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v + UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public void translate(final int position, final int lgth, final float v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v + UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public void translate(final int position, final int lgth, final long v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v + UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public void translate(final int position, final int lgth, final int v) {
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			UNSAFE.putDouble(ptr, v + UNSAFE.getDouble(ptr));
		}
	}

	@Override
	public int hashCode(final int position, final int lgth) {
		int result = 1;
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);
		for (; ptr < end; ptr += 8) {
			long bits = Double.doubleToLongBits(UNSAFE.getDouble(ptr));
			result = 31 * result + (int) (bits ^ (bits >>> 32));
		}

		return result;
	}

	@Override
	public IPrimitiveIterator topK(final int position, final int lgth, final int k) {
		return topKMinHeapDouble(position, lgth, k);
	}

	/**
	 * A min heap containing the smallest k elements.
	 *
	 * @param position the position in the block
	 * @param lgth the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MinHeapDouble topKMinHeapDouble(final int position, final int lgth, final int k) {
		final MinHeapDouble h = new MinHeapDouble(k);
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);

		for (; ptr < end; ptr += 8) {
			final int s = h.size();
			final double item = UNSAFE.getDouble(ptr);
			if (s < k || item > h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item);
			}
		}

		return h;
	}

	/**
	 * A min heap containing the smallest k elements and their indices.
	 *
	 * @param position the position in the block
	 * @param lgth the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MinHeapDoubleWithIndices topKMinHeapWithIndicesDouble(
			final int position,
			final int lgth,
			final int k) {

		final MinHeapDoubleWithIndices h = new MinHeapDoubleWithIndices(k);
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);

		int i = 0;
		for (; ptr < end; ptr += 8) {
			final int s = h.size();
			final double item = UNSAFE.getDouble(ptr);
			if (s < k || item > h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item, i);
			}
			i++;
		}

		return h;
	}

	@Override
	public IPrimitiveIterator bottomK(final int position, final int lgth, final int k) {
		return bottomKMaxHeapDouble(position, lgth, k);
	}

	/**
	 * A max heap containing the smallest k elements.
	 *
	 * @param position the position in the block
	 * @param lgth the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MaxHeapDouble bottomKMaxHeapDouble(final int position, final int lgth, final int k) {
		final MaxHeapDouble h = new MaxHeapDouble(k);
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);

		for (; ptr < end; ptr += 8) {
			final int s = h.size();
			final double item = UNSAFE.getDouble(ptr);
			if (s < k || item < h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item);
			}
		}

		return h;
	}

	/**
	 * A max heap containing the smallest k elements and their indices.
	 *
	 * @param position the position in the block
	 * @param lgth the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MaxHeapDoubleWithIndices bottomKMaxHeapWithIndicesDouble(
			final int position,
			final int lgth,
			final int k) {

		final MaxHeapDoubleWithIndices h = new MaxHeapDoubleWithIndices(k);
		long ptr = getAddress() + (position << 3);
		final long end = ptr + (lgth << 3);

		int i = 0;
		for (; ptr < end; ptr += 8) {
			final int s = h.size();
			final double item = UNSAFE.getDouble(ptr);
			if (s < k || item < h.peek()) {
				if (s == k) {
					h.poll();
				}
				h.add(item, i);
			}
			i++;
		}

		return h;
	}

	@Override
	public int[] topKIndices(final int position, final int lgth, final int k) {
		final MinHeapDoubleWithIndices h = topKMinHeapWithIndicesDouble(position, lgth, k);
		h.sort();
		return h.getArrayIndices();
	}

	@Override
	public int[] bottomKIndices(final int position, final int lgth, final int k) {
		final MaxHeapDoubleWithIndices h = bottomKMaxHeapWithIndicesDouble(position, lgth, k);
		h.sort();
		return h.getArrayIndices();
	}

	@Override
	public double quantileDouble(final int position, final int lgth, final double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException(
					"Order of the quantile should be greater than zero and less than 1.");
		}

		if (r >= 0.5) {
			return topKMinHeapDouble(position, lgth, lgth - nearestRank(lgth, r) + 1).peek();
		} else {
			return bottomKMaxHeapDouble(position, lgth, nearestRank(lgth, r)).peek();
		}
	}

	@Override
	public int quantileIndex(final int position, final int lgth, final double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException(
					"Order of the quantile should be greater than zero and less than 1.");
		}

		if (r >= 0.5) {
			return topKMinHeapWithIndicesDouble(position, lgth, lgth - nearestRank(lgth, r) + 1).peekIndex();
		} else {
			return bottomKMaxHeapWithIndicesDouble(position, lgth, nearestRank(lgth, r)).peekIndex();
		}
	}

}
