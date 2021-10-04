/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.chunk;

import static com.activeviam.IMemoryAllocator.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public interface SpecTestIntegerChunk {

  IntegerChunk createChunk(final int capacity);

  @Test
  default void testReadWrite() {
    final var chunk = createChunk(8);
    chunk.writeInt(3, 420);
    assertThat(chunk.readInt(3)).isEqualTo(420);
  }

  @Test
  default void testFullWrite() {
    final var values =
        IntStream.range(0, Math.toIntExact(PAGE_SIZE))
            .map(i -> 3 * i + 1)
            .toArray();
    final var chunk = createChunk(values.length);
    for (int position = 0; position < values.length; position += 1) {
      chunk.writeInt(position, values[position]);
    }

    // And now read
    SoftAssertions.assertSoftly(
        assertions -> {
          for (int position = 0; position < values.length; position += 1) {
            assertions
                .assertThat(chunk.readInt(position))
                .as("Chunk[%d]", position)
                .isEqualTo(values[position]);
          }
        });
  }
}
