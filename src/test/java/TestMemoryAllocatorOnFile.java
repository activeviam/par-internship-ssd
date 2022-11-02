/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

import com.activeviam.allocator.UnsafeNativeMemoryAllocator;
import org.junit.jupiter.api.Test;

public class TestMemoryAllocatorOnFile {

  @Test
  void testAllocatingBlock() {
    final var allocator = new UnsafeNativeMemoryAllocator();
    final var alloc1 = 2 * 4; // 2 ints
    final var ptr1 = allocator.allocateMemory(alloc1);
    final var alloc2 = 3 * 8; // 3 doubles
    final var ptr2 = allocator.allocateMemory(alloc2);
    allocator.freeMemory(ptr1, alloc1);
    allocator.freeMemory(ptr2, alloc2);
  }
}
