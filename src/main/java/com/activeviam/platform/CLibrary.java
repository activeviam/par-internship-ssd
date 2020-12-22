/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.platform;

import com.sun.jna.Library;

/**
 * C Library constants, retrieved from mman.h files for libc.so.6
 *
 * @author ActiveViam
 */
// Package private
interface CLibrary extends Library {
  static final String LIBRARY_NAME = "c";

  ///////////////////////////////////////////////
  // From /usr/include/sys/mman.h
  ///////////////////////////////////////////////
  static final int MAP_FAILED = -1;

  ///////////////////////////////////////////////
  // From /usr/include/asm-generic/mman.h
  ///////////////////////////////////////////////

  static final int MAP_GROWSDOWN = 0x0100; /* stack-like segment */
  static final int MAP_DENYWRITE = 0x0800; /* ETXTBSY */
  static final int MAP_EXECUTABLE = 0x1000; /* mark it as an executable */
  static final int MAP_LOCKED = 0x2000; /* pages are locked */
  static final int MAP_NORESERVE = 0x4000; /* don't check for reservations */
  static final int MAP_POPULATE = 0x8000; /* populate (prefault) pagetables */
  static final int MAP_NONBLOCK = 0x10000; /* do not block on IO */
  static final int MAP_STACK =
      0x20000; /* give out an address that is best suited for process/thread stacks */
  static final int MAP_HUGETLB = 0x40000; /* create a huge page mapping */
  /* Bits [26:31] are reserved, see mman-common.h for MAP_HUGETLB usage */

  ///////////////////////////////////////////////
  // From /usr/include/asm-generic/mman-common.h
  ///////////////////////////////////////////////
  static final int PROT_READ = 0x1; /* page can be read */
  static final int PROT_WRITE = 0x2; /* page can be written */
  static final int PROT_EXEC = 0x4; /* page can be executed */
  static final int PROT_SEM = 0x8; /* page may be used for atomic ops */
  static final int PROT_NONE = 0x0; /* page can not be accessed */
  static final int PROT_GROWSDOWN =
      0x01000000; /* mprotect flag: extend change to start of growsdown vma */
  static final int PROT_GROWSUP =
      0x02000000; /* mprotect flag: extend change to end of growsup vma */

  static final int MAP_SHARED = 0x01; /* Share changes */
  static final int MAP_PRIVATE = 0x02; /* Changes are private */
  static final int MAP_TYPE = 0x0f; /* Mask for type of mapping */
  static final int MAP_FIXED = 0x10; /* Interpret addr exactly */
  static final int MAP_ANONYMOUS = 0x20; /* don't use a file */
  static final int MAP_UNINITIALIZED =
      0x4000000; /* For anonymous mmap, memory could be uninitialized */
  /* compatibility flags */
  static final int MAP_FILE = 0;
  /*
   * When MAP_HUGETLB is set bits [26:31] encode the log2 of the huge page size.
   * This gives us 6 bits, which is enough until someone invents 128 bit address
   * spaces.
   * Assume these are all power of twos.
   * When 0 use the default page size.
   */
  static final int MAP_HUGE_SHIFT = 26;
  static final int MAP_HUGE_MASK = 0x3f;

  ///////////////////////////////////////////////
  // From /usr/include/asm/mman.h
  ///////////////////////////////////////////////
  static final int MAP_32BIT = 0x40; /* only give out 32bit addresses */
  static final int MAP_HUGE_2MB = (21 << MAP_HUGE_SHIFT);
  static final int MAP_HUGE_1GB = (30 << MAP_HUGE_SHIFT);

  ///////////////////////////////////////////////
  // From /usr/include/asm-generic/mman-common.h
  ///////////////////////////////////////////////

  static final int MADV_NORMAL = 0; /* no further special treatment */
  static final int MADV_RANDOM = 1; /* expect random page references */
  static final int MADV_SEQUENTIAL = 2; /* expect sequential page references */
  static final int MADV_WILLNEED = 3; /* will need these pages */
  static final int MADV_DONTNEED = 4; /* don't need these pages */

