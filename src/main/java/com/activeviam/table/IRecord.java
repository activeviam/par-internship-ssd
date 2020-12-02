package com.activeviam.table;

/**
 * A record is a row of a table.
 *
 * <p>
 *   Its fields can have numerics values ({@link #getValues()} stored as doubles. The other types of
 *   fields will be stored as ints ({@link #getAttributes()}) (we will suppose that the encoding of
 *   the values is done before storing the records).
 * </p>
 *
 * @author ActiveViam
 * @see ITable
 */
public interface IRecord {

	/**
	 * Gets the attribute values of the record.
	 *
	 * @return the attribute values
	 */
	int[] getAttributes();

	/**
	 * Gets the value values of the record.
	 *
	 * @return the value values
	 */
	double[] getValues();

	/**
	 * Gets the value of an attribute of the record.
	 *
	 * @return the value
	 */
	int readInt(int attributeIndex);

	/**
	 * Gets the value of a value of the record.
	 *
	 * @return the value
	 */
	double readDouble(int valueIndex);

	/**
	 * Clones a record.
	 *
	 * @return the clone
	 */
	IRecord clone();

}
