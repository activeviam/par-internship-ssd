/*
 * (C) ActiveViam 2007-2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.heap;

import com.activeviam.iterator.IPrimitiveIterator;
import com.activeviam.vector.ArrayIntegerVector;
import com.activeviam.vector.ITransientVector;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * A heap with the ability to store primitive ints. A heap is a tree-based data-structure
 * where each of the parent nodes is bigger (max heap) or tinier (min heap) than its parents.
 *
 * <p>The underlying data is stored inside an array of the same size as the heap.
 * It can be retrieved without cloning.
 *
 * @author ActiveViam
 */
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//    / \
//   / ! \
//  /  !  \  Generated code, do not hand edit!!!!!!!!!!!!!!!
// /   !   \
// ---------
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// See com.qfs.heap.compilation.impl.HeapCompiler
public class MaxHeapInteger implements IPrimitiveIterator {

	/**
	 * The two children of {@code queue[n]} are {@code queue[2*n+1]} and {@code queue[2*(n+1)]}. For
	 * each node {@code n} in the heap and each descendant {@code d} of {@code n}, {@code n <= d}.
	 * The element with the lowest value is in {@code queue[0]}, assuming the queue is not empty.
	 */
	protected final int[] queue;

	/** The number of elements in the priority queue. */
	protected int size = 0;

	/**
	 * Constructs a heap with a maximum capacity.
	 *
	 * @param capacity the maximum capacity of the heap
	 */
	public MaxHeapInteger(int capacity) {
		this.queue = new int[capacity];
	}

	/**
	 * Retrieves the underlying array.
	 *
	 * <p>Calling this method only makes sense if the heap has reached max size.
	 *
	 * @return the underlying array
	 */
	public int[] getArray() {
		return this.queue;
	}

	/**
	 * Adds a new int into the heap.
	 *
	 * @param value the new int to add
	 */
	public void add(final int value) {
		int pos = this.size;
		// Bubble up
		while (pos > 0) {
			final int parent = (pos - 1) >>> 1;
			final int p = queue[parent];
			if (value <= p) {
				break;
			}
			// Swap with the parent
			queue[pos] = p;
			pos = parent;
		}
		queue[pos] = value;
		++this.size;
	}

	/**
	 * Retrieves and removes the biggest element of the heap.
	 *
	 * @return the biggest element of the heap
	 */
	public int poll() {
		final int size = --this.size;
		final int result = queue[0];
		final int last = queue[size];

		if (size != 0) {
			int idx = 0;
			final int parentOfLast = size >>> 1;
			// While not a leaf
			while (idx < parentOfLast) {
				// Pick the child to promote as new parent
				int toPromoteChild = (idx << 1) + 1;
				int c = queue[toPromoteChild];
				final int right = toPromoteChild + 1;
				// If the right element is smaller, it's the one we should promote
				if (right < size && c < queue[right]) {
					toPromoteChild = right;
					c = queue[right];
				}
				if (last >= c) {
					break;
				}
				// Promote the child
				queue[idx] = c;
				idx = toPromoteChild;
			}
			queue[idx] = last;
		}
		return result;
	}

	/**
	 * Sorts into ascending numerical order the underlying array.
	 *
	 * <p><b>WARNING!</b> Once this method has been called, it is not possible anymore to add
	 * elements to the heap.
	 */
	public void sort() {
		for (int i = 0; i < queue.length; ++i) {
			queue[queue.length - 1 - i] = poll();
		}
	}

	/**
	 * Gets the biggest element of the heap.
	 *
	 * @return the biggest element of the heap
	 */
	public int peek() {
		return queue[0];
	}

	@Override
	public double nextDouble() {
		throw new UnsupportedOperationException(
				"This method is not implemented for this class " + getClass().getName());
	}

	@Override
	public float nextFloat() {
		throw new UnsupportedOperationException(
				"This method is not implemented for this class " + getClass().getName());
	}

	@Override
	public int nextInt() {
		if (size > 0) {
			final int position = size - 1;
			final int value = poll();
			queue[position] = value;
			return value;
		} else {
			throw new NoSuchElementException("Cannot access next int in the vector.");
		}
	}

	@Override
	public long nextLong() {
		return nextInt();
	}

	@Override
	public ITransientVector getUnderlyingArray() {
		return new ArrayIntegerVector(queue);
	}

	@Override
	public boolean hasNext() {
		return size != 0;
	}

	/**
	 * Gets the current size of the heap.
	 *
	 * @return the current size of the heap
	 */
	public int size() {
		return this.size;
	}

	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOf(queue, size));
	}

}
