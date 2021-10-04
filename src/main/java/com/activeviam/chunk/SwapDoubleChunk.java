package com.activeviam.chunk;

import com.activeviam.reference.ABlockStackAllocator;
import com.activeviam.reference.SwapBlockAllocator;
import com.activeviam.reference.SwapMemoryAllocator;

import java.util.logging.Logger;

import static com.activeviam.IMemoryAllocator.PAGE_SIZE;
import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;

public class SwapDoubleChunk extends ASwapChunk<Double> implements DoubleChunk {

    /** The order of the size in bytes of an element. */
    private static final int ELEMENT_SIZE_ORDER = 3;

    public SwapDoubleChunk(final SwapMemoryAllocator allocator, final int capacity) {
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
            final var blockAllocator = (SwapBlockAllocator)allocatorValue.getMetadata();
            final var rwlock = blockAllocator.rwlock();

            if (rwlock.readLock().tryLock()) {
                try {
                    if (blockAllocator.isActive()) {
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
            final var blockAllocator = (SwapBlockAllocator)allocatorValue.getMetadata();
            final var rwlock = blockAllocator.rwlock();

            if (rwlock.readLock().tryLock()) {
                try {
                    if (blockAllocator.isActive()) {
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
