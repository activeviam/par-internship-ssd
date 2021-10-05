/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.reference;

import com.activeviam.IMemoryAllocator;
import com.activeviam.UnsafeUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO
 *
 * @author ActiveViam
 */
public abstract class ABlockStackAllocator implements IBlockAllocator {

  /** Logger. */
  protected static final Logger LOGGER = Logger.getLogger("allocator");

  /** Size of memory (in bytes) that will be allocated when calling {@link #allocate()}. */
  protected final long size;

  protected final long blockSize;

  protected long blockAddress;

  /** The number of elements of size {@link #size} that can be stored in {@link #blockSize} */
  protected final int capacity;

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
   * Default constructor.
   *
   * @param size Size of memory (in bytes) that will be allocated when calling {@link #allocate()}.
   * @param blockSize amount of virtual memory to reserve for an entire block
   */
  public ABlockStackAllocator(final long size, final long blockSize) {
    this.size = size;
    this.blockSize = blockSize;
    this.capacity = (int) (blockSize / size);
    this.items = new ConcurrentUniqueIntegerStack(this.capacity);
    this.count = 0;
  }

  /** Must be called once before using {@link #allocate()}. */
  public boolean init() {
    final long blockPtr = virtualAlloc(this.blockSize);
    if (blockPtr == NULL_POINTER) {
      return false;
    }
    this.blockAddress = blockPtr;
    this.lastAddress = blockPtr;
    return true;
  }

  @Override
  public long size() {
    return this.size;
  }

  public int count() { return this.count; }

  public int capacity() { return this.capacity; }

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

  public boolean contains(long ptr) {
    return this.items.contains(getPosition(ptr));
  }

  @Override
  public IMemoryAllocator.ReturnValue allocate() {
    long ptr;
    boolean unused = false;

    final int popped = items.pop();

    if (popped != ConcurrentUniqueIntegerStack.NULL) {
      // Take an address from the ones already used but freed
      ptr = getAddress(popped);

    } else {
      // Otherwise, take a new one
      do {
        /*
         * Check to avoid unnecessary CASes by checking first the pointer do not exceed
         * the block capacity.
         * The last pointer an allocation can reclaim is equal to:
         * ptr = blockAddress + blockSize - size;
         */
        if ((ptr = lastAddress) > this.blockAddress + this.blockSize - this.size) {
          return null;
        }

      } while (!casLastAddress(this, ptr, ptr + size));

      unused = true;
    }

    // Increment the counter.
    int newC;
    do {
      newC = this.count;
      if (newC < 0) return null; // abort allocation
    } while (!casCount(this, newC, newC + 1));

    if (unused) {
      doAllocate(ptr, size);
    }

    return new IMemoryAllocator.ReturnValue(this, ptr);
  }

  @Override
  public void free(IMemoryAllocator.ReturnValue value) {
    // Decommit before pushing the pointer in the stack to
    // prevent another thread to retrieve the pointer before
    // decommitting and start writing/ using this piece of mem.

    final long ptr = value.getBlockAddress();
    doFree(ptr, this.size);

    final int pos = getPosition(ptr);

    // Store address for later usage
    if (this.items.push(pos)) {
      // If the push succeeds, decrements the counter
      // The stack used guarantees that concurrent calls on free()
      // with the same value does not decrement multiple times
      // the counter. Further more, when the counter value reaches 0,
      // the entire block can be released.

      int newC;
      do {
        newC = this.count;
        if (newC <= 0) {
          return;
        }
      } while (!casCount(this, newC, newC - 1));

    } else {
      // Not supposed to happen...

      /*
       * pause everything
       * acquire all locks
       * print chunks in all blocks
       * throw and exit
       */

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
      doRelease(this.blockAddress, this.blockSize);
      return true;
    }
    return false;
  }

  public void release() {
    int newC;
    do {
      newC = this.count;
      if (newC < 0) {
        return;
      }
    } while (!casCount(this, newC, -1));
    doRelease(this.blockAddress, this.blockSize);
  }

  protected abstract long virtualAlloc(long size);

  protected abstract void doAllocate(long ptr, long size);

  protected abstract void doFree(long ptr, long size);

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
      UnsafeUtil.getFieldOffset(ABlockStackAllocator.class, "count");
  protected static final long lastAddressOffset =
      UnsafeUtil.getFieldOffset(ABlockStackAllocator.class, "lastAddress");

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
      final ABlockStackAllocator blockAllocator, final int expected, final int updated) {
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
      final ABlockStackAllocator blockAllocator, final long expected, final long updated) {
    return UnsafeUtil.compareAndSwapLong(blockAllocator, lastAddressOffset, expected, updated);
  }
}
