/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.reference;

import com.activeviam.IMemoryAllocator;
import com.activeviam.UnsafeUtil;
import com.activeviam.platform.LinuxPlatform;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

/**
 * Does implement {@link IBlockAllocator} but it delegates all its call to a {@link IBlockAllocator}
 * within a list of {@link IBlockAllocator} it manages.
 *
 * @author ActiveViam
 */
public class BlockAllocatorManager implements IBlockAllocator {

  /** List of all underlying managed allocators */
  protected final Deque<ABlockStackAllocator> blocks;

  /** Size of memory (in bytes) that will be allocated when calling {@link #allocate()}. */
  protected final long size;

  /** The amount of virtual memory to reserve for a block */
  protected final long virtualBlockSize;

  protected volatile int ongoingCreation;

  protected final AMemoryAllocatorOnFile.IBlockAllocatorFactory allocatorFactory;

  /**
   * boolean to indicate huge pages (if supported) can be requested when allocating block of memory
   */
  protected final boolean useHugePage;

  /**
   * Default constructor.
   *
   * @param allocatorFactory created in {@link AMemoryAllocatorOnFile()} and constructing new block allocators
   * @param size of memory (in bytes) that will be allocated when calling {@link #allocate()}.
   * @param virtualBlockSize minimum size of memory to reserve for the entire block. Each subsequent
   *     call to {@link #allocate()} will take a portion of it.
   */
  public BlockAllocatorManager(final AMemoryAllocatorOnFile.IBlockAllocatorFactory allocatorFactory,
                               final long size, final long virtualBlockSize) {
    this.allocatorFactory = allocatorFactory;
    this.size = size;
    this.virtualBlockSize = computeBlockSizeAsMultipleOfSize(this.size, virtualBlockSize);
    this.blocks = new ConcurrentLinkedDeque<>(); // Lazily add elements to the list
    this.useHugePage = canUseHugePage();
    this.ongoingCreation = 0;
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

  /** @return a new {@link ABlockStackAllocator} that can be used immediately. */
  private ABlockStackAllocator createBlockAllocator() {
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
  public IMemoryAllocator.ReturnValue allocate() {

    IMemoryAllocator.ReturnValue value = null;
    ABlockStackAllocator blockAllocator;

    do {

      if ((blockAllocator = this.blocks.peekFirst()) != null &&
              (value = blockAllocator.allocate()) != null)
      {
        return value;
      }

      try {
        blockAllocator = this.blocks.getFirst();
        if ((value = blockAllocator.allocate()) != null) {
          this.blocks.addFirst(blockAllocator);
          return value;
        }
        this.blocks.addLast(blockAllocator);
      } catch (NoSuchElementException e) {
      }

      if ((blockAllocator = this.blocks.peekFirst()) != null &&
              (value = blockAllocator.allocate()) != null)
      {
        return value;
      }

      if (casOngoingCreation(this, 0, 1)) {
        try {
          if ((blockAllocator = createBlockAllocator()) == null) {
            return null;
          } else {
            value = blockAllocator.allocate();
            this.blocks.addFirst(blockAllocator);
            return value;
          }
        } finally {
          this.ongoingCreation = 0;
        }
      }

    } while (value == null);

    return value;
  }

  @Override
  public void free(IMemoryAllocator.ReturnValue value) {
    final var blockAllocator = (ABlockStackAllocator)(value.getMetadata());
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
        + "]\n");

    for (final var ba : this.blocks) {
      sb.append(ba.toString());
    }

    return sb.toString();
  }

  private static final long ongoingCreationOffset =
          UnsafeUtil.getFieldOffset(BlockAllocatorManager.class, "ongoingCreation");

  private static boolean casOngoingCreation(
          final BlockAllocatorManager blockAllocatorManager, final int expect, final int update) {
    return UnsafeUtil.compareAndSwapInt(
            blockAllocatorManager, ongoingCreationOffset, expect, update);
  }

  public void removeIf(Predicate<? super ABlockStackAllocator> predicate) {

      final var it = this.blocks.iterator();

      while (it.hasNext()) {
        final var blockAllocator = it.next();
        blockAllocator.release();
        it.remove();
      }

      //this.blocks.removeIf(predicate);
  }

  public void remove() {

    final var it = this.blocks.iterator();

    while (it.hasNext()) {
      final var blockAllocator = it.next();
      blockAllocator.release();
      it.remove();
    }

    //this.blocks.removeIf(predicate);
  }

}
