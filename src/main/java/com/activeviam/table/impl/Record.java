package com.activeviam.table.impl;

import com.activeviam.table.IRecord;
import java.util.Arrays;

/**
 * Basic implementation of {@link IRecord}.
 *
 * @author ActiveViam
 */
public class Record implements IRecord {

  /** The values of the attribute columns */
  protected final int[] attributes;

  /** The values of the value columns */
  protected final double[] values;

  /**
   * Constructor
   *
   * @param attributes The values of the attribute columns
   * @param values The values of the value columns
   */
  public Record(final int[] attributes, final double[] values) {
    this.attributes = attributes;
    this.values = values;
  }

  @Override
  public int[] getAttributes() {
    return attributes;
  }

  @Override
  public double[] getValues() {
    return values;
  }

  @Override
  public int readInt(final int attributeIndex) {
    return attributes[attributeIndex];
  }

  @Override
  public double readDouble(final int valueIndex) {
    return values[valueIndex];
  }

  @Override
  public IRecord clone() {
    return new Record(
        Arrays.copyOf(attributes, attributes.length), Arrays.copyOf(values, values.length));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(attributes);
    result = prime * result + Arrays.hashCode(values);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Record other = (Record) obj;
    if (!Arrays.equals(attributes, other.attributes) || !Arrays.equals(values, other.values)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Record [attributes="
        + Arrays.toString(attributes)
        + ", values="
        + Arrays.toString(values)
        + "]";
  }
}
