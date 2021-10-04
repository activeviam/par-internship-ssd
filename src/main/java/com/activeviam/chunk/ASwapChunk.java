package com.activeviam.chunk;

import com.activeviam.IMemoryAllocator;
import com.activeviam.UnsafeUtil;
import com.activeviam.platform.LinuxPlatform;
import com.activeviam.reference.*;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

public abstract class ASwapChunk<K> implements Chunk<K>, Closeable {

    private static final LongSupplier ID_GENERATOR = new AtomicLong()::getAndIncrement;

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();
    /** Unsafe provider. */
    protected static final sun.misc.Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    public static class Header {

        private final IMemoryAllocator.ReturnValue allocatorValue;
        private final int fd;

        public Header(final IMemoryAllocator.ReturnValue value, final int fd) {
            this.allocatorValue = value;
            this.fd = fd;
        }

        public IMemoryAllocator.ReturnValue getAllocatorValue() {
            return this.allocatorValue;
        }
        public int getFd() {
            return this.fd;
        }
    }

    protected final SwapMemoryAllocator allocator;
    protected final int capacity;
    protected final long blockSize;
    protected Header header;
    protected final Lock chunkLock;

    public ASwapChunk(
            final SwapMemoryAllocator allocator, final int capacity, final long blockSize) {
        this.allocator = allocator;
        this.capacity = capacity;
        this.blockSize = blockSize;
        final var path = this.allocator.dir.resolve("chunk_" + capacity + "_" + ID_GENERATOR.getAsLong());
        final int fd = PLATFORM.openFile(path.toAbsolutePath().toString());
        PLATFORM.fallocate(fd, 0, this.blockSize, false);
        this.header = assignNewMemoryBlock(fd);
        this.chunkLock = new ReentrantLock();
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    protected final long offset(final long offset) {
        return this.header.allocatorValue.getBlockAddress() + offset;
    }

    @Override
    public void close() {
        if (this.header.allocatorValue != null) {
            ((ABlockStackAllocator)(this.header.allocatorValue.getMetadata())).free(this.header.allocatorValue);
            PLATFORM.closeFile(this.header.fd);
        } else {
            throw new IllegalStateException("Cannot free twice the same block");
        }
    }

    protected void relocateMemoryBlock() {

        if (!this.chunkLock.tryLock()) {
            this.chunkLock.lock();
            this.chunkLock.unlock();
            return;
        }

        Header newHeader = assignNewMemoryBlock(this.header.getFd());
        final var allocatorValue = newHeader.getAllocatorValue();
        final var superblock = (SwapBlockAllocator) allocatorValue.getMetadata();

        superblock.rwlock().readLock().lock();
        try {
            final long newAddress = allocatorValue.getBlockAddress();
            if (superblock.isActive()) {
                PLATFORM.readFromFile(this.header.getFd(), newAddress, this.blockSize);
                this.header = newHeader;
            }
        } finally {
            superblock.rwlock().readLock().unlock();
            this.chunkLock.unlock();
        }
    }

    private Header assignNewMemoryBlock(int fd) {
        final var av = this.allocator.allocateMemory(this.blockSize);
        final Header header = new Header(av, fd);
        final var superblock = (SwapBlockAllocator)av.getMetadata();
        superblock.registerOwner(header);
        return header;
    }
}
