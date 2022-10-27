/*
 * (C) ActiveViam 2018
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.allocator;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to track the direct memory used.
 * <p>
 * This class mimics java.nio.Bits and java.nio.VM
 * </p>
 *
 * @author ActiveViam
 */
public class DirectMemoryTracker {

	/** The logger. */
	private static final Logger LOGGER = Logger.getLogger(DirectMemoryTracker.class.getName());

	/** 1 TiB. */
	public static final long TB = 1L << 40;
	/** 1 GiB. */
	public static final long GB = 1 << 30;
	/** 1 MiB. */
	public static final long MB = 1 << 20;
	/** 1 KiB. */
	public static final long KB = 1 << 10;

	/** Maximum direct memory that can be allocated. */
	private static final long MAX_MEMORY;

	/** Amount of direct memory already allocated. */
	private static final AtomicLong RESERVED_MEMORY = new AtomicLong();
	/** Total capacity allocated. */
	private static final AtomicLong TOTAL_CAPACITY = new AtomicLong();
	/** Count the number of buffer allocated. */
	private static final AtomicLong COUNT = new AtomicLong();

	protected static final String MAX_DIRECT_MEMORY_PARAM = "-XX:MaxDirectMemorySize=";

	/**
	 * Max. number of sleeps during try-reserving with exponentially increasing delay before throwing
	 * OutOfMemoryError: 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 (total 1023 ms ~ 1 s) which means that OOME will be
	 * thrown after 1s of trying
	 */
	private static final int MAX_SLEEPS = 10;

	/**
	 * The name of the {@link BufferPoolMXBean} giving access to direct memory data.
	 */
	protected static final String DIRECT_MEMORY_BEAN_NAME = "direct";

	/**
	 * Gives access to direct memory data using a standard way, avoid reflective accesses to
	 * JavaNioAccess.BufferPool.
	 */
	protected static BufferPoolMXBean directMemoryMXBean;

	static {
		for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
			if (pool.getName().equals(DIRECT_MEMORY_BEAN_NAME)) {
				directMemoryMXBean = pool;
			} else {
				LOGGER.log(
						Level.WARNING,
						"Could not get the BufferPoolMXBean giving access to direct memory data."
								+ "MemoryMonitoring MBean will only display ActivePivot's direct memory usage");
			}
		}
		final long setMaxMemory = getOptionDirectMemorySize();
		final long mBeanMaxMemory = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
		if (setMaxMemory != -1) {
			MAX_MEMORY = setMaxMemory;
		} else if (mBeanMaxMemory != -1) {
			MAX_MEMORY = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
		} else {
			MAX_MEMORY = Runtime.getRuntime().maxMemory();
		}
	}

	private static long getOptionDirectMemorySize() {
		RuntimeMXBean runtimemxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = new ArrayList<>(runtimemxBean.getInputArguments());
		for (String s : arguments) {
			if (s.contains(MAX_DIRECT_MEMORY_PARAM)) {
				String memSize = s.toLowerCase().replace(MAX_DIRECT_MEMORY_PARAM.toLowerCase(), "").trim();

				final long multiplier; // for the byte case.
				if (memSize.contains("k")) {
					multiplier = KB;
				} else if (memSize.contains("m")) {
					multiplier = MB;
				} else if (memSize.contains("g")) {
					multiplier = GB;
				} else if (memSize.contains("t")) {
					multiplier = TB;
				} else {
					multiplier = 1;
				}
				memSize = memSize.replaceAll("[^\\d]", "");
				long retValue = Long.parseLong(memSize);
				return retValue * multiplier;
			}
		}
		return -1;
	}

	DirectMemoryTracker() {
	}

	protected static long getMaxDirectMemory() {
		return MAX_MEMORY;
	}

	/**
	 * Increase the counters following the amount of direct memory allocated.
	 *
	 * @param size capacity to be increased.
	 * @param bytes memory to be reserved.
	 */
	protected void reserveMemory(long size, int bytes) {
		if (tryReserveMemory(size, bytes)) {
			return;
		}

		System.gc();

		// A retry loop with exponential back-off delays waiting for GC to finish.
		long sleepTime = 1;
		int sleeps = 0;
		while (true) {
			if (tryReserveMemory(size, bytes)) {
				return;
			}
			if (sleeps >= MAX_SLEEPS) {
				break;
			}
			try {
				Thread.sleep(sleepTime);
				sleepTime <<= 1;
				sleeps++;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		throw new OutOfMemoryError(
				"Cannot reserve " + size + " bytes of direct buffer memory (allocated: " + RESERVED_MEMORY.get()
						+ ", limit: " + MAX_MEMORY + ")");
	}

	/**
	 * Attempt to increase the amount of direct memory allocated.
	 *
	 * @param bytes capacity to be increased.
	 * @param size memory to be reserved.
	 * @return if memory can be allocated
	 */
	private static boolean tryReserveMemory(long size, long bytes) {
		long totalCap;
		while (bytes <= MAX_MEMORY - (totalCap = TOTAL_CAPACITY.get())) {
			if (TOTAL_CAPACITY.compareAndSet(totalCap, totalCap + bytes)) {
				RESERVED_MEMORY.addAndGet(size);
				COUNT.incrementAndGet();
				return true;
			}
		}

		return false;
	}

	/**
	 * Decrease the counters following the amount of direct memory allocated.
	 *
	 * @param size capacity to be reduced.
	 * @param bytes memory to be unreserved.
	 */
	protected void unreserveMemory(long size, int bytes) {
		long cnt = COUNT.decrementAndGet();
		long reservedMem = RESERVED_MEMORY.addAndGet(-size);
		long totalCap = TOTAL_CAPACITY.addAndGet(-bytes);
		assert cnt >= 0 && reservedMem >= 0 && totalCap >= 0;
	}

	/** Retrieves the total reserved memory. */
	public static long getTotalReservedMemory() {
		if (directMemoryMXBean == null) {
			return RESERVED_MEMORY.get();
		}
		return directMemoryMXBean.getMemoryUsed() + RESERVED_MEMORY.get();
	}

	/** Retrieves the total direct capacity. */
	public static long getTotalCapacity() {
		if (directMemoryMXBean == null) {
			return TOTAL_CAPACITY.get();
		}
		return directMemoryMXBean.getTotalCapacity() + TOTAL_CAPACITY.get();
	}

	/** Retrieves an estimate of the total number of direct buffers. */
	public static long getTotalCount() {
		if (directMemoryMXBean == null) {
			return COUNT.get();
		}
		return directMemoryMXBean.getCount() + COUNT.get();
	}

}
