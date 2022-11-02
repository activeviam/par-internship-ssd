/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.allocator.MemoryAllocator;
import com.activeviam.heap.MaxHeapInteger;
import com.activeviam.heap.MaxHeapIntegerWithIndices;
import com.activeviam.heap.MinHeapInteger;
import com.activeviam.heap.MinHeapIntegerWithIndices;
import com.activeviam.iterator.IPrimitiveIterator;

/**
 * @author ActiveViam
 */
public class DirectIntegerVectorBlock extends ADirectVectorBlock {

	public DirectIntegerVectorBlock(final MemoryAllocator allocator, final int capacity) {
		super(allocator, capacity, Types.DOUBLE);
	}

	@Override
	public void transfer(final int position, final double[] dest) {
		// Don't use Unsafe.copyMemory : there is no reason for the GC to not move things while
		// we are reading.
		final int lgth = dest.length;
		long ptr = getAddress() + (position << 2);
		for (int i = 0; i < lgth; ++i, ptr += 4) {
			dest[i] = UNSAFE.getInt(ptr);
		}
	}

	@Override
	public double readDouble(final int position) {
		return readInt(position);
	}

	@Override
	public void write(final int position, final int[] src) {
		// Don't use Unsafe.copyMemory : there is no reason for the GC to not move things while
		// we are reading.
		final int lgth = src.length;
		long ptr = getAddress() + (position << 2);
		for (int i = 0; i < lgth; ++i, ptr += 4) {
			UNSAFE.putInt(ptr, src[i]);
		}
	}

	@Override
	public void fillInt(final int position, final int lgth, final int v) {
		long ptr = getAddress() + (position << 2);
		final long end = ptr + (lgth << 2);
		for (; ptr < end; ptr += 4) {
			UNSAFE.putInt(ptr, v);
		}
	}

	@Override
	public void scale(final int position, final int lgth, final int v) {
		long ptr = getAddress() + (position << 2);
		final long end = ptr + (lgth << 2);
		for (; ptr < end; ptr += 4) {
			UNSAFE.putInt(ptr, v * UNSAFE.getInt(ptr));
		}
	}

	@Override
	public void translate(final int position, final int lgth, final int v) {
		long ptr = getAddress() + (position << 2);
		final long end = ptr + (lgth << 2);
		for (; ptr < end; ptr += 4) {
			UNSAFE.putInt(ptr, v + UNSAFE.getInt(ptr));
		}
	}

	@Override
	public int hashCode(final int position, final int lgth) {
		int result = 1;
		long ptr = this.ptr + (position << 2);
		final long end = ptr + (lgth << 2);
		for (; ptr < end; ptr += 4) {
			int bits = UNSAFE.getInt(ptr);
			result = 31 * result + bits;
		}

		return result;
	}

	@Override
	public IPrimitiveIterator topK(final int position, final int lgth, final int k) {
		return topKMinHeapInteger(position, lgth, k);
	}

	/**
	 * A min heap containing the smallest k elements.
	 *
	 * @param position the position in the block
	 * @param lgth the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MinHeapInteger topKMinHeapInteger(final int position, final int lgth, final int k) {
		final MinHeapInteger h = new MinHeapInteger(k);
		long ptr = getAddress() + (position << 2);
		final long end = ptr + (lgth << 2);

		for (; ptr < end; ptr += 4) {
			final int s = h.size();
			final int item = UNSAFE.getInt(ptr);
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
	protected MinHeapIntegerWithIndices topKMinHeapWithIndicesInteger(
			final int position,
			final int lgth,
			final int k) {

		final MinHeapIntegerWithIndices h = new MinHeapIntegerWithIndices(k);
		long ptr = getAddress() + (position << 2);
		final long end = ptr + (lgth << 2);

		int i = 0;
		for (; ptr < end; ptr += 4) {
			final int s = h.size();
			final int item = UNSAFE.getInt(ptr);
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
		return bottomKMaxHeapInteger(position, lgth, k);
	}

	/**
	 * A max heap containing the smallest k elements.
	 *
	 * @param position the position in the block
	 * @param lgth the length of the vector
	 * @param k the number of elements to return in the heap
	 * @return the heap
	 */
	protected MaxHeapInteger bottomKMaxHeapInteger(final int position, final int lgth, final int k) {
		final MaxHeapInteger h = new MaxHeapInteger(k);
		long ptr = getAddress() + (position << 2);
		final long end = ptr + (lgth << 2);

		for (; ptr < end; ptr += 8) {
			final int s = h.size();
			final int item = UNSAFE.getInt(ptr);
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
	protected MaxHeapIntegerWithIndices bottomKMaxHeapWithIndicesInteger(
			final int position,
			final int lgth,
			final int k) {

		final MaxHeapIntegerWithIndices h = new MaxHeapIntegerWithIndices(k);
		long ptr = getAddress() + (position << 2);
		final long end = ptr + (lgth << 2);

		int i = 0;
		for (; ptr < end; ptr += 8) {
			final int s = h.size();
			final int item = UNSAFE.getInt(ptr);
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
		final MinHeapIntegerWithIndices h = topKMinHeapWithIndicesInteger(position, lgth, k);
		h.sort();
		return h.getArrayIndices();
	}

	@Override
	public int[] bottomKIndices(final int position, final int lgth, final int k) {
		final MaxHeapIntegerWithIndices h = bottomKMaxHeapWithIndicesInteger(position, lgth, k);
		h.sort();
		return h.getArrayIndices();
	}

	@Override
	public int quantileInt(final int position, final int lgth, final double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException(
					"Order of the quantile should be greater than zero and less than 1.");
		}

		if (r >= 0.5) {
			return topKMinHeapInteger(position, lgth, lgth - nearestRank(lgth, r) + 1).peek();
		} else {
			return bottomKMaxHeapInteger(position, lgth, nearestRank(lgth, r)).peek();
		}
	}

	@Override
	public long quantileLong(final int position, final int lgth, final double r) {
		return quantileInt(position, lgth, r);
	}

	@Override
	public int quantileIndex(final int position, final int lgth, final double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException(
					"Order of the quantile should be greater than zero and less than 1.");
		}

		if (r >= 0.5) {
			return topKMinHeapWithIndicesInteger(position, lgth, lgth - nearestRank(lgth, r) + 1).peekIndex();
		} else {
			return bottomKMaxHeapWithIndicesInteger(position, lgth, nearestRank(lgth, r)).peekIndex();
		}
	}

}
