/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.platform;

import com.sun.jna.Library;
import com.sun.jna.Platform;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.Unsafe;

public class LinuxPlatform {

  /** This class logger. */
  private static final Logger LOGGER = Logger.getLogger("platform");

  /** The native C library. */
  protected final CLibrary cLib;

  /**
   * preload library to back text, data, malloc() or shared memory with hugepages.
   *
   * <p><a
   * href="http://linux.die.net/man/7/libhugetlbfs">http://linux.die.net/man/7/libhugetlbfs</a>
   */
  protected final HugetlbfsLib hugetlbfsLib;

  /** The JNA {@link Library library} to expose the {@code libc} methods. */
  protected final PthreadLibrary pthreadLib;

  /**
   * The JNA {@link Library library} to expose the {@code libc} methods.
   *
   * @author ActiveViam
   */
  protected interface PthreadLibrary extends Library {

    /** The name of the library */
    String LIBRARY_NAME = "pthread";

    /**
     * returns the number of the CPU on which the calling thread is currently executing.
     *
     * @return On success, sched_getcpu() returns a nonnegative CPU number. On error, -1 is returned
     *     and errno is set to indicate the error.
     */
    int sched_getcpu();
  }

  /** Instance of this class, used for the singleton pattern */
  protected static LinuxPlatform INSTANCE;

  /** The kernel pretends there is always enough memory until it actually runs out. */
  protected static final int OVERCOMMIT_MEMORY_UNLIMITED = 1;

  /** Supported page sizes sorted in ascending order */
  protected final long[] pageSizes;

  /** Constructor. */
  @SuppressWarnings("restriction")
  protected LinuxPlatform() {
    if (!Platform.isLinux()) {
      throw new RuntimeException("Not running on a linux platform!!");
    }
    PthreadLibrary pthreadLib;
    CLibrary cLib;
    HugetlbfsLib hugetlbfsLib;

    try {
      pthreadLib = SaferNative.loadLibrary(PthreadLibrary.LIBRARY_NAME, PthreadLibrary.class);
      LOGGER.config("The Pthread library was successfully loaded.");
    } catch (UnsatisfiedLinkError e) {
      pthreadLib = null;
      LOGGER.log(Level.CONFIG, "We were unable to load the Pthread library.", e);
    }

    try {
      cLib = SaferNative.loadLibrary(CLibrary.LIBRARY_NAME, CLibrary.class);
      LOGGER.config("The C library was successfully loaded.");
    } catch (UnsatisfiedLinkError e) {
      cLib = null;
      LOGGER.log(Level.CONFIG, "We were unable to load the C library.", e);
    }

    try {
      hugetlbfsLib = SaferNative.loadLibrary("hugetlbfs", HugetlbfsLib.class);
      LOGGER.config("The hugetlbfs library was successfully loaded.");
    } catch (UnsatisfiedLinkError e) {
      hugetlbfsLib = null;
      LOGGER.log(Level.CONFIG, "We were unable to load the hugetlbfs library.", e);
    }

    this.pthreadLib = pthreadLib;
    this.cLib = cLib;
    this.hugetlbfsLib = hugetlbfsLib;

    if (hugetlbfsLib != null) {
      this.pageSizes = getPageSizes();
    } else {
      // Retrieve with unsafe, hugetlbfs lib not installed.
      this.pageSizes = new long[] {retrieveUnsafe().pageSize()};
    }
  }

  /** @return the singleton Linux Platform */
  public static final LinuxPlatform getInstance() {
    if (INSTANCE == null) {
      synchronized (LinuxPlatform.class) {
        if (INSTANCE == null) {
          INSTANCE = new LinuxPlatform();
        }
      }
    }
    return INSTANCE;
  }

  public int getProcessorCount() {
    return Runtime.getRuntime().availableProcessors();
  }

  public void munmap(long ptr, long size) {
    if (cLib == null) {
      throw new RuntimeException(
          "C Library could not be loaded on your system. Calls to munmap are not available.");
    }
    final int result = cLib.munmap(ptr, size);
    if (result != 0) {
      final int errno = SaferNative.getLastError();
      switch (errno) {
        case Errno.ENOMEM:
          throw new OutOfMemoryError(
              "Tried to unmap partially a mapping, and "
                  + "now the process's maximum number of mappings has exceeded. ("
                  + ptr
                  + ","
                  + size
                  + ")");
        default:
          Errno.throwLastError("munmap", ptr, size);
      }
    }
  }

