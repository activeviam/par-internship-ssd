package com.activeviam.chunk;

import com.activeviam.platform.LinuxPlatform;
import com.activeviam.reference.AMemoryAllocatorOnFile;
import com.activeviam.reference.MemoryAllocatorWithMmap;
import com.activeviam.reference.Superblock;
import com.activeviam.reference.SuperblockMemoryAllocator;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public abstract class AbstractSuperblockChunk<K> implements Chunk<K>, Closeable {

    protected static final LongSupplier ID_GENERATOR = new AtomicLong()::getAndIncrement;
    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final Superblock.SuperblockChunkHeader header;

    private final int capacity;
    private final SuperblockMemoryAllocator allocator;
    private final long blockSize;

    long ptr;

    public AbstractSuperblockChunk(
            final SuperblockMemoryAllocator allocator, final int capacity, final long blockSize) {
        this.capacity = capacity;
        this.allocator = allocator;
        this.blockSize = blockSize;
        this.ptr = allocator.allocateMemory(this.blockSize);
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    protected final long offset(final long offset) {
        return this.ptr + offset;
    }

    @Override
    public void close() {
        if (this.ptr >= 0) {
            this.allocator.freeMemory(this.ptr, this.blockSize);
            this.ptr = -1;
        } else {
            throw new IllegalStateException("Cannot free twice the same block");
        }
    }
}
