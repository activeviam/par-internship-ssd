package com.activeviam.reference;

import com.activeviam.MemoryAllocator;
import com.activeviam.chunk.AbstractSuperblockChunk;
import com.activeviam.platform.LinuxPlatform;
import java.util.Arrays;

public class Superblock extends ABlockAllocator {

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final SuperblockManager storage;
    protected AbstractSuperblockChunk.Header[] owners;
    protected volatile boolean isActive;

    public Superblock(final SuperblockManager storage, final long size) {
        super(size, storage.size());
        this.storage = storage;
        this.owners = new AbstractSuperblockChunk.Header[this.capacity];
    }

    @Override
    protected long virtualAlloc(long size) {
        final MemoryAllocator.ReturnValue value = this.storage.allocate();
        if (value != null) {
            this.activate();
            return value.getBlockAddress();
        } else {
            return NULL_POINTER;
        }
    }

    @Override
    protected void doAllocate(long ptr, long size) {}

    @Override
    protected void doFree(long ptr, long size) {
        this.owners[getPosition(ptr)] = null;
    }

    @Override
    protected void doRelease(long ptr, long size) {
        this.rwlock.writeLock().lock();
        try {
            Arrays.stream(this.owners).forEach(header -> {
                if (header != null) {
                    if (header.getAllocatorValue().isDirtyBlock()) {
                        PLATFORM.writeToFile(header.getFd(), header.getAllocatorValue().getBlockAddress(), this.size);
                    }
                    this.free(header.getAllocatorValue());
                    header.getAllocatorValue().cleanBlock();
                    header.getAllocatorValue().desactivateBlock();
                }
            });
            this.desactivate();
            this.storage.free(new MemoryAllocator.ReturnValue(this.storage, ptr));
        } finally {
            this.rwlock.writeLock().unlock();
        }
    }

    public boolean registerOwner(AbstractSuperblockChunk.Header owner) {
        this.rwlock.readLock().lock();
        try {
            if (this.isActive()) {
                this.owners[getPosition(owner.getAllocatorValue().getBlockAddress())] = owner;
                return true;
            } else {
                return false;
            }
        } finally {
            this.rwlock.readLock().unlock();
        }
    }

    public void activate() { this.isActive = true; }
    public void desactivate() { this.isActive = false; }
    public boolean isActive() { return this.isActive; }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("Headers\n");
        Arrays.stream(this.owners).forEach(header -> {
            if (header != null) {
                sb.append(header).append("\n");
            }
        });

        return sb.toString();
    }
}
