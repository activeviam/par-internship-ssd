package com.activeviam.reference;

import java.nio.file.Path;

public class MemoryAllocatorWithMmap extends AMemoryAllocatorOnFile {

    public MemoryAllocatorWithMmap(final Path dir) {
        super(dir);
    }

    @Override
    public long allocateMemory(final long bytes) {
        return getOrCreateAllocator(bytes).allocate();
    }

    @Override
    public void freeMemory(final long address, final long bytes) {
        getOrCreateAllocator(bytes).free(address);
    }

    @Override
    protected IBlockAllocatorFactory createBlockAllocatorFactory() {
        return (size, blockSize, useHugePage) -> {
            final var ba = new BlockAllocatorOnFile(this.dir, size, blockSize, useHugePage);
            ba.init();
            return ba;
        };
    }
}
