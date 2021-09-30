/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam;

import com.activeviam.reference.ABlockAllocator;

import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;

public interface MemoryAllocator {

  /** The native page size. */
  long PAGE_SIZE = UnsafeUtil.pageSize();

  public class ReturnValue {

    private final ABlockAllocator blockAllocator;
    private long blockAddress;

    public ReturnValue(final ABlockAllocator blockAllocator, final long ptr) {
      this.blockAllocator = blockAllocator;
      this.blockAddress = ptr;
      this.activateBlock();
    }

    public ABlockAllocator getBlockAllocator() {
      return blockAllocator;
    }

    public long getBlockAddress() {
      return this.blockAddress & (~0x3);
    }

    public void activateBlock() {
      this.blockAddress |= 0x2;
    }

    public void desactivateBlock() {
      this.blockAddress &= (~0x2);
    }

    public boolean isActiveBlock() {
      return (this.blockAddress & 0x2) != 0;
    }

    public void dirtyBlock() {
      this.blockAddress |= 0x1;
    }

    public void cleanBlock() {
      this.blockAddress &= (~0x1);
    }

    public boolean isDirtyBlock() {
      return (this.blockAddress & 0x1) != 0;
    }

  };

  /**
   * Allocates a new block of static final memory, of the given size in bytes.
   *
   * <p>The contents of the memory are uninitialized; they will generally be garbage. The resulting
   * static final pointer will never be zero, and will be aligned for all value types.
   *
   * <p>Dispose of this memory by calling {@link #freeMemory(long, long)}.
   *
   * <p>The only constraint to the allocated memory is that it can be read an written to by {@link
   * sun.misc.Unsafe}.
   *
   * <p>Notice: An {@link OutOfMemoryError} is thrown if the allocation is refused by the system
   * because of a resource constraint.
   *
   * @param bytes The size (in bytes) of the block of memory to allocate
   * @return The pointer to this allocated memory.
   * @throws OutOfMemoryError if the allocation is refused by the system, because of a resource
   *     constraint.
   */
  ReturnValue allocateMemory(long bytes);

  /**
   * Disposes of a block of static final memory obtained from {@link #allocateMemory}.
   *
   * @see #allocateMemory
   * @param address The address of the memory block to free
   * @param bytes The number of bytes to free. It is the responsibility of the caller to make sure
   *     that bytes corresponds to the number of bytes passed to allocateMemory, otherwise memory is
   *     leaked.
   */
  void freeMemory(ReturnValue value);
}
