/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.UnsafeUtil;
import com.activeviam.allocator.AllocationType;
import com.activeviam.allocator.MemoryAllocator;

/**
 * A block that can store vectors using direct memory allocated an {@link MemoryAllocator}.
 *
 * @author ActiveViam
 */
public abstract class ADirectVectorBlock extends AbstractDirectChunk implements IBlock {

	protected static final sun.misc.Unsafe UNSAFE = UnsafeUtil.getUnsafe();
	private final Types type;

	/**
	 * Constructor.
	 *
	 */
	protected ADirectVectorBlock(final MemoryAllocator allocator, final int capacity, Types type) {
		super(allocator, capacity, getBlockSizeInBytes(type, capacity));
		this.type = type;
	}

	/**
	 * Returns the size of the block in bytes for the given allocation settings.
	 *
	 * @param type     the content type
	 * @param capacity the block capacity in terms of number of components
	 * @return the block size, in bytes
	 */
	static int getBlockSizeInBytes(final Types type, int capacity) {
		switch (type) {
			case DOUBLE:
				return capacity << 3;
			case INTEGER:
				return capacity << 2;
			default:
				throw new IllegalStateException(
						"Unexpected type: " + type.name());
		}
	}

	/**
	 * Gets the direct memory address.
	 *
	 * @return the direct memory address
	 */
	public long getAddress() {
		return this.ptr;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Unused methods from ADirectChunk
	/////////////////////////////////////////////////////////////////////////////////////

	@Override
	public Void read(final int position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isNull(final int position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Types getComponentType() {
		return this.type;
	}

	@Override
	public void transfer(final int position, final double[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void transfer(final int position, final float[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void transfer(final int position, final long[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void transfer(final int position, final int[] dest) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void write(final int position, final Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(final int position, final double[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void write(final int position, final float[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void write(final int position, final long[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void write(final int position, final int[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void fillDouble(final int position, final int lgth, final double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void fillFloat(final int position, final int lgth, final float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void fillLong(final int position, final int lgth, final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void fillInt(final int position, final int lgth, final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void scale(final int position, final int lgth, final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void divide(final int position, final int lgth, final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void divide(final int position, final int lgth, final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void translate(final int position, final int lgth, final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public int readInt(final int position) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public long readLong(final int position) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public double readDouble(final int position) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public float readFloat(final int position) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void writeDouble(final int position, final double value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void writeFloat(final int position, final float value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void writeLong(final int position, final long value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public void writeInt(final int position, final int value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public int quantileInt(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public double quantileDouble(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public float quantileFloat(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	@Override
	public long quantileLong(final int position, final int lgth, final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the block " + getClass().getName());
	}

	/**
	 * Convenient method for quantile using the Nearest Rank definition of quantile.
	 *
	 * @param lgth the length of the vector
	 * @param r the radix, as a number between 0 and 1
	 * @return the nearest rank
	 */
	protected int nearestRank(final int lgth, final double r) {
		return (int) Math.ceil(lgth * r);
	}

	@Override
	public AllocationType getAllocation() {
		return AllocationType.DIRECT;
	}

}