  public String toString() {
    return getClass().getSimpleName()
        + " [pthread library found: "
        + (pthreadLib != null)
        + ", C library found: "
        + (cLib != null)
        + "]";
  }

  public void commit(long ptr, long size, boolean useHugePage) {
    // Do nothing on Linux platform
  }

  public long getAvailableVirtualMemory() {
    // Default is 128TB on Linux if overcommit_memory=1
    return 128 * (1L << 40); // FIXME try to get it from the system.
  }

  /**
   * Check the parameter sysctl <i>vm.overcommit_memory</i> is set to 1 and throws if the value is
   * not correctly set.
   *
   * <p>See <a href="https://www.kernel.org/doc/Documentation/sysctl/vm.txt">
   * https://www.kernel.org/doc/Documentation/sysctl/vm.txt</a>
   *
   * @throws Exception <i>vm.overcommit_memory</i> is not set to 1
   */
  public void ensureOverCommitMemoryParameter() throws Exception {
    if (checkOverCommitParameter() != OVERCOMMIT_MEMORY_UNLIMITED) {
      throw new Exception(
          "Please set the value of vm.overcommit_memory to 1 (run 'sysctl -w vm.overcommit_memory=1' as root).");
    }
  }

  /** The path to the file where we read the overcommit memory setting from. * */
  protected static final String OVERCOMMIT_FILE_PATH = "/proc/sys/vm/overcommit_memory";
  /** The advice to append to most error messages about overcommit memory. * */
  protected static final String OVERCOMMIT_MEMORY_ADVICE =
      "Please make sure that vm.overcommit_memory is set to 1.";

  /**
   * Check the parameter sysctl vm.overcommit_memory is set to 1.
   *
   * <p>See <a href="https://www.kernel.org/doc/Documentation/sysctl/vm.txt">
   * https://www.kernel.org/doc/Documentation/sysctl/vm.txt</a>
   *
   * @return the value of vm.overcommit_memory. It can be 0, 1 or 2.
   */
  protected int checkOverCommitParameter() {
    try {
      List<String> lines =
          Files.readAllLines(FileSystems.getDefault().getPath(OVERCOMMIT_FILE_PATH));
      if (lines.isEmpty()) {
        LOGGER.warning(OVERCOMMIT_FILE_PATH + " is empty. " + OVERCOMMIT_MEMORY_ADVICE);
        return -1;
      }
      int overcommitMemory = Integer.parseInt(lines.iterator().next());
      if (overcommitMemory != 1) {
        LOGGER.warning(
            "vm.overcommit_memory = "
                + overcommitMemory
                + ". It is strongly advised to set "
                + "this value to 1. See https://www.kernel.org/doc/Documentation/sysctl/vm.txt");
      }
      return overcommitMemory;
    } catch (Throwable t) {
      LOGGER.log(
          Level.WARNING,
          "Unable to read " + OVERCOMMIT_FILE_PATH + ". " + OVERCOMMIT_MEMORY_ADVICE,
          t);
    }
    return -1;
  }

  public int openFile(Path path) {
    return cLib.open(path.toFile().getAbsolutePath(), CLibrary.OPEN_O_RDWR);
  }

  @Deprecated
  public int createTmpFile(Path dir) {
    int fd =
        cLib.open(
            dir.toFile().getAbsolutePath(),
            CLibrary.OPEN_O_RDWR | CLibrary.OPEN_O_TMPFILE | CLibrary.OPEN_O_CLOEXEC);
    switch (fd) {
      case Errno.EOPNOTSUPP:
        {
          throw new RuntimeException(" Your filesystem containing path does not support O_TMPFILE");
        }
      default:
        {
          return fd;
        }
    }
  }

  public int openFile(String path) {
    return cLib.open(path, CLibrary.OPEN_O_RDWR);
  }

  public void closeFile(int fd) {
    final int result = cLib.close(fd);
    if (result != 0) {
      Errno.throwLastError("close", fd);
    }
  }