  static final int MADV_REMOVE = 9; /* remove these pages & resources */
  static final int MADV_DONTFORK = 10; /* don't inherit across fork */
  static final int MADV_DOFORK = 11; /* do inherit across fork */
  static final int MADV_HWPOISON = 100; /* poison a page for testing */
  static final int MADV_SOFT_OFFLINE = 101; /* soft offline page for testing */

  static final int MADV_MERGEABLE = 12; /* KSM may merge identical pages */
  static final int MADV_UNMERGEABLE = 13; /* KSM may not merge identical pages */

  static final int MADV_HUGEPAGE = 14; /* Worth backing with hugepages */
  static final int MADV_NOHUGEPAGE = 15; /* Not worth backing with hugepages */

  /*
   * Explicity exclude from the core dump,
   * overrides the coredump filter bits
   */
  static final int MADV_DONTDUMP = 16;
  static final int MADV_DODUMP = 17; /* Clear the MADV_NODUMP flag */

  /**
   * mmap() creates a new mapping in the virtual address space of the calling process. The starting
   * address for the new mapping is specified in addr. The length argument specifies the length of
   * the mapping.
   *
   * <p>If addr is NULL, then the kernel chooses the address at which to create the mapping; this is
   * the most portable method of creating a new mapping. If addr is not NULL, then the kernel takes
   * it as a hint about where to place the mapping; on Linux, the mapping will be created at a
   * nearby page boundary. The address of the new mapping is returned as the result of the call.
   *
   * <p>The contents of a file mapping (as opposed to an anonymous mapping; see MAP_ANONYMOUS
   * below), are initialized using length bytes starting at offset offset in the file (or other
   * object) referred to by the file descriptor fd. offset must be a multiple of the page size as
   * returned by sysconf(_SC_PAGE_SIZE).
   *
   * <p>The prot argument describes the desired memory protection of the mapping (and must not
   * conflict with the open mode of the file). It is either PROT_NONE or the bitwise OR of one or
   * more of the following flags:
   *
   * <ul>
   *   <li>PROT_EXEC Pages may be executed.
   *   <li>PROT_READ Pages may be read.
   *   <li>PROT_WRITE Pages may be written.
   *   <li>PROT_NONE Pages may not be accessed.
   * </ul>
   *
   * The flags argument determines whether updates to the mapping are visible to other processes
   * mapping the same region, and whether updates are carried through to the underlying file. This
   * behavior is determined by including exactly one of the following values in flags:
   *
   * <ul>
   *   <li>MAP_SHARED Share this mapping. Updates to the mapping are visible to other processes that
   *       map this file, and are carried through to the underlying file. (To precisely control when
   *       updates are carried through to the underlying file requires the use of msync(2).)
   *   <li>MAP_PRIVATE Create a private copy-on-write mapping. Updates to the mapping are not
   *       visible to other processes mapping the same file, and are not carried through to the
   *       underlying file. It is unspecified whether changes made to the file after the mmap() call
   *       are visible in the mapped region.
   * </ul>
   *
   * Both of these flags are described in POSIX.1-2001 and POSIX.1-2008.
   *
   * <p>In addition, zero or more of the following values can be ORed in flags:
   *
   * <ul>
   *   <li>MAP_32BIT (since Linux 2.4.20, 2.6) Put the mapping into the first 2 Gigabytes of the
   *       process address space. This flag is supported only on x86-64, for 64-bit programs. It was
   *       added to allow thread stacks to be allocated somewhere in the first 2GB of memory, so as
   *       to improve context-switch performance on some early 64-bit processors. Modern x86-64
   *       processors no longer have this performance problem, so use of this flag is not required
   *       on those systems. The MAP_32BIT flag is ignored when MAP_FIXED is set.
   *   <li>MAP_ANON Synonym for MAP_ANONYMOUS. Deprecated.
   *   <li>MAP_ANONYMOUS The mapping is not backed by any file; its contents are initialized to
   *       zero. The fd and offset arguments are ignored; however, some implementations require fd
   *       to be -1 if MAP_ANONYMOUS (or MAP_ANON) is specified, and portable applications should
   *       ensure this. The use of MAP_ANONYMOUS in conjunction with MAP_SHARED is supported on
   *       Linux only since kernel 2.4.
   *   <li>MAP_DENYWRITE This flag is ignored. (Long ago, it signaled that attempts to write to the
   *       underlying file should fail with ETXTBUSY. But this was a source of denial-of-service
   *       attacks.)
   *   <li>MAP_EXECUTABLE This flag is ignored.
   *   <li>MAP_FILE Compatibility flag. Ignored.
   *   <li>MAP_FIXED Don't interpret addr as a hint: place the mapping at exactly that address. addr
   *       must be a multiple of the page size. If the memory region specified by addr and len
   *       overlaps pages of any existing mapping(s), then the overlapped part of the existing
   *       mapping(s) will be discarded. If the specified address cannot be used, mmap() will fail.
   *       Because requiring a fixed address for a mapping is less portable, the use of this option
   *       is discouraged.
   *   <li>MAP_GROWSDOWN Used for stacks. Indicates to the kernel virtual memory system that the
   *       mapping should extend downward in memory.
   *   <li>MAP_HUGETLB (since Linux 2.6.32) Allocate the mapping using "huge pages." See the Linux
   *       kernel source file Documentation/vm/hugetlbpage.txt for further information, as well as
   *       NOTES, below.
   *   <li>MAP_HUGE_2MB, MAP_HUGE_1GB (since Linux 3.8) Used in conjunction with MAP_HUGETLB to
   *       select alternative hugetlb page sizes (respectively, 2 MB and 1 GB) on systems that
   *       support multiple hugetlb page sizes.
   *       <p>More generally, the desired huge page size can be configured by encoding the base-2
   *       logarithm of the desired page size in the six bits at the offset MAP_HUGE_SHIFT. (A value
   *       of zero in this bit field provides the default huge page size; the default huge page size
   *       can be discovered vie the Hugepagesize field exposed by /proc/meminfo.) Thus, the above
   *       two constants are defined as:
   *       <p>#define MAP_HUGE_2MB (21 << MAP_HUGE_SHIFT) #define MAP_HUGE_1GB (30 <<
   *       MAP_HUGE_SHIFT)
   *       <p>The range of huge page sizes that are supported by the system can be discovered by
   *       listing the subdirectories in /sys/kernel/mm/hugepages.
   *   <li>MAP_LOCKED (since Linux 2.5.37) Mark the mmaped region to be locked in the same way as
   *       mlock(2). This implementation will try to populate (prefault) the whole range but the
   *       mmap call doesn't fail with ENOMEM if this fails. Therefore major faults might happen
   *       later on. So the semantic is not as strong as mlock(2). One should use mmap(2) plus
   *       mlock(2) when major faults are not acceptable after the initialization of the mapping.
   *       The MAP_LOCKED flag is ignored in older kernels.
   *   <li>MAP_NONBLOCK (since Linux 2.5.46) Only meaningful in conjunction with MAP_POPULATE. Don't
   *       perform read-ahead: create page tables entries only for pages that are already present in
   *       RAM. Since Linux 2.6.23, this flag causes MAP_POPULATE to do nothing. One day, the
   *       combination of MAP_POPULATE and MAP_NONBLOCK may be reimplemented.
   *   <li>MAP_NORESERVE Do not reserve swap space for this mapping. When swap space is reserved,
   *       one has the guarantee that it is possible to modify the mapping. When swap space is not
   *       reserved one might get SIGSEGV upon a write if no physical memory is available. See also
   *       the discussion of the file /proc/sys/vm/overcommit_memory in proc(5). In kernels before
   *       2.6, this flag had effect only for private writable mappings.
   *   <li>MAP_POPULATE (since Linux 2.5.46) Populate (prefault) page tables for a mapping. For a
   *       file mapping, this causes read-ahead on the file. This will help to reduce blocking on
   *       page faults later. MAP_POPULATE is supported for private mappings only since Linux
   *       2.6.23.
   *   <li>MAP_STACK (since Linux 2.6.27) Allocate the mapping at an address suitable for a process
   *       or thread stack. This flag is currently a no-op, but is used in the glibc threading
   *       implementation so that if some architectures require special treatment for stack
   *       allocations, support can later be transparently implemented for glibc.
   *   <li>MAP_UNINITIALIZED (since Linux 2.6.33) Don't clear anonymous pages. This flag is intended
   *       to improve performance on embedded devices. This flag is honored only if the kernel was
   *       configured with the CONFIG_MMAP_ALLOW_UNINITIALIZED option. Because of the security
   *       implications, that option is normally enabled only on embedded devices (i.e., devices
   *       where one has complete control of the contents of user memory).
   * </ul>
   *
   * Of the above flags, only MAP_FIXED is specified in POSIX.1-2001 and POSIX.1-2008. However, most
   * systems also support MAP_ANONYMOUS (or its synonym MAP_ANON).
   *
   * <p>Some systems document the additional flags MAP_AUTOGROW, MAP_AUTORESRV, MAP_COPY, and
   * MAP_LOCAL.
   *
   * <p>Memory mapped by mmap() is preserved across fork(2), with the same attributes.
   *
   * <p>A file is mapped in multiples of the page size. For a file that is not a multiple of the
   * page size, the remaining memory is zeroed when mapped, and writes to that region are not
   * written out to the file. The effect of changing the size of the underlying file of a mapping on
   * the pages that correspond to added or removed regions of the file is unspecified.
   *
   * <p>On success, mmap() returns a pointer to the mapped area. On error, the value MAP_FAILED
   * (that is, -1) is returned, and errno is set to indicate the cause of the error.
   *
   * @param addr The starting address of the new mapping.
   * @param length The length of the mapping.
   * @param prot The protection (PROT_*) flags.
   * @param flags The MAP flags
   * @param fd The file descriptor
   * @param offset The offset in the file at which to start the mapping (multiple of page size).
   * @return a pointer to the mapped area, or -1 in case of failure.
   */
  // void *mmap(void *addr, size_t length, int prot, int flags, int fd, off_t offset);
  long mmap(long addr, long length, int prot, int flags, int fd, long offset);

