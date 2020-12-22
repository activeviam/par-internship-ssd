package com.activeviam.table.impl;

import com.activeviam.chunk.OnFileAllocator;
import com.activeviam.reference.MemoryAllocatorOnFile;
import com.activeviam.table.impl.ColumnarTable.TableFormat;
import java.nio.file.Path;
import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestFileColumnarTable {

  @Test
  public void testFindRows(@TempDir Path tempDir) {

    final var allocator = new MemoryAllocatorOnFile(tempDir);
    final var chunkAllocator = new OnFileAllocator(allocator);

    System.out.println("Running test with temp dir " + tempDir);

    int chunkSize = 4;
    final ColumnarTable table = new ColumnarTable(new TableFormat(3, 2, 4), chunkAllocator);
    for (int i = 0; i < 2 * chunkSize + 1; i++) {
      table.append(new Record(new int[] {i, i * 2, i % 4}, new double[] {i * 1D, 1D}));
    }

    table.print();

    BitSet expected = new BitSet();
    expected.set(1);
    Assertions.assertEquals(expected, table.findRows(new int[] {1, 2, 1}));

    expected.clear();
    expected.set(2);
    expected.set(6);
    Assertions.assertEquals(expected, table.findRows(new int[] {-1, -1, 2}));

    File f = new File(tempDir);

    pathnames = f.list();

    // For each pathname in the pathnames array
    for (String pathname : pathnames) {
      // Print the names of files and directories
      System.out.println(pathname);
    }
  }
}
