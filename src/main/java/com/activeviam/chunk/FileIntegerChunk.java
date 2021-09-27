/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.UnsafeUtil;
import com.activeviam.reference.MemoryAllocatorWithMmap;

import java.util.BitSet;
import java.util.logging.Logger;

import static com.activeviam.MemoryAllocator.PAGE_SIZE;

public class FileIntegerChunk extends AbstractFileChunk<Integer> implements IntegerChunk {

	/** Unsafe provider. */
	private static final sun.misc.Unsafe UNSAFE = UnsafeUtil.getUnsafe();

	/** The order of the size in bytes of an element. */
	private static final int ELEMENT_SIZE_ORDER = 2;

	public FileIntegerChunk(final MemoryAllocatorWithMmap allocator, final int capacity) {
		super(allocator, capacity, computeBlockSize(capacity));
	}

	private static long computeBlockSize(final int capacity) {
		final var minSize = capacity << ELEMENT_SIZE_ORDER;
		if (minSize % PAGE_SIZE == 0) {
			return minSize;
		} else {
			// Find the closest multiple of PAGE_SIZE
			final var size = ((minSize / PAGE_SIZE) + 1) * PAGE_SIZE;
			Logger.getLogger("chunk").warning("Wasting " + (size - minSize) + " bytes");
			return size;
		}
	}

	@Override
	public int readInt(int position) {
		assert 0 <= position && position < capacity();
		return UNSAFE.getInt(offset(position << ELEMENT_SIZE_ORDER));
	}

	@Override
	public void writeInt(int position, int value) {
		assert 0 <= position && position < capacity();
		UNSAFE.putInt(offset(position << ELEMENT_SIZE_ORDER), value);
	}

	@Override
	public BitSet findRows(int value, int limit) {
		assert limit <= capacity();

		BitSet result = null;
		long addr = offset(0);
		for (int i = 0; i < limit; i++) {
			if (UNSAFE.getInt(addr) == value) {
				if (result == null) {
					result = new BitSet();
				}
				result.set(i);
			}

			addr += 1 << ELEMENT_SIZE_ORDER;
		}
		return result;
	}

}
