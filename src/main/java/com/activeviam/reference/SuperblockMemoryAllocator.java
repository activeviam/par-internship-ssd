package com.activeviam.reference;

import com.activeviam.MemoryAllocator;

import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;

public class SuperblockMemoryAllocator extends AMemoryAllocatorOnFile {

    protected final SuperblockManager storage;
    protected final ReadWriteLock rwlock;

    public SuperblockMemoryAllocator(final Path dir, final SuperblockManager storage) {
        super(dir);
        this.storage = storage;
        this.rwlock = new ReentrantReadWriteLock();
    }


    @Override
    public MemoryAllocator.ReturnValue allocateMemory(long bytes) {
        MemoryAllocator.ReturnValue value = getOrCreateAllocator(bytes).allocate();
        while (value.ptr == NULL_POINTER) {
            garbageCollect();
            value = getOrCreateAllocator(bytes).allocate();
        }
        return value;
    }

    @Override
    public void freeMemory(MemoryAllocator.ReturnValue value) {
        value.blockAllocator.free(value.ptr);
    }

    @Override
    protected IBlockAllocatorFactory createBlockAllocatorFactory() {
        return (size, blockSize, useHugePage) -> {
            final var superblock = new Superblock(this.storage, size);
            if (superblock.init()) {
                return superblock;
            } else {
                superblock.release();
                return null;
            }
        };
    }

    private void garbageCollect() {

    }

}
