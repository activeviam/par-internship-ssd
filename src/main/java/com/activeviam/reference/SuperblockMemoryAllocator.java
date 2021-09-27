package com.activeviam.reference;

import com.activeviam.MemoryAllocator;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.LongSupplier;

import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;

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

    public ReadWriteLock getGcRwlock() {
        return gcRwlock;
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
        try {
            this.gcRwlock.readLock().lock();
            return getOrCreateAllocator(bytes).allocate();
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
        try {
            gcRwlock.writeLock().lock();
            var it = this.allocators.entrySet().iterator();
            while (it.hasNext() && this.storage.count > GARBAGE_COLLECT_RATE * this.storage.capacity) {
                it.next().getValue().release();
            }
            this.gcCounter.incrementAndGet();
        } finally {
            gcRwlock.writeLock().unlock();
        }
    }

    public long getGCCounter() {
        return this.gcCounter.get();
    }

    @Override
    public void close() {
        this.storage.release();
    }
}
