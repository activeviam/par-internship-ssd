package com.activeviam.reference;

import java.nio.file.Path;

public class MmapMemoryAllocator extends AMemoryAllocatorOnFile {

    public MmapMemoryAllocator(final Path dir) {
        super(dir);
    }

    @Override
    public ReturnValue allocateMemory(final long bytes) {
        return getOrCreateAllocator(bytes).allocate();
    }

    @Override
    public void freeMemory(ReturnValue value) { ((ABlockStackAllocator)(value.getMetadata())).free(value); }

    @Override
    protected IBlockAllocatorFactory createBlockAllocatorFactory() {
        return (size, blockSize, useHugePage) -> {
            final var ba = new MmapBlockAllocator(this.dir, size, blockSize, useHugePage);
            ba.init();
            return ba;
        };
    }
}
