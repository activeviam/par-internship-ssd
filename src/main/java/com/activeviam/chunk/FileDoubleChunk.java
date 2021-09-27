/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.UnsafeUtil;
import com.activeviam.reference.MemoryAllocatorWithMmap;

import java.util.logging.Logger;

import static com.activeviam.MemoryAllocator.PAGE_SIZE;

public class FileDoubleChunk extends AbstractFileChunk<Double> implements DoubleChunk {

  /** The order of the size in bytes of an element. */
  private static final int ELEMENT_SIZE_ORDER = 3;

  public FileDoubleChunk(final MemoryAllocatorWithMmap allocator, final int capacity) {
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
  public double readDouble(int position) {
    assert 0 <= position && position < capacity();
    return UNSAFE.getDouble(offset(position << ELEMENT_SIZE_ORDER));
  }

  @Override
  public void writeDouble(int position, double value) {
    assert 0 <= position && position < capacity();
    UNSAFE.putDouble(offset(position << ELEMENT_SIZE_ORDER), value);
  }

  /** Unsafe provider. */
  private static final sun.misc.Unsafe UNSAFE = UnsafeUtil.getUnsafe();
}
