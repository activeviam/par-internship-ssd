/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

public interface IntegerChunk extends Chunk<Integer> {

  @Override
  default double readDouble(int position) {
    return readInt(position);
  }

  @Override
  default void writeDouble(int position, double value) {
    throw new UnsupportedOperationException("Cannot write double into ints");
  }
}
