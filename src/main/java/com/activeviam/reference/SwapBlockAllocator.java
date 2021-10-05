package com.activeviam.reference;

import com.activeviam.IMemoryAllocator;
import com.activeviam.chunk.ASwapChunk;
import com.activeviam.platform.LinuxPlatform;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SwapBlockAllocator extends ABlockStackAllocator {

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final VirtMemStorage storage;
    protected final ReadWriteLock rwlock;

    protected ASwapChunk.Header [] owners;
    protected volatile boolean active;
    protected int usages;

    public SwapBlockAllocator(final VirtMemStorage storage, final long size) {
        super(size, storage.size());
        this.storage = storage;
        this.owners = new ASwapChunk.Header [this.capacity()];
        this.rwlock = new ReentrantReadWriteLock();
    }

    public int usages() {
        final var usages = this.usages;
        this.usages = 0;
        return usages;
    }

    public boolean active() {
        this.usages++;
        return this.active;
    }

    @Override
    protected long virtualAlloc(long size) {
        final var value = this.storage.allocate();
        if (value != null) {
            this.active = true;
            this.usages = 0;
            return value.getBlockAddress();
        } else {
            return NULL_POINTER;
        }
    }

    @Override
    protected void doAllocate(long ptr, long size) {}

    @Override
    protected void doFree(long ptr, long size) { this.owners[getPosition(ptr)] = null; }

    /**
     * Acquires a write lock to block IO operations on SwapBlockAllocator and iterates over {@link #owners} to flush
     * dirty blocks on SSD
     *
     * @param ptr address of the SwapBlockAllocator
     * @param size size of the SwapBlockAllocator
     */
    @Override
    protected void doRelease(long ptr, long size) {
        try {
            this.rwlock.writeLock().lock();
            for (ASwapChunk.Header owner : this.owners) {
                if (owner != null) {
                    final var av = owner.getAllocatorValue();
                    if (av.isDirtyBlock()) {
                        PLATFORM.writeToFile(owner.getFd(), av.getBlockAddress(), this.size);
                    }
                    this.free(av);
                }
            }
            this.active = false;
            this.storage.free(new IMemoryAllocator.ReturnValue(this.storage, ptr));
        } finally {
            this.rwlock.writeLock().unlock();
        }
    }

    public Lock readLock() {
        return this.rwlock.readLock();
    }

    /**
     * Registers a chunk owning a block of the current SwapBlockAllocator. At the moment of this
     * call the SwapBlockAllocator may be already desactivated by {@link #release()} function.
     *
     * @param owner header of the chunk to register
     */
    public void registerOwner(ASwapChunk.Header owner) {
        this.rwlock.readLock().lock();
        try {
            this.owners[getPosition(owner.getAllocatorValue().getBlockAddress())] = owner;
        } finally {
            this.rwlock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Headers\n");
        for (ASwapChunk.Header owner : this.owners) {
            if (owner != null) {
                sb.append(owners).append("\n");
            }
        }
        return sb.toString();
    }
}
