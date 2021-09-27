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

import java.util.stream.IntStream;

public interface SpecTestDoubleChunk {

  DoubleChunk createChunk(final int capacity);
  void checkGcCounter(long count);

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

    checkGcCounter(4);
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
