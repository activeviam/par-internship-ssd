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
public class MaxHeapIntegerWithIndices implements IPrimitiveIterator {

	/**
	 * The two children of {@code queue[n]} are {@code queue[2*n+1]} and {@code queue[2*(n+1)]}. For
	 * each node {@code n} in the heap and each descendant {@code d} of {@code n}, {@code n <= d}.
	 * The element with the lowest value is in {@code queue[0]}, assuming the queue is not empty.
	 */
	protected final int[] queue;

	/**
	 * A secondary array mapped to {@link #queue} i.e the i<sup>th</sup> element of {@link #queue}
	 * correspond to the i<sup>th</sup> element of {@link #queueIndices}.
	 */
	protected final int[] queueIndices;

	/** The number of elements in the priority queue. */
	protected int size = 0;

	/**
	 * Constructs a heap with a maximum capacity.
	 *
	 * @param capacity the maximum capacity of the heap
	 */
	public MaxHeapIntegerWithIndices(int capacity) {
		this.queue = new int[capacity];
		this.queueIndices = new int[capacity];
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
	 * Retrieves the underlying array of indices.
	 *
	 * <p>Calling this method only makes sense if the heap has reached max size.
	 *
	 * @return the underlying array of indices
	 */
	public int[] getArrayIndices() {
		return this.queueIndices;
	}

	/**
	 * Adds a new int into the heap.
	 *
	 * @param value the new int to add
	 * @param index an integer associated to the new {@code value} that follows its movement in the
	 *        heap in a dedicated queue, if {@code i} is an integer between {@code 0} and
	 *        {@link #size},
	 *        <ul>
	 *         <li>{@code value = }{@link #queue queue}{@code [i];}
	 *         <li>{@code index = }{@link #queueIndices queueIndices}{@code [i];}
	 *        </ul>
	 *        it is typically used to store the index of the value coming from an array
	 */
	public void add(final int value, final int index) {
		int pos = this.size;
		// Bubble up
		while (pos > 0) {
			final int parent = (pos - 1) >>> 1;
			final int p = queue[parent];
			final int indexParent = queueIndices[parent];
			if (value <= p) {
				break;
			}
			// Swap with the parent
			queue[pos] = p;
			queueIndices[pos] = indexParent;
			pos = parent;
		}
		queue[pos] = value;
		queueIndices[pos] = index;
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
		final int lastIndex = queueIndices[size];

		if (size != 0) {
			int idx = 0;
			final int parentOfLast = size >>> 1;
			// While not a leaf
			while (idx < parentOfLast) {
				// Pick the child to promote as new parent
				int toPromoteChild = (idx << 1) + 1;
				int c = queue[toPromoteChild];
				int queueIndex = queueIndices[toPromoteChild];
				final int right = toPromoteChild + 1;
				// If the right element is smaller, it's the one we should promote
				if (right < size && c < queue[right]) {
					toPromoteChild = right;
					c = queue[right];
					queueIndex = queueIndices[right];
				}
				if (last >= c) {
					break;
				}
				// Promote the child
				queue[idx] = c;
				queueIndices[idx] = queueIndex;
				idx = toPromoteChild;
			}
			queue[idx] = last;
			queueIndices[idx] = lastIndex;
		}
		return result;
	}

	/**
	 * Sorts into ascending numerical order the underlying array.
	 * The sort is done on the values mapped with the underlying array of indices.
	 *
	 * <p><b>WARNING!</b> Once this method has been called, it is not possible anymore to add
	 * elements to the heap.
	 */
	public void sort() {
		for (int i = 0; i < queue.length; ++i) {
			int index = queueIndices[0]; // to call before poll()
			queue[queue.length - 1 - i] = poll();
			queueIndices[queueIndices.length - 1 - i] = index;
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

	/**
	 * Gets the index of the biggest element of the heap.
	 *
	 * @return the index of the biggest element of the heap
	 */
	public int peekIndex() {
		return queueIndices[0];
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
			final int indice = queueIndices[position];
			final int value = poll();
			queue[position] = value;
			queueIndices[position] = indice;
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
		return "Values: " + Arrays.toString(Arrays.copyOf(queue, size))
				+ " - Indices: " + Arrays.toString(Arrays.copyOf(queueIndices, size));
	}

}
