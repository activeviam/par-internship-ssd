package com.activeviam.table.impl;

import com.activeviam.chunk.DoubleChunk;
import com.activeviam.chunk.HeapDoubleChunk;
import com.activeviam.chunk.HeapIntegerChunk;
import com.activeviam.chunk.IntegerChunk;
import com.activeviam.table.IChunkSet;
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
	protected final IntegerChunk[] attributes;

	/** The values of the value columns */
	protected final DoubleChunk[] values;

	/**
	 * Constructor
	 *
	 * @param attributes Number of attributes
	 * @param values Number of values
	 * @param chunkSize Size of a chunk
	 */
	public ChunkSet(final int attributes, final int values, final int chunkSize) {
		this.attributes = new IntegerChunk[attributes];
		for (int i = 0; i < attributes; i++) {
			this.attributes[i] = new HeapIntegerChunk(chunkSize);
		}
		this.values = new DoubleChunk[values];
		for (int i = 0; i < values; i++) {
			this.values[i] = new HeapDoubleChunk(chunkSize);
		}
		this.chunkSize = chunkSize;

	}

	@Override
	public int readInt(final int row, final int column) {
		return attributes[column].readInt(row);
	}

	@Override
	public double readDouble(final int row, final int column) {
		return values[column].readDouble(row);
	}

	@Override
	public void writeInt(final int row, final int column, final int value) {
		this.attributes[column].writeInt(row, value);
	}

	@Override
	public void writeDouble(final int row, final int column, final double value) {
		this.values[column].writeDouble(row, value);
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

			final IntegerChunk column = attributes[p];
			final BitSet partialResult = column.findRows(value, limit);
			if (partialResult != null) {
				if (result == null) {
					result = partialResult;
				} else {
					result.and(partialResult);
				}
				if (result.isEmpty()) {
					return result;
				}

			} else {
				return new BitSet();
			}
		}

		if (null == result) {
			result = new BitSet(limit);
			result.flip(0, limit);
		}
		return result;
	}

}
