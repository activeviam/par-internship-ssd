package com.activeviam.reference;

import com.activeviam.IMemoryAllocator;
import com.activeviam.chunk.ASwapChunk;
import com.activeviam.platform.LinuxPlatform;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

public class SwapBlockAllocator extends ABlockStackAllocator {

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final VirtMemStorage storage;

    protected ASwapChunk.Header[] owners;
    protected VarHandle OWNERS = MethodHandles.arrayElementVarHandle(ASwapChunk.Header[].class);

    protected volatile boolean isActive;

    public SwapBlockAllocator(final VirtMemStorage storage, final long size) {
        super(size, storage.size());
        this.storage = storage;
        this.owners = new ASwapChunk.Header[this.capacity];
    }

    public boolean isActive() { return this.isActive; }

    @Override
    protected long virtualAlloc(long size) {
        final var value = this.storage.allocate();
        if (value != null) {
            this.isActive = true;
            return value.getBlockAddress();
        } else {
            return NULL_POINTER;
        }
    }

    @Override
    protected void doAllocate(long ptr, long size) {}

    @Override
    protected void doFree(long ptr, long size) {
        OWNERS.setVolatile(owners, getPosition(ptr), null);
    }

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
            for (int i = 0; i < this.capacity; i++) {
                final var header = (ASwapChunk.Header)OWNERS.getVolatile(this.owners, i);
                if (header != null) {
                    final var value = header.getAllocatorValue();
                    if (value.isDirtyBlock()) {
                        PLATFORM.writeToFile(header.getFd(), value.getBlockAddress(), this.size);
                    }
                    this.free(value);
                }
            }
            this.isActive = false;
            this.storage.free(new IMemoryAllocator.ReturnValue(this.storage, ptr));
        } finally {
            this.rwlock.writeLock().unlock();
        }
    }

    /**
     * Registers a chunk owning a block of the current SwapBlockAllocator. At the moment of this
     * call the SwapBlockAllocator may be already desactivated by {@link #release()} function.
     *
     * @param owner header of the chunk to register
     * @return true if the chunk header has been registered, false otherwise.
     */
    public boolean registerOwner(ASwapChunk.Header owner) {
        this.rwlock.readLock().lock();
        try {
            if (this.isActive()) {
                final int pos = getPosition(owner.getAllocatorValue().getBlockAddress());
                OWNERS.setVolatile(this.owners, pos, owner);
                return true;
            } else {
                return false;
            }
        } finally {
            this.rwlock.readLock().unlock();
        }
    }

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
