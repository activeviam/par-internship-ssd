package com.activeviam.table;

import java.util.BitSet;

/**
 * A chunk set represents a set of chunks that correspond to the columns of a {@link ITable store}.
 *
 * @author ActiveViam
 */
public interface IChunkSet {

  /**
   * Returns the value of an attribute column at a given row.
   *
   * @param row The row
   * @param column The attribute column
   * @return the value
   */
  int readInt(int row, int column);

  /**
   * Returns the value of a value column at a given row.
   *
   * @param row The row
   * @param column The value column
   * @return the value
   */
  double readDouble(int row, int column);

  /**
   * Sets the value of an attribute column at a given row.
   *
   * @param row The row
   * @param column The attribute column
   * @param value The value
   */
  void writeInt(int row, int column, int value);

  /**
   * Sets the value of a value column at a given row.
   *
   * @param row The row
   * @param column The value column
   * @param value The value
   */
  void writeDouble(int row, int column, double value);

  /**
   * Finds the rows whose attributes match the given predicate.
   *
   * <p>The size of the predicate must be equals to the number of attributes.
   *
   * <p>A negative value means no condition on the attribute.
   *
   * @param predicate
   * @param limit the rows after this limit will be ignored
   * @return the rows matching the given predicate
   */
  BitSet findRows(int[] predicate, int limit);
}