  public void fallocate(int fd, long offset, long length, boolean deallocate) {
    final int mode =
        deallocate
            ? CLibrary.FALLOCATE_FALLOC_FL_PUNCH_HOLE | CLibrary.FALLOCATE_FALLOC_FL_KEEP_SIZE
            : 0;
    final int result = cLib.fallocate(fd, mode, offset, length);
    if (result != 0) {
      Errno.throwLastError("fallocate", fd, mode, offset, length);
    }
  }

  public long mmapFile(int fd, long size, boolean useHugePage) {
    if (cLib == null) {
      throw new RuntimeException(
          "C Library could not be loaded on your system. Calls to mmap are not available.");
    }
    if (size < 0) {
      throw new IllegalArgumentException("Cannot allocate a negative size, was " + size);
    }
    // All Linux distro should support MAP_ANONYMOUS, so no need to create a mapping in /dev/zero.
    final long ptr =
        cLib.mmap(0, size, CLibrary.PROT_READ | CLibrary.PROT_WRITE, CLibrary.MAP_SHARED, fd, 0);
    if (ptr == CLibrary.MAP_FAILED) {
      final int errno = SaferNative.getLastError();
      switch (errno) {
        case Errno.EINVAL:
          throw new IllegalArgumentException("Invalid length: was " + size);
        case Errno.ENOMEM:
          throw new OutOfMemoryError(
              "No memory is available, or the process's maximum number of mappings has exceeded. Could not allocate "
                  + size);
        case Errno.EBADF:
          throw new RuntimeException("Your system does not support map anonymous. (" + size + ")");
        default:
          Errno.throwLastError(
              "mmap",
              0,
              size,
              CLibrary.PROT_READ | CLibrary.PROT_WRITE,
              CLibrary.MAP_PRIVATE | CLibrary.MAP_ANONYMOUS,
              -1,
              0);
      }
    }

    if (useHugePage) {
      cLib.madvise(ptr, size, CLibrary.MADV_HUGEPAGE);
    }
    return ptr;
  }

  /** @return array in bytes of available page sizes sorted in ascending order. */
  public long[] getSupportedPageSizes() {
    return pageSizes;
  }

  /** @return array in bytes of available page sizes sorted in ascending order. */
  @SuppressWarnings("restriction")
  protected long[] getPageSizes() {
    long[] pageSizes;
    /*
     * Try catch to properly handle deprecated hugetlbfs lib. Cf UKS-1246:
     * java.lang.UnsatisfiedLinkError: Error looking up function 'getpagesizes':
     * /usr/lib64/libhugetlbfs.so: undefined symbol: getpagesizes
     */
    try {
      int nelem = hugetlbfsLib.getpagesizes(null, 0); // see javadoc.
      pageSizes = new long[nelem];
      if (hugetlbfsLib.getpagesizes(pageSizes, nelem) == -1) {
        Errno.throwLastError("getpagesizes");
      }
      Arrays.sort(pageSizes);
    } catch (Throwable throwable) {
      LOGGER.log(
          Level.WARNING,
          "Could not retrieve the number of system supported page sizes. A higher "
              + "version than 2.1 of 'libhugetlbfs.so' is required.",
          throwable);
      // HANDLE potential error by fallbacking on default page size.
      pageSizes = new long[] {retrieveUnsafe().pageSize()};
    }

    return pageSizes;
  }

  /**
   * Returns a sun.misc.Unsafe. Suitable for use in a 3rd party package.
   *
   * @return a sun.misc.Unsafe
   */
  @SuppressWarnings({"restriction"})
  private static final sun.misc.Unsafe retrieveUnsafe() {
    try {
      return sun.misc.Unsafe.getUnsafe();
    } catch (SecurityException se) {
      try {
        return java.security.AccessController.doPrivileged(
            (PrivilegedExceptionAction<Unsafe>)
                () -> {
                  Field f = Unsafe.class.getDeclaredField("theUnsafe");
                  f.setAccessible(true);
                  return (Unsafe) f.get(null);
                });
      } catch (java.security.PrivilegedActionException e) {
        throw new RuntimeException("Could not initialize intrinsics", e.getCause());
      }
    }
  }
}
