/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.reference.MemoryAllocatorOnFile;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

public class TestFileDoubleChunk implements SpecTestDoubleChunk {

  private MemoryAllocatorOnFile allocator;

  @TempDir static Path tempDir;

  @BeforeEach
  void createAllocator() {
    this.allocator = new MemoryAllocatorOnFile(tempDir);
  }

  @AfterEach
  void cleanAllocator() {
    this.allocator.close();
    this.allocator = null;
  }

  @Override
  public DoubleChunk createChunk(int capacity) {
    return new FileDoubleChunk(this.allocator, capacity);
  }
}
