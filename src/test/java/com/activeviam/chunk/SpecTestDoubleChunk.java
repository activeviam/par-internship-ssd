/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import com.activeviam.MemoryAllocator;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public interface SpecTestDoubleChunk {

  DoubleChunk createChunk(final int capacity);

  @Test
  default void testReadWrite() {
    final var chunk = createChunk(8);
    chunk.writeDouble(7, -58d);
    assertThat(chunk.readDouble(7)).isEqualTo(-58d);
  }

  @Test
  default void testFullWrite() {
    final var values =
        IntStream.range(0, Math.toIntExact(MemoryAllocator.PAGE_SIZE))
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
  }
}
