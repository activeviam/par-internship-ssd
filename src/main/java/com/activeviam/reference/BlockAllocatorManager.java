/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.reference;

import com.activeviam.MemoryAllocator;
import com.activeviam.UnsafeUtil;
import com.activeviam.platform.LinuxPlatform;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Does implement {@link IBlockAllocator} but it delegates all its call to a {@link IBlockAllocator}
 * within a list of {@link IBlockAllocator} it manages.
 *
 * @author ActiveViam
 */
public class BlockAllocatorManager implements IBlockAllocator {

  /** List of all underlying managed allocators */
  private final Deque<ABlockAllocator> blocks;

  /** Size of memory (in bytes) that will be allocated when calling {@link #allocate()}. */
  private final long size;

  /** The amount of virtual memory to reserve for a block */
  private final long virtualBlockSize;

  private volatile int ongoingSwapOfFullAllocator;

  private final long MIN_BALLOC_LIFE_DURATION = 0;

  /**
   * boolean to indicate huge pages (if supported) can be requested when allocating block of memory
   */
  private final boolean useHugePage;

  private final AMemoryAllocatorOnFile.IBlockAllocatorFactory allocatorFactory;

  /**
   * Default constructor.
   *
   * @param size of memory (in bytes) that will be allocated when calling {@link #allocate()}.
   * @param virtualBlockSize minimum size of memory to reserve for the entire block. Each subsequent
   *     call to {@link #allocate()} will take a portion of it.
   */
  public BlockAllocatorManager(
          final AMemoryAllocatorOnFile.IBlockAllocatorFactory factory, final long size, final long virtualBlockSize) {
    this.allocatorFactory = factory;
    this.size = size;
    this.virtualBlockSize = computeBlockSizeAsMultipleOfSize(this.size, virtualBlockSize);
    this.blocks = new ConcurrentLinkedDeque<>(); // Lazily add elements to the list
    this.useHugePage = canUseHugePage();
  }

  @Override
  public long size() {
    return this.size;
  }

  /**
   * Check whether the huge pages may be asked form the system.
   *
   * @return true if huge pages can be asked, false otherwise.
   */
  private boolean canUseHugePage() {
    final long[] pageSizes = LinuxPlatform.getInstance().getSupportedPageSizes();
    if (pageSizes != null && pageSizes.length > 1) {
      // more than 1 element. The first element is meant for regular page size.
      for (int i = 1; i < pageSizes.length; i++) {
        if (this.size % pageSizes[i] == 0) {
          return true;
        }
      }
    }
    return false;
  }

  /** @return a new {@link ABlockAllocator} that can be used immediately. */
  private ABlockAllocator createBlockAllocator() {
    return this.allocatorFactory.create(this.size, this.virtualBlockSize, this.useHugePage);
  }

  /**
   * Compute the amount of memory to reserved by this allocator. We must ensure it is a multiple of
   * size parameter (<code> blockSize = i * size </code>)
   *
   * @param size the size of each allocation
   * @param minBlockSize minimum size of memory to reserve for the entire block. Each subsequent
   *     call to {@link #allocate()} will take a portion of it.
   * @return the size of memory to reserve.
   */
  private long computeBlockSizeAsMultipleOfSize(final long size, final long minBlockSize) {
    if (
    /* Unlikely */ size >= minBlockSize) {
      // Only one allocation will be allowed in this case.
      return size;
    } else {
      final long modulo = minBlockSize % size;
      return (minBlockSize / size + (modulo == 0L ? 0L : 1L)) * size;
    }
  }

  @Override
  public MemoryAllocator.ReturnValue allocate() {

    MemoryAllocator.ReturnValue value = null;
    ABlockAllocator blockAllocator;

    do {

      if ((blockAllocator = this.blocks.peekFirst()) != null && (value = blockAllocator.allocate()) != null) {
        return value;
      }

      if (casOngoingSwapOfFullAllocator(this, 0, 1)) {
        try {

          try {
            blockAllocator = this.blocks.getFirst();
            if ((value = blockAllocator.allocate()) != null) {
              this.blocks.addFirst(blockAllocator);
              return value;
            }
            this.blocks.addLast(blockAllocator);
          } catch (NoSuchElementException e) {
          }

          if ((blockAllocator = this.blocks.peekFirst()) != null && (value = blockAllocator.allocate()) != null) {
            return value;
          }

          if ((blockAllocator = createBlockAllocator()) == null) {
            return null;
          }

          value = blockAllocator.allocate();
          this.blocks.addFirst(blockAllocator);

        } finally {
          this.ongoingSwapOfFullAllocator = 0;
        }
      }
    } while (value == null);

    return value;
  }

  @Override
  public void free(MemoryAllocator.ReturnValue value) {
    final var blockAllocator = value.getBlockAllocator();
    blockAllocator.free(value);
    if (blockAllocator.tryRelease()) {
      this.blocks.removeFirstOccurrence(blockAllocator);
    }
  }

  @Override
  public void release() {
    final var it = this.blocks.iterator();
    while (it.hasNext()) {
      final var blockAllocator = it.next();
      if (!blockAllocator.tryRelease()){
        blockAllocator.release();
      }
      it.remove();
    }
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder(BlockAllocatorManager.class.getSimpleName()
        + " [size="
        + this.size
        + ", blockSize="
        + this.virtualBlockSize
        + ", useHugePage="
        + this.useHugePage
        + "]");

    for (final var ba : this.blocks) {
      sb.append(ba.toString());
    }

    return sb.toString();
  }

  private static final long ongoingSwapOfFullAllocatorOffset =
          UnsafeUtil.getFieldOffset(BlockAllocatorManager.class, "ongoingSwapOfFullAllocator");

  private static boolean casOngoingSwapOfFullAllocator(
          final BlockAllocatorManager blockAllocatorManager, final int expect, final int update) {
    return UnsafeUtil.compareAndSwapInt(
            blockAllocatorManager, ongoingSwapOfFullAllocatorOffset, expect, update);
  }

  public void garbageCollect() {
    final var it = this.blocks.iterator();
    while (it.hasNext()) {
      final var blockAllocator = it.next();
      final long currentTime = System.currentTimeMillis();
      if (blockAllocator.getTimestamp() + MIN_BALLOC_LIFE_DURATION < currentTime) {
        if (!blockAllocator.tryRelease()) {
          blockAllocator.release();
        }
        it.remove();
      }
    }
  }

}
