package com.activeviam.chunk;

import com.activeviam.platform.LinuxPlatform;
import com.activeviam.reference.SwapBlockAllocator;
import com.activeviam.reference.SwapMemoryAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSuperblockDoubleChunk implements SpecTestDoubleChunk {

    private SwapMemoryAllocator allocator;

    private void checkSwapCounter(long count) {
        System.out.println("Checking GC counter");
        if (count < 0) {
            System.out.println("GC counter = " + this.allocator.getSwapCounter());
        } else {
            assertThat(this.allocator.getSwapCounter()).isEqualTo(count);
        }
    }

    @Test
    void testArbitration() {

        final int numThreads = 2;
        final List<Thread> threads = new LinkedList<>();
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int threadId = 0; threadId < numThreads; threadId++) {

            final int id = threadId + 1;

            final Thread thread = new Thread(() -> {

                final var chunk = new SwapDoubleChunk(this.allocator, 1 << (id + 11));

                for (int i = 0; i < chunk.capacity(); i++) {
                    chunk.writeDouble(i, -5.8 * id * i);
                }

                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < chunk.capacity(); i++) {
                    if (chunk.readDouble(i) != -5.8 * id * i) {
                        System.out.println("[" + i
                                + "] expected = " + (-5.8 * id * i)
                                + ", actual = " + chunk.readDouble(i)
                                + ", dirty? " + chunk.header.getAllocatorValue().isDirtyBlock()
                                + ", allocator " + ((SwapBlockAllocator)(chunk.header.getAllocatorValue().getMetadata())).active());
                    }
                }

            }, "thread " + id);

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(this.allocator.getSwapCounter());
    }

    @Test
    void testFree() {

        final int numThreads = 9;
        final List<Thread> threads = new LinkedList<>();

        for (int threadId = 0; threadId < numThreads; threadId++) {

            final int id = threadId + 1;

            final Thread thread = new Thread(() -> {

                for (int k = 0; k < 10; k++) {
                    final var chunk = new SwapDoubleChunk(this.allocator, 1 << (id + 9));

                    for (int i = 0; i < chunk.capacity(); i++) {
                        chunk.writeDouble(i, -5.8 * id * i);
                    }

                    for (int i = 0; i < chunk.capacity(); i++) {
                        if (chunk.readDouble(i) != -5.8 * id * i) {
                            System.out.println("[" + i
                                    + "] expected = " + (-5.8 * id * i)
                                    + ", actual = " + chunk.readDouble(i)
                                    + ", dirty? " + chunk.header.getAllocatorValue().isDirtyBlock()
                                    + ", allocator " + ((SwapBlockAllocator)(chunk.header.getAllocatorValue().getMetadata())).active());
                        }
                    }

                    chunk.close();
                }
            }, "thread " + id);

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(this.allocator.getSwapCounter());
    }

    @TempDir
    static Path tempDir;

    @BeforeEach
    void createAllocator() {
        final long hugePageSize = 1 << 21;
        this.allocator = new SwapMemoryAllocator(tempDir, hugePageSize, hugePageSize, false);
    }

    @AfterEach
    void cleanAllocator() {
        this.allocator.close();
        this.allocator = null;
    }

    @Override
    public DoubleChunk createChunk(int capacity) {
        return new SwapDoubleChunk(this.allocator, capacity);
    }
}
