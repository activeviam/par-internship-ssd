/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

import java.nio.file.Path;

import com.activeviam.reference.SuperblockMemoryAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.activeviam.MemoryAllocator.PAGE_SIZE;

public class TestMemoryAllocatorOnFile {

  @Test
  void testAllocatingBlock(@TempDir Path tempDir) {
    System.out.println("Running test with temp dir " + tempDir);
    final var allocator = new SuperblockMemoryAllocator(tempDir, 1 << 21, 1 << 30, false);
    final var size1 = 2 * PAGE_SIZE;
    final var alloc1 = allocator.allocateMemory(size1);
    final var size2 = PAGE_SIZE;
    final var alloc2 = allocator.allocateMemory(size2);
    allocator.freeMemory(alloc1);
    allocator.freeMemory(alloc2);
  }

}
