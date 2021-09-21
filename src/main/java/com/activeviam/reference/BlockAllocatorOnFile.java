package com.activeviam.reference;

import com.activeviam.MemoryAllocator;
import com.activeviam.platform.LinuxPlatform;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

class BlockAllocatorOnFile extends ABlockAllocator {

  protected static final LongSupplier ID_GENERATOR = new AtomicLong()::getAndIncrement;

  protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

  /** File descriptor */
  protected final int fd;

  protected final Path path;

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
  public BlockAllocatorOnFile(Path dir, long size, long blockSize, boolean useHugePage) {
    super(size, blockSize);
    this.useHugePage = useHugePage;

    if ((size % MemoryAllocator.PAGE_SIZE) != 0) {
      throw new IllegalArgumentException(size + " " + MemoryAllocator.PAGE_SIZE);
    }
    this.path = dir.resolve("hugefile_" + size + "_" + ID_GENERATOR.getAsLong());
    try {
      File file = this.path.toFile();
      // Just in case the file already exists
      file.delete();

      checkCanWriteFile();
      setFileLength(blockSize);

      file = path.toFile();
      file.deleteOnExit();
      this.fd = PLATFORM.openFile(file.getAbsolutePath());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void checkCanWriteFile() throws IOException {
    try (FileChannel channel =
        FileChannel.open(
            path,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
            StandardOpenOption.SPARSE)) {}
  }

  private void setFileLength(long blockSize) throws IOException {
    try (RandomAccessFile f = new RandomAccessFile(path.toFile(), "rw")) {
      f.setLength(blockSize);
    }
  }

  @Override
  protected long virtualAlloc(long size) {
    return PLATFORM.mmapFile(fd, size, useHugePage);
  }

  @Override
  protected void doAllocate(long ptr, long size) {
    PLATFORM.commit(ptr, size, useHugePage);
  }

  @Override
  protected void doFree(long ptr, long size) {
    PLATFORM.fallocate(this.fd, ptr - this.blockAddress, size, true);
  }

  @Override
  protected void doRelease(long ptr, long size) {
    PLATFORM.munmap(blockAddress, size);
    PLATFORM.closeFile(fd);
    path.toFile().delete();
  }
}
