package com.activeviam.reference;

import com.activeviam.chunk.AbstractSuperblockChunk;
import com.activeviam.platform.LinuxPlatform;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

public class Superblock extends ABlockAllocator {

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final ReadWriteLock rwlock;
    protected final SuperblockManager storage;
    protected AbstractSuperblockChunk.Header[] owners;

    public Superblock(final SuperblockManager storage, final long size) {
        super(size, storage.size());
        this.storage = storage;
        this.rwlock = new ReentrantReadWriteLock();
        this.owners = new AbstractSuperblockChunk.Header[this.capacity];
    }

    @Override
    protected long virtualAlloc(long size) { return this.storage.allocate(); }

    @Override
    protected void doAllocate(long ptr, long size) {}

    @Override
    protected void doFree(long ptr, long size) {}

    @Override
    protected void doRelease(long ptr, long size) {

        if (this.rwlock.writeLock().tryLock()) {
            try {
                IntStream.range(0, this.capacity).parallel().forEach(index -> {
                    AbstractSuperblockChunk.Header header = this.owners[index];
                    PLATFORM.writeToFile(header.fd, getAddress(index), this.size);
                    header.ptr = NULL_POINTER;
                });
            } finally {
                this.rwlock.writeLock().unlock();
            }
        }

        if (this.rwlock.writeLock().tryLock()) {
            try {
                this.storage.free(this.blockAddress);
            } finally {
                this.rwlock.writeLock().unlock();
            }
        }
    }

    public void registerOwner(AbstractSuperblockChunk.Header owner) {
        this.owners[getPosition(owner.ptr)] = owner;
    }
}
