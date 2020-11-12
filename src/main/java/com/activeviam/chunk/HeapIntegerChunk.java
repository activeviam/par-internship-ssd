/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

public class HeapIntegerChunk implements IntegerChunk {

  private int[] array;

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
}
