package com.activeviam.reference;

import com.activeviam.allocator.MemoryAllocator;
import com.activeviam.platform.LinuxPlatform;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/** @author ActiveViam */
public class MemoryAllocatorOnFile implements MemoryAllocator, Closeable {

  /** Class logger. */
  private static final Logger logger = Logger.getLogger("allocator");

  /**
   * It is the default value for <i>max_map_count</i> for Linux. We use is as a reference to size
   * our allocation pools.
   */
  protected static final int MAX_MAP_COUNT;

  /** Default value of {@link #RATIO}. */
  public static final double DEFAULT_NATIVE_MEMORY_CACHE_RATIO = 0.8d;

  /**
   * Percentage of -XX:MaxDirectMemorySize of memory that can be kept in cache. A value between 0.0
   * and 1.0 is expected.
   *
   * <p>Setting the ratio to 0 will deactivate the cache.
   */
  protected static final double RATIO;

  static {
    MAX_MAP_COUNT = 65536; // = 1 << 16
    RATIO = DEFAULT_NATIVE_MEMORY_CACHE_RATIO;
  }

  private final LinuxPlatform platform;

  private final Path dir;

  /** {@link IBlockAllocator Allocators} currently available (one per size of chunks). */
  protected volatile Map<Long, IBlockAllocator> allocators;

  /**
   * The default size of virtual memory to allocate/reserved for each new {@link IBlockAllocator}
   */
  protected final long virtualBlockSize;

  /** @param dir the directory where to allocate the memory mapped files */
  public MemoryAllocatorOnFile(final Path dir) {
    this.platform = LinuxPlatform.getInstance();
    this.dir = dir;
    this.dir.toFile().mkdirs();
    this.allocators = Collections.synchronizedMap(new HashMap<>());
    this.virtualBlockSize = computeMinimumBlockSize();
  }

  @Override
  public void close() {
    // TODO
  }

  /** @return the minimum size of block memory that should be allocated. */
  protected long computeMinimumBlockSize() {
    /*
     * Notice this value should be about 2GB:
     * (128TB = 2^47) / 2^16
     */
    return this.platform.getAvailableVirtualMemory() / MAX_MAP_COUNT;
  }

  @Override
  public long allocateMemory(final long bytes) {
    return getOrCreateAllocator(bytes).allocate();
  }

  @Override
  public void freeMemory(final long address, final long bytes) {
    getOrCreateAllocator(bytes).free(address);
  }

  /**
   * Lazily allocate the allocator.
   *
   * @param bytes the number of bytes to be allocated
   * @return the allocator to use
   */
  private IBlockAllocator getOrCreateAllocator(final long bytes) {
    final Long mappedSize = getMappedSize(bytes);
    var existingAllocator = this.allocators.get(mappedSize);
    if (existingAllocator != null) {
      return existingAllocator;
    }
    synchronized (this.allocators) {
      existingAllocator = this.allocators.get(mappedSize);
      if (existingAllocator != null) {
        return existingAllocator;
      }

      final var newAllocator = createAllocator(bytes, mappedSize);
      this.allocators.put(mappedSize, newAllocator);
      return newAllocator;
    }
  }

  /**
   * Compute the amount of memory that each chunk will need. The computation is based on the size of
   * the chunk (in bytes) and is round up to the first multiple of {@link UnsafeUtil#pageSize()}.
   *
   * <p>E.g: size = 6144, 8192 is returned (for pageSize = 4096 bytes).
   *
   * @param size size of a chunk
   * @return the real amount of memory that will be used.
   */
  protected long getMappedSize(final long size) {
    final long pSize = MemoryAllocator.PAGE_SIZE;
    if (pSize >= size) {
      return size;
    } else {
      final long modulo = size % pSize;
      return (size / pSize + (modulo == 0 ? 0 : 1)) * pSize;
    }
  }

  /**
   * @param bytes the number of bytes to be allocated
   * @param mappedSize associated to the new allocator. The size if expected to be a multiple of
   *     {@link MemoryAllocator#PAGE_SIZE}. Computed by {@link #getMappedSize(long)}.
   * @return the new {@link IBlockAllocator allocator}
   */
  protected IBlockAllocator createAllocator(final long bytes, final long mappedSize) {
    // Log if not multiple of page size.
    if (bytes > MemoryAllocator.PAGE_SIZE && (bytes % MemoryAllocator.PAGE_SIZE) != 0) {
      logger.warning(
          "Trying to allocate a chunk of size "
              + bytes
              + " bytes (not a multiple "
              + "of "
              + MemoryAllocator.PAGE_SIZE
              + " bytes (memory page)).");
    }

    return new BlockAllocatorManager(
        createBlockAllocatorFactory(), mappedSize, this.virtualBlockSize);
  }

  private IBlockAllocatorFactory createBlockAllocatorFactory() {
    // Ignore the nodeId as the threads that allocate the memory are already bound to this node
    return (size, blockSize, useHugePage) -> {
      final var ba = new BlockAllocatorOnFile(this.dir, size, blockSize, useHugePage);
      ba.init();
      return ba;
    };
  }

  protected interface IBlockAllocatorFactory {

    /**
     * Creates a new {@link ABlockAllocator}.
     *
     * @param size Size of memory (in bytes) that will be allocated when calling {@link
     *     IBlockAllocator#allocate()}.
     * @param blockSize amount of virtual memory to reserve for an entire block
     * @param useHugePage true to indicate to the system that it should use huge pages (if it
     *     supports them).
     * @return a new {@link IBlockAllocator}
     */
    ABlockAllocator create(long size, long blockSize, boolean useHugePage);
  }
}
