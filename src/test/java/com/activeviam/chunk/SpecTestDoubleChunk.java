/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.chunk;

import static com.activeviam.MemoryAllocator.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public interface SpecTestDoubleChunk {

  DoubleChunk createChunk(final int capacity);
  void checkGcCounter(long count);
  void dumpAllocatorState();

  @Test
  default void testReadWrite() {
    final var chunk = createChunk(8);
    chunk.writeDouble(7, -58d);
    assertThat(chunk.readDouble(7)).isEqualTo(-58d);
    checkGcCounter(0);
  }

  @Test
  default void testCreateTwoChunks() {

    final var chunk1 = createChunk(2 * (int)PAGE_SIZE);
    chunk1.writeDouble(1, -58d);

    final var chunk2 = createChunk((int)PAGE_SIZE);
    chunk1.writeDouble(2, -58d);
    chunk1.writeDouble(3, -58d);
    assertThat(chunk1.readDouble(1)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(2)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(3)).isEqualTo(-58d);

    chunk2.writeDouble(3, -57d);
    assertThat(chunk1.readDouble(3)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(2)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(1)).isEqualTo(-58d);

    assertThat(chunk2.readDouble(3)).isEqualTo(-57d);

    for (int i = 0; i < chunk2.capacity(); i++) {
      chunk1.writeDouble(i, 1d);
      chunk2.writeDouble(i, 2d);
      assertThat(chunk1.readDouble(i)).isEqualTo(1d);
      assertThat(chunk2.readDouble(i)).isEqualTo(2d);
    }

    checkGcCounter(5 + 4 * chunk2.capacity());
  }

  @Test
  default void testNoGC() {

    final var chunk1 = createChunk(2 * (int)PAGE_SIZE);
    chunk1.writeDouble(1, -58d);

    final var chunk2 = createChunk(2 * (int)PAGE_SIZE);
    chunk1.writeDouble(2, -58d);
    chunk1.writeDouble(3, -58d);
    assertThat(chunk1.readDouble(1)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(2)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(3)).isEqualTo(-58d);

    chunk2.writeDouble(3, -57d);
    assertThat(chunk1.readDouble(3)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(2)).isEqualTo(-58d);
    assertThat(chunk1.readDouble(1)).isEqualTo(-58d);

    checkGcCounter(0);
  }

  @Test
  default void testArbitration() {

    final int numThreads = 9;
    final List<Thread> threads = new LinkedList<>();
    final Lock lock = new ReentrantLock();

    for (int threadId = 0; threadId < numThreads; threadId++) {

      final int id = threadId + 1;

      final Thread thread = new Thread(() -> {

        final var chunk = createChunk(1 << (id + 9));

        for (int i = 0; i < chunk.capacity(); i++) {
          chunk.writeDouble(i, -5.8 * id * i);
        }

        for (int i = 0; i < chunk.capacity(); i++) {
          if (chunk.readDouble(i) != -5.8 * id * i) {
            System.out.println("[" + i + "] expected = " + (-5.8 * id * i) + ", actual = " + chunk.readDouble(i));
            //dumpAllocatorState();
          }
        }

      }, "thread " + id);

      threads.add(thread);
      thread.start();
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    checkGcCounter(-1);
  }

  @Test
  default void testFullWrite() {
    final var values =
        IntStream.range(0, Math.toIntExact(PAGE_SIZE))
            .mapToDouble(i -> 3 * i + 1 / 7d)
            .toArray();
    final var chunk = createChunk(values.length);
    for (int position = 0; position < values.length; position += 1) {
      chunk.writeDouble(position, values[position]);
    }

    // And now read
    SoftAssertions.assertSoftly(
        assertions -> {
          for (int position = 0; position < values.length; position += 1) {
            assertions
                .assertThat(chunk.readDouble(position))
                .as("Chunk[%d]", position)
                .isEqualTo(values[position]);
          }
        });

    checkGcCounter(0);
  }

}
