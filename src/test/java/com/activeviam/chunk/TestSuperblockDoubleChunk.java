package com.activeviam.chunk;

import com.activeviam.platform.LinuxPlatform;
import com.activeviam.reference.SuperblockMemoryAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSuperblockDoubleChunk implements SpecTestDoubleChunk {

    private SuperblockMemoryAllocator allocator;

    @Test
    void testFree() {


    }

    @TempDir
    static Path tempDir;

    @BeforeEach
    void createAllocator() {
        final long hugePageSize = 1 << 21;
        this.allocator = new SuperblockMemoryAllocator(tempDir, hugePageSize, 8 * hugePageSize, false);
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
        if (count < 0) {
            System.out.println("GC counter = " + this.allocator.getGcCounter());
        } else {
            assertThat(this.allocator.getGcCounter()).isEqualTo(count);
        }
    }

    @Override
    public void dumpAllocatorState() {
        synchronized (this.allocator) {
            System.out.println(this.allocator);
        }
    }
}
