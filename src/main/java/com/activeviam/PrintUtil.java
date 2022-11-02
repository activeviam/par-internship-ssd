/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam;

public class PrintUtil {

  /** 1 TiB */
  public static final long TB = 1L << 40;
  /** 1 GiB */
  public static final long GB = 1 << 30;
  /** 1 MiB */
  public static final long MB = 1 << 20;
  /** 1 KiB */
  public static final long KB = 1 << 10;

  /**
   * Format a number of bytes into a readable data size.
   *
   * @param byteCount The amount of bytes to print.
   * @return data size as text
   */
  public static String printDataSize(final long byteCount) {
    final long tb = byteCount / TB;
    final long gb = (byteCount % TB) / GB;
    final long mb = (byteCount % GB) / MB;
    final long kb = (byteCount % MB) / KB;
    final long remaining = (byteCount % KB);
    if (tb != 0) {
      return printNonZero(tb, "TiB") + printNonZero(gb, "GiB") + "(" + byteCount + ")";
    } else if (gb != 0) {
      return printNonZero(gb, "GiB") + printNonZero(mb, "MiB") + "(" + byteCount + ")";
    } else if (mb != 0) {
      return printNonZero(mb, "MiB") + printNonZero(kb, "KiB") + "(" + byteCount + ")";
    } else {
      return printNonZero(kb, "KiB") + printNonZero(remaining, "bytes") + "(" + byteCount + ")";
    }
  }

  /**
   * Print if non zero.
   *
   * @param value The value to print.
   * @param suffix The suffix to add to the value if non-zero
   * @return The printed value.
   */
  private static String printNonZero(final long value, final String suffix) {
    return value != 0 ? value + " " + suffix + " " : "";
  }
}
