package com.activeviam.table;

/**
 * Interface to edit an {@link ITable}.
 *
 * @author ActiveViam
 */
public interface IWritableTable extends ITable {

  /**
   * Adds a record to the table.
   *
   * @param record The record to add
   * @return the row at which this record was added
   */
  int append(IRecord record);

  /**
   * This method can be called before {@link #append(IRecord) adding} several records to allocate in
   * one operation the memory to store the records.
   *
   * @param capacity The target capacity
   * @return the new capacity
   */
  int ensureCapacity(int capacity);

  interface ITableWriter {

    /**
     * Sets the value of an attribute column.
     *
     * @param column The attribute column
     * @param value The value
     */
    void writeInt(int column, int value);

    /**
     * Sets the value of a value column.
     *
     * @param column The value column
     * @param value The value
     */
    void writeDouble(int column, double value);

    /**
     * Sets the row on which to write.
     *
     * @param row The row
     */
    void setRow(int row);
  }
}
