/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.reference;

import com.activeviam.UnsafeUtil;
import com.activeviam.platform.LinuxPlatform;
import com.activeviam.reference.MemoryAllocatorOnFile.IBlockAllocatorFactory;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Does implement {@link IBlockAllocator} but it delegate all its call to a {@link IBlockAllocator}
 * within a list of {@link IBlockAllocator} it manages.
 *
 * @author ActiveViam
 */
public class BlockAllocatorManager implements IBlockAllocator {

  /** List of all underlying managed allocators */
  private final CopyOnWriteArrayList<ABlockAllocator> blocks;

  /** Size of memory (in bytes) that will be allocated when calling {@link #allocate()}. */
  private final long size;

  /** The amount of virtual memory to reserve for a block */
  private final long virtualBlockSize;

  /**
   * Protection to not allocate two blocks of memory if two calls on {@link #allocate()} are made
   * when all blocks are full. They are only two acceptable values for this attributes: If the value
   * is 1, a new {@link ABlockAllocator} is going to be allocate. 0 if not.
   */
  private volatile int ongoingCreationProcess;

  /**
   * boolean to indicate huge pages (if supported) can be requested when allocating block of memory
   */
  private final boolean useHugePage;

  private final IBlockAllocatorFactory allocatorFactory;

  /**
   * Default constructor.
   *
   * @param size of memory (in bytes) that will be allocated when calling {@link #allocate()}.
   * @param virtualBlockSize minimum size of memory to reserve for the entire block. Each subsequent
   *     call to {@link #allocate()} will take a portion of it.
   */
  public BlockAllocatorManager(
      final IBlockAllocatorFactory factory, final long size, final long virtualBlockSize) {
    this.allocatorFactory = factory;
    this.size = size;
    this.virtualBlockSize = computeBlockSizeAsMultipleOfSize(this.size, virtualBlockSize);
    this.blocks = new CopyOnWriteArrayList<>(); // Lazily add elements to the list
    this.useHugePage = canUseHugePage();
  }

  @Override
  public long size() {
    return this.size;
  }

  /**
   * Check whether or not huge pages can be asked to the system.
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
  public long allocate() {
    long ptr;
    do {
      if ((ptr = tryAllocate()) != NULL_POINTER) return ptr;

      if (casOngoingCreationProcess(this, 0, 1)) {
        // From that point, no block can be created by another thread.
        try {
          // Once the CAS succeeds, check no block have been
          // created since the end of the for loop
          if ((ptr = tryAllocate()) != NULL_POINTER) return ptr;

          // Need to extend the overall capacity i.e create a new block.
          final ABlockAllocator newBlock = createBlockAllocator();
          // IMPORTANT !! Make the allocation first before adding the new block
          // to the block list to make sure this allocation will succeed.
          ptr = newBlock.allocate();
          this.blocks.add(newBlock); // From that point, the new block is visible by other threads
        } finally {
          this.ongoingCreationProcess = 0; // restore the value
        }
      }
    } while (ptr == NULL_POINTER);

    return ptr;
  }

  /**
   * Try to allocate a piece of memory of size {@link #size}: iterate over all {@link
   * ABlockAllocator underlying allocators} until one of them succeeds to allocate.
   *
   * @return The pointer to this allocated memory. If the allocation failed it return a {@link
   *     IBlockAllocator#NULL_POINTER}.
   */
  private long tryAllocate() {
    long ptr;
    for (final var block : this.blocks) {
      if ((ptr = block.allocate()) != NULL_POINTER) return ptr;
    }
    return NULL_POINTER;
  }

  @Override
  public void free(final long address) {
    for (final var b : this.blocks) {
      // Need to find the block an address belongs to
      if (b.blockAddress <= address && address < b.blockAddress + b.blockSize) {
        b.free(address);
        if (b.tryRelease()) {
          // If tryRelease succeed, remove b from the block list
          this.blocks.remove(b);
        }
        return;
      }
    }
  }

  @Override
  public void release() {
    for (final var b : this.blocks) {
      b.release();
      this.blocks.remove(b);
    }
  }

  @Override
  public String toString() {
    return BlockAllocatorManager.class.getSimpleName()
        + " [size="
        + this.size
        + ", blockSize="
        + this.virtualBlockSize
        + ", useHugePage="
        + this.useHugePage
        + "]";
  }

  private static final long ongoingCreationProcessOffset =
      UnsafeUtil.getFieldOffset(BlockAllocatorManager.class, "ongoingCreationProcess");

  /**
   * Static wrappers for UNSAFE methods {@link UnsafeUtil#compareAndSwapInt(Object, long, int, int)}
   * with <code>creationCount = </code>{@link #ongoingCreationProcess}.
   *
   * <p>Hopefully it will be in-lined ...
   *
   * @return true if successful. false return indicates that the actual value was not equal to the
   *     expected value.
   */
  private static final boolean casOngoingCreationProcess(
      final BlockAllocatorManager blockAllocatorManager, final int expect, final int update) {
    return UnsafeUtil.compareAndSwapInt(
        blockAllocatorManager, ongoingCreationProcessOffset, expect, update);
  }
}
