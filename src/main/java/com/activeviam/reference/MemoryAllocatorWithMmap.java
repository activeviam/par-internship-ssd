package com.activeviam.reference;

import java.nio.file.Path;

public class MemoryAllocatorWithMmap extends AMemoryAllocatorOnFile {

    public MemoryAllocatorWithMmap(final Path dir) {
        super(dir);
    }

    @Override
    public ReturnValue allocateMemory(final long bytes) {
        return getOrCreateAllocator(bytes).allocate();
    }

    @Override
    public void freeMemory(ReturnValue value) { value.getBlockAllocator().free(value); }

    @Override
    protected IBlockAllocatorFactory createBlockAllocatorFactory() {
        return (size, blockSize, useHugePage) -> {
            final var ba = new BlockAllocatorOnFile(this.dir, size, blockSize, useHugePage);
            ba.init();
            return ba;
        };
    }
}
