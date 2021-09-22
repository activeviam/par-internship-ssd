package com.activeviam.chunk;

import com.activeviam.platform.LinuxPlatform;
import com.activeviam.reference.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;

public abstract class AbstractSuperblockChunk<K> implements Chunk<K>, Closeable {

    protected static final LongSupplier ID_GENERATOR = new AtomicLong()::getAndIncrement;
    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    public class Header {
        public long ptr;
        public Header(final long ptr, final int fd) { this.ptr = ptr; this.fd = fd; }
    };
    protected final Header header;

    private final int capacity;
    private final long blockSize;
    private final Superblock superblock;

    public AbstractSuperblockChunk(
            final SuperblockMemoryAllocator allocator, final int capacity, final long blockSize) {
        this.capacity = capacity;
        this.blockSize = blockSize;

        final int fd = PLATFORM.openFile(allocator.dir + "_" + ID_GENERATOR.getAsLong());
        final long ptr = allocator.allocateMemory(this.blockSize);

        this.header = new Header(ptr, fd);

        this.superblock = allocator.retrieveSuperblock(ptr, this.blockSize);
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    protected final long offset(final long offset) {
        return this.header.ptr + offset;
    }

    @Override
    public void close() {
        if (this.header.ptr >= 0) {
            this.superblock.free(this.header.ptr);
            this.header.ptr = NULL_POINTER;
        } else {
            throw new IllegalStateException("Cannot free twice the same block");
        }
    }
}