  /**
   * The munmap() system call deletes the mappings for the specified address range, and causes
   * further references to addresses within the range to generate invalid memory references. The
   * region is also automatically unmapped when the process is terminated. On the other hand,
   * closing the file descriptor does not unmap the region.
   *
   * <p>The address addr must be a multiple of the page size (but length need not be). All pages
   * containing a part of the indicated range are unmapped, and subsequent references to these pages
   * will generate SIGSEGV. It is not an error if the indicated range does not contain any mapped
   * pages.
   *
   * @param addr The address of the mapping to free.
   * @param length The length of the mapping to free.
   * @return 0 on success, -1 on failure.
   */
  // int munmap(void *addr, size_t length);
  int munmap(long addr, long length);

  /**
   * The madvise() system call advises the kernel about how to handle paging input/output in the
   * address range beginning at address addr and with size length bytes. It allows an application to
   * tell the kernel how it expects to use some mapped or shared memory areas, so that the kernel
   * can choose appropriate read-ahead and caching techniques. This call does not influence the
   * semantics of the application (except in the case of MADV_DONTNEED), but may influence its
   * performance. The kernel is free to ignore the advice. The advice is indicated in the advice
   * argument which can be:
   *
   * <ul>
   *   <li>MADV_NORMAL No special treatment. This is the default.
   *   <li>MADV_RANDOM Expect page references in random order. (Hence, read ahead may be less useful
   *       than normally.)
   *   <li>MADV_SEQUENTIAL Expect page references in sequential order. (Hence, pages in the given
   *       range can be aggressively read ahead, and may be freed soon after they are accessed.)
   *   <li>MADV_WILLNEED Expect access in the near future. (Hence, it might be a good idea to read
   *       some pages ahead.)
   *   <li>MADV_DONTNEED Do not expect access in the near future. (For the time being, the
   *       application is finished with the given range, so the kernel can free resources associated
   *       with it.) Subsequent accesses of pages in this range will succeed, but will result either
   *       in reloading of the memory contents from the underlying mapped file (see mmap(2)) or
   *       zero-fill-on-demand pages for mappings without an underlying file.
   *   <li>MADV_REMOVE (Since Linux 2.6.16) Free up a given range of pages and its associated
   *       backing store. Currently, only shmfs/tmpfs supports this; other file systems return with
   *       the error ENOSYS.
   *   <li>MADV_DONTFORK (Since Linux 2.6.16) Do not make the pages in this range available to the
   *       child after a fork(2). This is useful to prevent copy-on-write semantics from changing
   *       the physical location of a page(s) if the parent writes to it after a fork(2). (Such page
   *       relocations cause problems for hardware that DMAs into the page(s).)
   *   <li>MADV_DOFORK (Since Linux 2.6.16) Undo the effect of MADV_DONTFORK, restoring the default
   *       behavior, whereby a mapping is inherited across fork(2).
   *   <li>MADV_HWPOISON (Since Linux 2.6.32) Poison a page and handle it like a hardware memory
   *       corruption. This operation is only available for privileged (CAP_SYS_ADMIN) processes.
   *       This operation may result in the calling process receiving a SIGBUS and the page being
   *       unmapped. This feature is intended for testing of memory error-handling code; it is only
   *       available if the kernel was configured with CONFIG_MEMORY_FAILURE.
   *   <li>MADV_SOFT_OFFLINE (Since Linux 2.6.33) Soft offline the pages in the range specified by
   *       addr and length. The memory of each page in the specified range is preserved (i.e., when
   *       next accessed, the same content will be visible, but in a new physical page frame), and
   *       the original page is offlined (i.e., no longer used, and taken out of normal memory
   *       management). The effect of the MADV_SOFT_OFFLINE operation is invisible to (i.e., does
   *       not change the semantics of) the calling process. This feature is intended for testing of
   *       memory error-handling code; it is only available if the kernel was configured with
   *       CONFIG_MEMORY_FAILURE.
   *   <li>MADV_MERGEABLE (since Linux 2.6.32) Enable Kernel Samepage Merging (KSM) for the pages in
   *       the range specified by addr and length. The kernel regularly scans those areas of user
   *       memory that have been marked as mergeable, looking for pages with identical content.
   *       These are replaced by a single write-protected page (which is automatically copied if a
   *       process later wants to update the content of the page). KSM only merges private anonymous
   *       pages (see mmap(2)). The KSM feature is intended for applications that generate many
   *       instances of the same data (e.g., virtualization systems such as KVM). It can consume a
   *       lot of processing power; use with care. See the Linux kernel source file
   *       Documentation/vm/ksm.txt for more details. The MADV_MERGEABLE and MADV_UNMERGEABLE
   *       operations are only available if the kernel was configured with CONFIG_KSM.
   *   <li>MADV_UNMERGEABLE (since Linux 2.6.32) Undo the effect of an earlier MADV_MERGEABLE
   *       operation on the specified address range; KSM unmerges whatever pages it had merged in
   *       the address range specified by addr and length.
   *   <li>MADV_HUGEPAGE (since Linux 2.6.38) Enables Transparent Huge Pages (THP) for pages in the
   *       range specified by addr and length. Currently, Transparent Huge Pages only work with
   *       private anonymous pages (see mmap(2)). The kernel will regularly scan the areas marked as
   *       huge page candidates to replace them with huge pages. The kernel will also allocate huge
   *       pages directly when the region is naturally aligned to the huge page size (see
   *       posix_memalign(2)). This feature is primarily aimed at applications that use large
   *       mappings of data and access large regions of that memory at a time (e.g., virtualization
   *       systems such as QEMU). It can very easily waste memory (e.g., a 2MB mapping that only
   *       ever accesses 1 byte will result in 2MB of wired memory instead of one 4KB page). See the
   *       Linux kernel source file Documentation/vm/transhuge.txt for more details. The
   *       MADV_HUGEPAGE and MADV_NOHUGEPAGE operations are only available if the kernel was
   *       configured with CONFIG_TRANSPARENT_HUGEPAGE.
   *   <li>MADV_NOHUGEPAGE (since Linux 2.6.38) Ensures that memory in the address range specified
   *       by addr and length will not be collapsed into huge pages.
   *   <li>MADV_DONTDUMP (since Linux 3.4) Exclude from a core dump those pages in the range
   *       specified by addr and length. This is useful in applications that have large areas of
   *       memory that are known not to be useful in a core dump. The effect of MADV_DONTDUMP takes
   *       precedence over the bit mask that is set via the /proc/PID/coredump_filter file (see
   *       core(5)).
   *   <li>MADV_DODUMP (since Linux 3.4) Undo the effect of an earlier MADV_DONTDUMP.
   * </ul>
   *
   * <p>On success madvise() returns zero. On error, it returns -1 and errno is set appropriately.
   *
   * @param addr The address of the mapping to madvise.
   * @param length The length of the mapping to madvise.
   * @param advice the advice which can be one of the listed above.
   * @return 0 on success, -1 on failure.
   */
  // int madvise (void *__addr, size_t __len, int __advice);
  int madvise(long addr, long length, int advice);

