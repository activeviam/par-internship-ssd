/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.platform;

import com.sun.jna.Library;

/** @author ActiveViam */
interface HugetlbfsLib extends Library {

  /**
   * <a href="http://linux.die.net/man/3/getpagesizes">http://linux.die.net/man/3/getpagesizes</a>
   *
   * <p>See #include <hugetlbfs.h>
   *
   * <p>The getpagesizes() function returns either the number of system supported page sizes or the
   * sizes themselves. If pagesizes is NULL and n_elem is 0, then the number of pages the system
   * supports is returned. Otherwise, pagesizes is filled with at most n_elem page sizes.
   *
   * <pre>
   * The getpagesizes() function will fail if:
   *
   *  ERRNO
   *  n_elem is less than zero or n_elem is greater than zero and pagesizes is NULL.
   * </pre>
   *
   * @param pagesizes supported page sizes
   * @param nelem size of the array
   * @return On success, either the number of page sizes supported by the system or the number of
   *     page sizes stored in pagesizes is returned. On failure, -1 is returned and errno is set
   *     appropriately.
   */
  // int getpagesizes(long[] pagesize, int nelem);
  int getpagesizes(long[] pagesizes, int nelem);
}
