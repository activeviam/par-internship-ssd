/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.reference;

import com.activeviam.MemoryAllocator;

/**
 * An allocator effective to allocate a fixed amount of memory among a reserved and sized block of
 * memory.
 *
 * @author ActiveViam
 */
public interface IBlockAllocator {

  /** Value that indicates an allocation does not succeed. */
  public static final long NULL_POINTER = 0L;

  /** @return the size of piece of memory it must allocate via {@link #allocate()}. */
  long size();

  /**
   * Allocate a fixed amount of memory within the reserved block of memory.
   *
   * @return The pointer to this allocated memory. If the allocation failed it return a {@link
   *     #NULL_POINTER}.
   */
  MemoryAllocator.ReturnValue allocate();

  void free(MemoryAllocator.ReturnValue value);

  /**
   * Release all memory reserved by this block.
   *
   * <p>That means you <b>CAN'T REUSE</b> it later on (i.e call {@link #allocate()}) and you'll need
   * to create a <b>new one</b>.
   */
  void release();
}
