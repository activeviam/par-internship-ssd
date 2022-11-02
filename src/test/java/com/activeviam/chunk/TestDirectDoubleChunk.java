/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.allocator.MemoryAllocator;
import com.activeviam.allocator.UnsafeNativeMemoryAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class TestDirectDoubleChunk implements SpecTestDoubleChunk {

	private MemoryAllocator allocator;

	@BeforeEach
	void createAllocator() {
		this.allocator = new UnsafeNativeMemoryAllocator();
	}

	@AfterEach
	void cleanAllocator() {
		this.allocator = null;
	}

	@Override
	public DoubleChunk createChunk(int capacity) {
		return new DirectDoubleChunk(this.allocator, capacity);
	}
}
