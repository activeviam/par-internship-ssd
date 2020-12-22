package com.activeviam.table;

import java.util.BitSet;

/**
 * A table contains {@link IRecord}.
 *
 * @author ActiveViam
 */
public interface ITable {

  /**
   * Fetches the record stored at the given row.
   *
   * @param row The row
   * @return the record
   */
  IRecord getRecord(int row);

  /**
   * Reads the attribute of a record without materializing it like {@link #getRecord(int)}.
   *
   * @param row The row of the record to read
   * @param column The index of the attribute to read
   * @return the value of the attribute
   */
  int readInt(int row, int column);

  /**
   * Reads the value of a record without materializing it like {@link #getRecord(int)}.
   *
   * @param row The row of the record to read
   * @param column The index of the value to read
   * @return the value of the {@link IRecord#getValues() value}
   */
  double readDouble(int row, int column);

  /**
   * Returns the size of the table.
   *
   * @return the number of records in the table
   */
  int size();

  /**
   * Finds the rows whose attributes match the given predicate.
   *
   * <p>The size of the predicate must be equals to the number of attributes.
   *
   * <p>A negative value means no condition on the attribute.
   *
   * @param predicate
   * @return the rows matching the given predicate
   */
  BitSet findRows(int[] predicate);
}
