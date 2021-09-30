package com.activeviam.chunk;

import com.activeviam.UnsafeUtil;
import com.activeviam.reference.Superblock;
import com.activeviam.reference.SuperblockMemoryAllocator;

import java.util.logging.Logger;

import static com.activeviam.MemoryAllocator.PAGE_SIZE;
import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;

public class SuperblockDoubleChunk extends AbstractSuperblockChunk<Double> implements DoubleChunk {

    /** The order of the size in bytes of an element. */
    private static final int ELEMENT_SIZE_ORDER = 3;

    public SuperblockDoubleChunk(final SuperblockMemoryAllocator allocator, final int capacity) {
        super(allocator, capacity, computeBlockSize(capacity));
    }

    private static long computeBlockSize(final int capacity) {
        final long minSize = capacity << ELEMENT_SIZE_ORDER;
        if (minSize % PAGE_SIZE == 0) {
            return minSize;
        } else {
            // Find the closest multiple of PAGE_SIZE
            final var size = ((minSize / PAGE_SIZE) + 1) * PAGE_SIZE;
            Logger.getLogger("chunk").warning("Wasting " + (size - minSize) + " bytes");
            return size;
        }
    }

    @Override
    public double readDouble(int position) {
        assert 0 <= position && position < capacity();

        for (;;) {
            final var allocatorValue = this.header.getAllocatorValue();
            final var blockAllocator = allocatorValue.getBlockAllocator();
            final var rwlock = blockAllocator.rwlock();

            if (rwlock.readLock().tryLock()) {
                try {
                    if (allocatorValue.isActiveBlock()) {
                        blockAllocator.updateTimestamp();
                        return UNSAFE.getDouble(offset(position << ELEMENT_SIZE_ORDER));
                    }
                } finally {
                  rwlock.readLock().unlock();
                }
            }

            relocateMemoryBlock();
        }
    }

    @Override
    public void writeDouble(int position, double value) {
        assert 0 <= position && position < capacity();
        for (;;) {

            final var allocatorValue = this.header.getAllocatorValue();
            final var blockAllocator = allocatorValue.getBlockAllocator();
            final var rwlock = blockAllocator.rwlock();

            if (rwlock.readLock().tryLock()) {
                try {
                    if (allocatorValue.isActiveBlock()) {
                        blockAllocator.updateTimestamp();
                        allocatorValue.dirtyBlock();
                        UNSAFE.putDouble(offset(position << ELEMENT_SIZE_ORDER), value);
                        return;
                    }
                } finally {
                    rwlock.readLock().unlock();
                }
            }

            relocateMemoryBlock();
        }
    }

}
