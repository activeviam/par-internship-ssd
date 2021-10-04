package com.activeviam.reference;

import com.activeviam.IMemoryAllocator;
import com.activeviam.UnsafeUtil;
import com.activeviam.platform.LinuxPlatform;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** @author ActiveViam */
public abstract class AMemoryAllocatorOnFile implements IMemoryAllocator, Closeable {
  /** Class logger. */
  protected static final Logger logger = Logger.getLogger("allocator");

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

  protected final LinuxPlatform platform;

  public final Path dir;

  /** {@link IBlockAllocator Allocators} currently available (one per size of chunks). */
  protected volatile Map<Long, IBlockAllocator> allocators;

  /**
   * The default size of virtual memory to allocate/reserved for each new {@link IBlockAllocator}
   */
  protected final long virtualBlockSize;

  /** @param dir the directory where to allocate the memory mapped files */
  public AMemoryAllocatorOnFile(final Path dir) {
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

  /**
   * Lazily allocate the allocator.
   *
   * @param bytes the number of bytes to be allocated
   * @return the allocator to use
   */
  protected IBlockAllocator getOrCreateAllocator(final long bytes) {
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

  protected long getMappedSize(final long size) {
    final long pSize = PAGE_SIZE;
    if (pSize >= size) {
      return size;
    } else {
      final long modulo = size % pSize;
      return (size / pSize + (modulo == 0 ? 0 : 1)) * pSize;
    }
  }


  protected IBlockAllocator createAllocator(final long bytes, final long mappedSize) {
    // Log if not multiple of page size.
    if (bytes > PAGE_SIZE && (bytes % PAGE_SIZE) != 0) {
      logger.warning(
          "Trying to allocate a chunk of size "
              + bytes
              + " bytes (not a multiple "
              + "of "
              + PAGE_SIZE
              + " bytes (memory page)).");
    }

    return new BlockAllocatorManager(
        createBlockAllocatorFactory(), mappedSize, this.virtualBlockSize);
  }

  protected abstract IBlockAllocatorFactory createBlockAllocatorFactory();

  protected interface IBlockAllocatorFactory {

    /**
     * Creates a new {@link ABlockStackAllocator}.
     *
     * @param size Size of memory (in bytes) that will be allocated when calling {@link
     *     IBlockAllocator#allocate()}.
     * @param blockSize amount of virtual memory to reserve for an entire block
     * @param useHugePage true to indicate to the system that it should use huge pages (if it
     *     supports them).
     * @return a new {@link IBlockAllocator}
     */
    ABlockStackAllocator create(long size, long blockSize, boolean useHugePage);
  }
}
