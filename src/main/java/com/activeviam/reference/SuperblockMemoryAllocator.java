package com.activeviam.reference;

import com.activeviam.MemoryAllocator;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SuperblockMemoryAllocator extends AMemoryAllocatorOnFile {

    protected final SuperblockManager storage;
    protected final ReadWriteLock gcRwlock;
    protected final AtomicLong gcCounter;

    public static final float GARBAGE_COLLECT_RATE = 0.8f;

    public SuperblockMemoryAllocator(final Path dir, final long size, final long blockSize, final boolean useHugePage) {
        super(dir);
        this.storage = new SuperblockManager(size, blockSize, useHugePage);
        this.storage.init();
        this.gcRwlock = new ReentrantReadWriteLock();
        this.gcCounter = new AtomicLong(0);
    }

    @Override
    public MemoryAllocator.ReturnValue allocateMemory(long bytes) {
        MemoryAllocator.ReturnValue value = tryAllocateMemory(bytes);
        while (value == null) {
            this.garbageCollect();
            value = tryAllocateMemory(bytes);
        }
        return value;
    }

    private ReturnValue tryAllocateMemory(long bytes) {
        this.gcRwlock.readLock().lock();
        try {
            final var value = getOrCreateAllocator(bytes).allocate();
            return value;
        } finally {
            this.gcRwlock.readLock().unlock();
        }
    }

    @Override
    public void freeMemory(MemoryAllocator.ReturnValue value) {
        if (this.gcRwlock.readLock().tryLock()) {
            try {
                value.getBlockAllocator().free(value);
            } finally {
                this.gcRwlock.readLock().unlock();
            }
        }
    }

    @Override
    protected IBlockAllocatorFactory createBlockAllocatorFactory() {
        return (size, blockSize, useHugePage) -> {
            final var superblock = new Superblock(this.storage, size);
            if (superblock.init()) {
                return superblock;
            } else {
                return null;
            }
        };
    }

    private void garbageCollect() {
        if (this.gcRwlock.writeLock().tryLock()) {
            try {
                var it = this.allocators.entrySet().iterator();
                while (it.hasNext() && this.storage.count > GARBAGE_COLLECT_RATE * this.storage.capacity) {
                    ((BlockAllocatorManager)(it.next().getValue())).garbageCollect();
                }
            } finally {
                this.gcCounter.incrementAndGet();
                this.gcRwlock.writeLock().unlock();
            }
        }
    }

    public long getGcCounter() {
        return this.gcCounter.get();
    }

    @Override
    public void close() {
        this.storage.release();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Storage:\n").append(storage).append("\n\nAllocators:\n");

        for (final var allocator : this.allocators.entrySet()) {
            sb.append(allocator.getValue()).append("\n-----\n");
        }
        return sb.toString();
    }
}
