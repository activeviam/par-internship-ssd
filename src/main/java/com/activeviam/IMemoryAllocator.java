/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam;

public interface IMemoryAllocator {

  /** The native page size. */
  long PAGE_SIZE = UnsafeUtil.pageSize();
  /** Special value representing invalid address */
  long NULL_POINTER = 0L;

  class ReturnValue {

    private final Object metadata;
    private long blockAddress;

    public ReturnValue(final Object metadata, final long ptr) {
      this.metadata = metadata;
      this.blockAddress = ptr;
    }

    public Object getMetadata() {
      return metadata;
    }

    public long getBlockAddress() {
      return this.blockAddress & (~0x3);
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
   * <p>Dispose of this memory by calling {@link #freeMemory(ReturnValue)}.
   *
   * <p>The only constraint to the allocated memory is that it can be read an written to by {@link
   * sun.misc.Unsafe}.
   *
   * <p>Notice: An {@link OutOfMemoryError} is thrown if the allocation is refused by the system
   * because of a resource constraint.
   *
   * @param bytes The size (in bytes) of the block of memory to allocate
   * @return The pointer to the allocated memory with metadata object.
   */
  ReturnValue allocateMemory(long bytes);

  /**
   * Disposes of a block of static final memory obtained from {@link #allocateMemory}.
   *
   * @see #allocateMemory
   * @param value The address of the memory block to free with a metadata
   */
  void freeMemory(ReturnValue value);
}
