package com.activeviam.chunk;

import com.activeviam.reference.SwapBlockAllocator;
import com.activeviam.reference.SwapMemoryAllocator;

import java.util.logging.Logger;

import static com.activeviam.IMemoryAllocator.PAGE_SIZE;

public class SwapIntegerChunk extends ASwapChunk<Integer> implements IntegerChunk {

    protected static final int ELEMENT_SIZE_ORDER = 2;

    public SwapIntegerChunk(final SwapMemoryAllocator allocator, final int capacity) {
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
    public int readInt(int position) {
        assert 0 <= position && position < capacity();

        for (;;) {
            final var allocatorValue = this.header.getAllocatorValue();
            final var blockAllocator = (SwapBlockAllocator)allocatorValue.getMetadata();

            int value = UNSAFE.getInt(offset(position << ELEMENT_SIZE_ORDER));
            if (blockAllocator.active()) {
                return value;
            }

            relocateMemoryBlock();
        }
    }

    @Override
    public void writeInt(int position, int value) {
        assert 0 <= position && position < capacity();

        for (;;) {

            final var allocatorValue = this.header.getAllocatorValue();
            final var blockAllocator = (SwapBlockAllocator)allocatorValue.getMetadata();

            if (blockAllocator.active() && blockAllocator.readLock().tryLock()) {
                try {
                    if (blockAllocator.active()) {
                        allocatorValue.dirtyBlock();
                        UNSAFE.putInt(offset(position << ELEMENT_SIZE_ORDER), value);
                        return;
                    }
                } finally {
                    blockAllocator.readLock().unlock();
                }
            }

            relocateMemoryBlock();
        }
    }

}

