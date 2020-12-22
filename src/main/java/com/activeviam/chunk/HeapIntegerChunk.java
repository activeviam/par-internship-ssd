/*
 * (C) ActiveViam 2020 ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use reproduction or transfer of this material is
 * strictly prohibited
 */

package com.activeviam.chunk;

import java.util.BitSet;

public class HeapIntegerChunk implements IntegerChunk {

  private final int[] array;

  public HeapIntegerChunk(final int capacity) {
    this.array = new int[capacity];
  }

  @Override
  public int capacity() {
    return this.array.length;
  }

  @Override
  public int readInt(int position) {
    return this.array[position];
  }

  @Override
  public void writeInt(int position, int value) {
    this.array[position] = value;
  }

  @Override
  public BitSet findRows(int value, int limit) {
    BitSet result = null;
    final int[] array = this.array;
    for (int i = 0; i < limit; i++) {
      if (array[i] == value) {
        if (result == null) {
          result = new BitSet();
        }
        result.set(i);
      }
    }
    return result;
  }
}
