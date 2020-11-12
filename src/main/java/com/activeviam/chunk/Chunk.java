/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

public interface Chunk<K> {

  /** Returns the (fixed) capacity of the chunk (number of elements it can store). */
  int capacity();

  /**
   * Read an int primitive value from the array.
   *
   * <p>An exception is thrown if the value is not of the right type.
   *
   * @param position 0-based index in an array
   */
  int readInt(int position);

  /**
   * Read a double primitive value from the array.
   *
   * <p>An exception is thrown if the value is not of the right type.
   *
   * @param position 0-based index in an array
   */
  double readDouble(int position);

  /**
   * Write an int primitive value in the array.
   *
   * @param position 0-based index in an array
   */
  void writeInt(int position, int value);

  /**
   * Write a double primitive value in the array.
   *
   * @param position 0-based index in an array
   */
  void writeDouble(int position, double value);
}
