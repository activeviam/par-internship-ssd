package com.activeviam.table.impl;

import com.activeviam.table.IChunkSet;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @author ActiveViam
 */
public class ChunkSet implements IChunkSet {

	/** The value that identifies an empty spot */
	protected static final int EMPTY_VALUE = -1;

	/** The size of a chunk */
	protected final int chunkSize;

	/** The values of the attribute columns */
	protected final int[][] attributes;

	/** The values of the value columns */
	protected final double[][] values;

	/**
	 * Constructor
	 *
	 * @param attributes Number of attributes
	 * @param values Number of values
	 * @param chunkSize Size of a chunk
	 */
	public ChunkSet(final int attributes, final int values, final int chunkSize) {
		this.attributes = new int[attributes][chunkSize];
		this.values = new double[values][chunkSize];
		this.chunkSize = chunkSize;

		for (int i = 0; i < attributes; ++i) {
			Arrays.fill(this.attributes[i], EMPTY_VALUE);
		}
	}

	@Override
	public int readInt(final int row, final int column) {
		return attributes[column][row];
	}

	@Override
	public double readDouble(final int row, final int column) {
		return values[column][row];
	}

	@Override
	public void writeInt(final int row, final int column, final int value) {
		this.attributes[column][row] = value;
	}

	@Override
	public void writeDouble(final int row, final int column, final double value) {
		this.values[column][row] = value;
	}

	@Override
	public BitSet findRows(int[] predicate, int limit) {
		BitSet result = null;

		for (int p = 0; p < predicate.length; p++) {
			final int value = predicate[p];
			if (value < 0) {
				// no condition
				continue;
			}

			final BitSet partialResult = new BitSet(limit);
			final int[] column = attributes[p];
			for (int i = 0; i < limit; i++) {
				if (column[i] == value) {
					partialResult.set(i);
				}
			}

			if (result == null) {
				result = partialResult;
			} else {
				result.and(partialResult);
			}
		}

		if (null == result) {
			result = new BitSet(limit);
			result.flip(0, limit);
		}
		return result;
	}

}
