package com.activeviam.table.impl;

import com.activeviam.table.impl.ColumnarTable.TableFormat;
import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestColumnarTable {

	@Test
	public void testFindRows() {
		int chunkSize = 4;
		final ColumnarTable table = new ColumnarTable(new TableFormat(3, 2, 4));
		for (int i = 0; i < 2 * chunkSize + 1; i++) {
			table.append(new Record(new int[] {i,  i *2, i % 4}, new double[] {i * 1D, 1D}));
		}

		table.print();

		BitSet expected = new BitSet();
		expected.set(1);
		Assertions.assertEquals(expected, table.findRows(new int[] {1, 2, 1}));

		expected.clear();
		expected.set(2);
		expected.set(6);
		Assertions.assertEquals(expected, table.findRows(new int[] {-1, -1, 2}));
	}

}
