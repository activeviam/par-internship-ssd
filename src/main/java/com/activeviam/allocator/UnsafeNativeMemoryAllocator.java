/*
 * (C) ActiveViam 2015
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.allocator;

import com.activeviam.UnsafeUtil;

/**
 * A memory allocator that allocates everything via Unsafe.
 *
 * @author ActiveViam
 */
public class UnsafeNativeMemoryAllocator implements MemoryAllocator {

	@Override
	public long allocateMemory(final long bytes) {
		try {
			PlatformOperations.reserveDirectMemory((int) bytes);
			return UnsafeUtil.allocateMemory(bytes);
		} catch (OutOfMemoryError e) {
			PlatformOperations.unreserveDirectMemory((int) bytes);
			throw e;
		}
	}

	@Override
	public void freeMemory(final long address, final long bytes) {
		PlatformOperations.unreserveDirectMemory((int) bytes);
		UnsafeUtil.freeMemory(address);
	}

}
