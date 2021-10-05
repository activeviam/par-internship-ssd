package com.activeviam.reference;

import com.activeviam.IMemoryAllocator;
import com.activeviam.UnsafeUtil;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

public class SwapMemoryAllocator extends AMemoryAllocatorOnFile {

    protected final VirtMemStorage storage;
    protected long swapCounter = 0;
    protected volatile int ongoingSwap = 0;
    protected Predicate<ABlockStackAllocator> cold;

    public static final float GARBAGE_COLLECT_RATE = 0.8f;

    public SwapMemoryAllocator(final Path dir, final long size, final long blockSize, final boolean useHugePage) {
        super(dir);
        this.storage = new VirtMemStorage(size, blockSize, useHugePage);
        this.storage.init();
        this.cold = new Predicate<>() {
            private int MIN_USAGE_COUNT = 100;
            @Override
            public boolean test(ABlockStackAllocator blockAllocator) {
                final var superblock = (SwapBlockAllocator)blockAllocator;
                if (superblock.usages() < MIN_USAGE_COUNT) {
                    superblock.release();
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    @Override
    public IMemoryAllocator.ReturnValue allocateMemory(long bytes) {
        IMemoryAllocator.ReturnValue value = getOrCreateAllocator(bytes).allocate();
        while (value == null) {
            swap();
            value = getOrCreateAllocator(bytes).allocate();
        }
        return value;
    }

    @Override
    public void freeMemory(IMemoryAllocator.ReturnValue value) {
        ((ABlockStackAllocator)(value.getMetadata())).free(value);
    }

    @Override
    protected IBlockAllocatorFactory createBlockAllocatorFactory() {
        return (size, blockSize, useHugePage) -> {
            final var swapBlockAllocator = new SwapBlockAllocator(this.storage, size);
            if (swapBlockAllocator.init()) {
                return swapBlockAllocator;
            } else {
                return null;
            }
        };
    }

    public long getSwapCounter() {
        return this.swapCounter;
    }

    @Override
    public void close() {
        this.storage.release();
    }

    private static final long ongoingSwapOffset =
            UnsafeUtil.getFieldOffset(SwapMemoryAllocator.class, "ongoingSwap");

    private static boolean casOngoingSwap(
            final SwapMemoryAllocator memoryAllocator, final int expect, final int update) {
        return UnsafeUtil.compareAndSwapInt(memoryAllocator, ongoingSwapOffset, expect, update);
    }

    private void swap() {
        if (casOngoingSwap(this, 0, -1)) {
            try {
                synchronized (this.allocators) {
                    var it = this.allocators.entrySet().iterator();
                    while (it.hasNext() && this.storage.count > GARBAGE_COLLECT_RATE * this.storage.capacity) {
                        ((BlockAllocatorManager) (it.next().getValue())).remove();
                    }
                }
                this.swapCounter++;
            } finally {
                this.ongoingSwap = 0;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Storage:\n");
        sb.append(storage);
        sb.append("\nAllocators:\n");
        for (final var allocator : this.allocators.entrySet()) {
            sb.append(allocator.getValue()).append("\n");
        }
        return sb.toString();
    }
}
