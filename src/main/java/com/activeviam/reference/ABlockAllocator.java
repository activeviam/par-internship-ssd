/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.reference;

import com.activeviam.UnsafeUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO
 *
 * @author ActiveViam
 */
public abstract class ABlockAllocator implements IBlockAllocator {

  /** Logger. */
  protected static final Logger LOGGER = Logger.getLogger("allocator");

  /** Size of memory (in bytes) that will be allocated when calling {@link #allocate()}. */
  protected final long size;

  /** minimum size of memory to reserve for an entire block */
  protected final long blockSize;

  /** The number of elements of size {@link #size} that can be stored in {@link #blockSize} */
  protected final int capacity;

  /** Address of the reserved block memory */
  protected long blockAddress = -2;

  /** The address that can be used to allocate an element within a block */
  protected volatile long lastAddress = -2;

  /**
   * A stack of available items that can be use to write/read a piece of memory within the allocated
   * block. An item refers to a position within the block. It means {@link #capacity} items exist
   * from 0 to <code>{@link #capacity} -1 </code>.
   *
   * <p>Notice this stack <b>DOES NOT CONTAIN DUPLICATES</b>.
   */
  protected final ConcurrentUniqueIntegerStack items;

  /**
   * Count to track the number of allocation being made. If this value is negative every subsequent
   * allocations must failed (it basically means the count has reached zero and the allocated block
   * of memory is going to be released via {@link #release()}).
   *
   * <p>Once it is negative, it should/must never become positive again.
   */
  protected volatile int count;

  /**
   * boolean to indicate huge pages (if supported) can be requested when allocating block of memory
   */
  protected final boolean useHugePage;

  /**
   * Default constructor.
   *
   * @param size Size of memory (in bytes) that will be allocated when calling {@link #allocate()}.
   * @param blockSize amount of virtual memory to reserve for an entire block
   * @param useHugePage true to indicate to the system that it should use huge pages (if it supports
   *     them).
   */
  public ABlockAllocator(final long size, final long blockSize, final boolean useHugePage) {
    this.size = size;
    this.blockSize = blockSize;
    this.useHugePage = useHugePage;
    this.capacity = (int) (blockSize / size);
    this.items = new ConcurrentUniqueIntegerStack(this.capacity);
    this.count = 0;
  }

  /** Must be called once before using {@link #allocate()}. */
  public void init() {
    final long blockPtr = virtualAlloc(this.blockSize);
    this.blockAddress = blockPtr;
    this.lastAddress = blockPtr;
  }

  @Override
  public long size() {
    return this.size;
  }

  /**
   * Get the address corresponding to the n-th piece of memory within the block. It is computed
   * using {@link #blockAddress} and {@link #size}.
   *
   * @param n the position of the piece of memory within the block.
   * @return the address
   */
  protected long getAddress(final int n) {
    return this.blockAddress + n * this.size;
  }

  /**
   * The reverse function of {@link #getAddress(int)}. From the address of a piece of memory of size
   * {@link #size} within the block, it gives it position.
   *
   * @param address the address of the piece of memory.
   * @return the position within the block.
   */
  protected int getPosition(final long address) {
    return (int) ((address - this.blockAddress) / this.size);
  }

  @Override
  public long allocate() {
    long ptr;

    // Otherwise, take a new one
    do {
      /*
       * Check to avoid unnecessary CASes by checking
       * first the pointer do not exceed the block
       * capacity.
       * The last pointer an allocation can reclaim is
       * equal to:
       *
       * ptr = blockAddress + blockSize - size
       */
      if ((ptr = this.lastAddress) >= this.blockAddress + this.blockSize) {
        return NULL_POINTER;
      }
    } while (!casLastAddress(this, ptr, ptr + this.size));

    // Increment the counter.
    int newC;
    do {
      newC = this.count;
      if (newC < 0) return NULL_POINTER; // abort allocation
    } while (!casCount(this, newC, newC + 1));

    doAllocate(ptr, this.size);

    return ptr;
  }

