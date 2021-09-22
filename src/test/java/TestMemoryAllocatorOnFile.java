/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

import com.activeviam.reference.MemoryAllocatorOnFile;
import java.nio.file.Path;

import com.activeviam.reference.SuperblockManager;
import com.activeviam.reference.SuperblockMemoryAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestMemoryAllocatorOnFile {

  @Test
  void testAllocatingBlock(@TempDir Path tempDir) {
    System.out.println("Running test with temp dir " + tempDir);
    final var storage = new SuperblockManager(1024, 1024, false);
    final var allocator = new SuperblockMemoryAllocator(tempDir, storage);
    final var alloc1 = 2 * MemoryAllocator.PAGE_SIZE;
    final var ptr1 = allocator.allocateMemory(alloc1);
    final var alloc2 = MemoryAllocator.PAGE_SIZE;
    final var ptr2 = allocator.allocateMemory(alloc2);
    allocator.freeMemory(ptr1, alloc1);
    allocator.freeMemory(ptr2, alloc2);
  }

}