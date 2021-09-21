package com.activeviam.reference;

import com.activeviam.chunk.AbstractSuperblockChunk;
import com.activeviam.platform.LinuxPlatform;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

public class Superblock extends ABlockAllocator {

    public class SuperblockChunkHeader {

        public final int fd;

        public SuperblockChunkHeader(final int fd) { this.fd = fd; }
    }

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final ReadWriteLock rwlock;
    protected final SuperblockManager storage;
    protected SuperblockChunkHeader [] owners;

    public Superblock(final SuperblockManager storage, final long size) {
        super(size, storage.size());
        this.storage = storage;
        this.rwlock = new ReentrantReadWriteLock();
        this.owners = new SuperblockChunkHeader[this.capacity];
    }

    @Override
    protected long virtualAlloc(long size) { return this.storage.allocate(); }

    @Override
    protected void doAllocate(long ptr, long size) { }

    @Override
    protected void doFree(long ptr, long size) { }

    @Override
    protected void doRelease(long ptr, long size) {

        if (this.rwlock.writeLock().tryLock()) {

            try {
                IntStream.range(0, this.capacity).parallel().forEach(index -> {
                    SuperblockChunkHeader header = this.owners[index];
                    PLATFORM.writeToFile(header.fd, getAddress(index), this.size);
                    header.chunk = null;
                });
                this.storage.free();

            } finally {
                this.rwlock.writeLock().unlock();
            }
        }
    }

    public void registerOwner(long ptr, SuperblockChunkHeader owner) {
        this.owners[getPosition(ptr)] = owner;
    }
}
