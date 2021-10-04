package com.activeviam.reference;

import com.activeviam.platform.LinuxPlatform;

public class VirtMemStorage extends ABlockStackAllocator {

    protected static final LinuxPlatform PLATFORM = LinuxPlatform.getInstance();

    protected final boolean useHugePage;
    protected boolean lockVirtMem;

    /**
     * Default constructor.
     *
     * @param size      Size of memory (in bytes) that will be allocated when calling {@link #allocate()}.
     * @param blockSize amount of virtual memory to reserve for an entire block
     */
    public VirtMemStorage(long size, long blockSize, boolean useHugePage) {
        super(size, blockSize);
        this.useHugePage = useHugePage;
        this.lockVirtMem = false;
    }

    @Override
    protected long virtualAlloc(long size) {

        final long mmapSoftLimit = PLATFORM.getSoftLimit(PLATFORM.RLIMIT_DATA);
        final long mmapHardLimit = PLATFORM.getHardLimit(PLATFORM.RLIMIT_DATA);

        if (mmapHardLimit >= 0 && size > mmapHardLimit) {
            LOGGER.warning("cannot request cache of size " + size);
            LOGGER.warning("the actual cache size will be bounded to the hard limit of the process");
            size = mmapHardLimit;
        }

        if (mmapSoftLimit >= 0 && size > mmapSoftLimit) {
            PLATFORM.setSoftLimit(PLATFORM.RLIMIT_DATA, mmapHardLimit);
        }

        long address = PLATFORM.mmapAnon(size, this.useHugePage);

        final long memlockSoftLimit = PLATFORM.getSoftLimit(PLATFORM.RLIMIT_MEMLOCK);
        final long memlockHardLimit = PLATFORM.getHardLimit(PLATFORM.RLIMIT_MEMLOCK);

        if (memlockHardLimit >= 0 && size > memlockHardLimit) {
            LOGGER.warning("requested cache cannot be entirely locked in memory due to system limitations");
            return address;
        }

        if (memlockSoftLimit >= 0 && size > memlockSoftLimit) {
            PLATFORM.setSoftLimit(PLATFORM.RLIMIT_MEMLOCK, size);
        }

        this.lockVirtMem = true;
        PLATFORM.mlock(address, size);
        return address;
    }

    @Override
    protected void doAllocate(long ptr, long size) { PLATFORM.commit(ptr, size, this.useHugePage); }

    @Override
    protected void doFree(long ptr, long size) { }

    @Override
    protected void doRelease(long ptr, long size) {
        if (this.lockVirtMem) {
            PLATFORM.munlock(ptr, size);
        }
        PLATFORM.munmap(ptr, size);
    }
}
