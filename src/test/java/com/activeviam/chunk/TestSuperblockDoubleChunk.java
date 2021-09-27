package com.activeviam.chunk;

import com.activeviam.platform.LinuxPlatform;
import com.activeviam.reference.SuperblockMemoryAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSuperblockDoubleChunk implements SpecTestDoubleChunk {

    private SuperblockMemoryAllocator allocator;

    @TempDir
    static Path tempDir;

    @BeforeEach
    void createAllocator() {
        final long hugePageSize = 1 << 21;
        this.allocator = new SuperblockMemoryAllocator(tempDir, hugePageSize, hugePageSize, false);
    }

    @AfterEach
    void cleanAllocator() {
        this.allocator.close();
        this.allocator = null;
    }

    @Override
    public DoubleChunk createChunk(int capacity) {
        return new SuperblockDoubleChunk(this.allocator, capacity);
    }

    @Override
    public void checkGcCounter(long count) {
        System.out.println("Checking GC counter");
        assertThat(this.allocator.getGCCounter()).isEqualTo(count);
    }
}
