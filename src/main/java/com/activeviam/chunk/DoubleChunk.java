/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

public interface DoubleChunk extends Chunk<Double> {

  @Override
  default int readInt(int position) {
    throw new UnsupportedOperationException("Cannot read double as int.");
  }

  @Override
  default void writeInt(int position, int value) {
    writeDouble(position, value);
  }
}