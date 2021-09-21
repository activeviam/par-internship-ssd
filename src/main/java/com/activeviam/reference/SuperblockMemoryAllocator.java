package com.activeviam.reference;

import java.nio.file.Path;
import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;

public class SuperblockMemoryAllocator extends AMemoryAllocatorOnFile {

    protected final SuperblockManager storage;

    public SuperblockMemoryAllocator(final Path dir, final SuperblockManager storage) {
        super(dir);
        this.storage = storage;
    }

    @Override
    public long allocateMemory(long bytes) {
        long ptr;
        if ((ptr = getOrCreateAllocator(bytes).allocate()) == NULL_POINTER) {
            garbageCollect();
            return getOrCreateAllocator(bytes).allocate();
        }
        return ptr;
    }

    @Override
    public void freeMemory(final long address, final long bytes) {
        getOrCreateAllocator(bytes).free(address);
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
