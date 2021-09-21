package com.activeviam.reference;

import com.activeviam.platform.LinuxPlatform;

public class SuperblockManager extends ABlockAllocator {

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final boolean useHugePage;

    /**
     * Default constructor.
     *
     * @param size      Size of memory (in bytes) that will be allocated when calling {@link #allocate()}.
     * @param blockSize amount of virtual memory to reserve for an entire block
     */
    public SuperblockManager(long size, long blockSize, boolean useHugePage) {
        super(size, blockSize);
        this.useHugePage = useHugePage;
    }

    @Override
    protected long virtualAlloc(long size) {
        long address = PLATFORM.mmapAnon(size, this.useHugePage);
        PLATFORM.mlock(address, size);
        return address;
    }

    @Override
    protected void doAllocate(long ptr, long size) { PLATFORM.commit(ptr, size, this.useHugePage); }

    @Override
    protected void doFree(long ptr, long size) { /* do nothing */ }

    @Override
    protected void doRelease(long ptr, long size) { PLATFORM.munmap(ptr, size); }
}