  @Override
  public void free(final long address) {
    final boolean cacheUsed;
    final ConcurrentUniqueIntegerStack stack; // the stack to use
    // Decommit before pushing the pointer in the stack to
    // prevent another thread to retrieve the pointer before
    // decommitting and start writing/ using this piece of mem.
    doFree(address, this.size);
    cacheUsed = false;
    stack = this.items;

    // Store address for later usage
    if (stack.push(getPosition(address))) {
      // If the push succeeds, decrements the counter
      // The stack used guarantees that concurrent calls on free()
      // with the same value does not decrement multiple times
      // the counter. Further more, when the counter value reaches 0,
      // the entire block can be released.
      int newC;
      do {
        newC = this.count;
      } while (!casCount(this, newC, newC - 1));

    } else {
      // Not suppose to happen...
      LOGGER.log(
          Level.WARNING,
          "Cleaning address twice for chunk of size " + PrintUtil.printDataSize(this.size) + ".");
    }
  }

  /**
   * @return true if the memory allocated by this block allocator has been freed, false otherwise.
   *     Once the memory has been freed, it is not possible to use {@link #allocate()} anymore.
   */
  public boolean tryRelease() {
    if (casCount(this, 0, -1)) { // set to -1 to turn it off to protect further allocation
      release();
      return true;
    }

    return false;
  }

  @Override
  public void release() {
    doRelease(this.blockAddress, this.blockSize);
  }

  /**
   * Reserves a range of virtual address space without allocating any actual physical storage in
   * memory or in the paging file on disk.
   *
   * @param size The size of the region to reserve, in bytes.
   * @return The starting address of the allocated region.
   */
  protected abstract long virtualAlloc(long size);

  /**
   * Method called by {@link #allocate()}.
   *
   * @param ptr A pointer to the base address of the region of pages to be allocated.
   * @param size size of the specific region in bytes.
   */
  protected abstract void doAllocate(long ptr, long size);

  /**
   * Method called by {@link #free(long)}.
   *
   * @param ptr A pointer to the base address of the region of pages to be disposed.
   * @param size size of the specific region in bytes.
   */
  protected abstract void doFree(long ptr, long size);

  /**
   * Method called by {@link #release()}.
   *
   * @param ptr pointer to the base address of the region of pages to be released.
   * @param size size of the region in bytes.
   */
  protected abstract void doRelease(long ptr, long size);

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName())
        .append(" [size=")
        .append(this.size)
        .append(", blockSize=")
        .append(this.blockSize)
        .append(", blockAddress=")
        .append(this.blockAddress)
        .append(", capacity=")
        .append(this.capacity)
        .append(", lastAddress=")
        .append(this.lastAddress)
        .append(", stack=")
        .append(this.items)
        .append(", allocationCounter=")
        .append(this.count)
        .append("]");
    return sb.toString();
  }

  protected static final long countOffset =
      UnsafeUtil.getFieldOffset(ABlockAllocator.class, "count");
  protected static final long lastAddressOffset =
      UnsafeUtil.getFieldOffset(ABlockAllocator.class, "lastAddress");

  /**
   * Static wrappers for UNSAFE methods {@link UnsafeUtil#compareAndSwapInt(Object, long, int, int)}
   * with <code>offset = </code>{@link #countOffset}.
   *
   * <p>Hopefully it will be in-lined ...
   *
   * @return true if successful. false return indicates that the actual value was not equal to the
   *     expected value.
   */
  protected static final boolean casCount(
      final ABlockAllocator blockAllocator, final int expected, final int updated) {
    return UnsafeUtil.compareAndSwapInt(blockAllocator, countOffset, expected, updated);
  }

  /**
   * Static wrappers for UNSAFE methods {@link UnsafeUtil#compareAndSwapLong(Object, long, long,
   * long)} with <code>offset = </code>{@link #countOffset}.
   *
   * <p>Hopefully it will be in-lined ...
   *
   * @return true if successful. false return indicates that the actual value was not equal to the
   *     expected value.
   */
  protected static final boolean casLastAddress(
      final ABlockAllocator blockAllocator, final long expected, final long updated) {
    return UnsafeUtil.compareAndSwapLong(blockAllocator, lastAddressOffset, expected, updated);
  }
}