  int getpagesizes(long pagesize[], int nelem);

  // From https://github.com/torvalds/linux/blob/master/include/uapi/asm-generic/fcntl.h
  static final int OPEN_O_RDONLY = 0x00000000;
  static final int OPEN_O_WRONLY = 0x00000001;
  static final int OPEN_O_RDWR = 0x00000002;
  static final int OPEN_O_CLOEXEC = 0x02000000; /* set close_on_exec */
  static final int OPEN_O_LARGEFILE = 0x00100000;
  static final int OPEN_O_TMPFILE = 0x020000000;

  /**
   * Given a {@code pathName} for a file, this method returns a file descriptor, a small,
   * nonnegative integer for use in subsequent system calls.
   *
   * @param pathName path of the file to open
   * @param flags file creation flags and file status flags can be bitwise-or'd
   * @return the new file descriptor, or -1 if an error occurred
   */
  // http://man7.org/linux/man-pages/man2/open.2.html
  // int open(const char *pathname, int flags);
  int open(String pathName, int flags);

  /**
   * Closes a file descriptor, so that it no longer refers to any file and may be reused.
   *
   * @param fd a file descriptor
   * @return 0 on success, -1 on failure.
   */
  // http://man7.org/linux/man-pages/man2/close.2.html
  int close(int fd);

  static final int FALLOCATE_FALLOC_FL_KEEP_SIZE = 0x01; /* default is extend size */
  static final int FALLOCATE_FALLOC_FL_PUNCH_HOLE = 0x02; /* de-allocates range */

  // http://man7.org/linux/man-pages/man2/fallocate.2.html
  // int fallocate(int fd, int mode, off_t offset, off_t len);
  int fallocate(int fd, int mode, long offset, long len);

  // https://man7.org/linux/man-pages/man2/read.2.html
  // ssize_t read(int fd, void *buf, size_t count);
  int read(int fd, byte[] buffer, long count);
}
