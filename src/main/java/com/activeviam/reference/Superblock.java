package com.activeviam.reference;

import com.activeviam.MemoryAllocator;
import com.activeviam.UnsafeUtil;
import com.activeviam.chunk.AbstractSuperblockChunk;
import com.activeviam.platform.LinuxPlatform;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

public class Superblock extends ABlockAllocator {

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final ReadWriteLock rwlock;
    protected final SuperblockManager storage;
    protected AbstractSuperblockChunk.Header[] owners;
    protected boolean isActive;

    public Superblock(final SuperblockManager storage, final long size) {
        super(size, storage.size());
        this.storage = storage;
        this.rwlock = new ReentrantReadWriteLock();
        this.owners = new AbstractSuperblockChunk.Header[this.capacity];
        this.activate();
    }

    public ReadWriteLock getRwlock() {
        return rwlock;
    }

    @Override
    protected long virtualAlloc(long size) {
        final var value = this.storage.allocate();
        if (value != null) {
            return value.getBlockAddress();
        } else {
            return NULL_POINTER;
        }
    }

    @Override
    protected void doAllocate(long ptr, long size) {
        this.activate();
    }

    @Override
    protected void doFree(long ptr, long size) {
        this.owners[getPosition(ptr)] = null;
    }

    @Override
    protected void doRelease(long ptr, long size) {

        try {
            this.rwlock.writeLock().lock();
            Arrays.stream(this.owners).forEach(header -> {
                if (header != null) {
                    if (header.getAllocatorValue().isSpoiledBlock()) {
                        PLATFORM.writeToFile(header.getFd(), header.getAllocatorValue().getBlockAddress(), this.size);
                    }
                    this.free(header.getAllocatorValue());
                    header.getAllocatorValue().cleanBlock();
                    header.getAllocatorValue().desactivateBlock();
                }
            });
        } finally {
            this.rwlock.writeLock().unlock();
        }

        try {
            this.rwlock.writeLock().lock();
            this.desactivate();
            this.storage.free(new MemoryAllocator.ReturnValue(this.storage, ptr));
        } finally {
            this.rwlock.writeLock().unlock();
        }
    }

    public void registerOwner(AbstractSuperblockChunk.Header owner) {
        this.owners[getPosition(owner.getAllocatorValue().getBlockAddress())] = owner;
    }

    private void activate() { this.isActive = true; }
    private void desactivate() { this.isActive = false; }
    public boolean isActive() {
        return isActive;
    }
}
