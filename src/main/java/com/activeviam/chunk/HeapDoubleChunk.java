/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

public class HeapDoubleChunk implements DoubleChunk {

  private double[] array;

  public HeapDoubleChunk(final int capacity) {
    this.array = new double[capacity];
  }

  @Override
  public int capacity() {
    return this.array.length;
  }

  @Override
  public double readDouble(int position) {
    return this.array[position];
  }

  @Override
  public void writeDouble(int position, double value) {
    this.array[position] = value;
  }

  @Override
  public Runnable destroy() {
    return EMPTY_DESTORYER;
  }
}
