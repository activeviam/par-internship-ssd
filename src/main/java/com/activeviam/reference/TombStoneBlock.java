/*
 * (C) ActiveViam 2016-2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.reference;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.block.IBlock;
import com.activeviam.iterator.IPrimitiveIterator;

/**
 * A dead {@link IBlock block}.
 * <p>
 * All actions performed in this block throw errors.
 *
 * @author ActiveViam
 */
public class TombStoneBlock implements IBlock {

	/** The unique instance of TombStone. */
	public static final IBlock INSTANCE = new TombStoneBlock();

	/**
	 * Reduced-visibility constructor.
	 */
	private TombStoneBlock() {
	}

	@Override
	public boolean isNull(int position) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public Object read(int position) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void write(int position, Object value) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public int capacity() {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public Types getComponentType() {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void acquire() {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void release(final int v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void transfer(final int position, final double[] dest) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void transfer(final int position, final float[] dest) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void transfer(final int position, final long[] dest) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void transfer(final int position, final int[] dest) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void write(final int position, final double[] src) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void write(final int position, final float[] src) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void write(final int position, final long[] src) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void write(final int position, final int[] src) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void fillDouble(final int position, final int lgth, final double v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void fillFloat(final int position, final int lgth, final float v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void fillLong(final int position, final int lgth, final long v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void fillInt(final int position, final int lgth, final int v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void scale(final int position, final int lgth, final double v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void scale(final int position, final int lgth, final float v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void scale(final int position, final int lgth, final long v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void scale(final int position, final int lgth, final int v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void divide(final int position, final int lgth, final long v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void divide(final int position, final int lgth, final int v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void translate(final int position, final int lgth, final double v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void translate(final int position, final int lgth, final float v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void translate(final int position, final int lgth, final long v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public void translate(final int position, final int lgth, final int v) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public int hashCode(final int position, final int length) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public IPrimitiveIterator topK(final int position, final int lgth, final int k) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public IPrimitiveIterator bottomK(final int position, final int lgth, final int k) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public int[] topKIndices(final int position, final int lgth, final int k) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public int[] bottomKIndices(final int position, final int lgth, final int k) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public double quantileDouble(final int position, final int lgth, final double r) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public float quantileFloat(final int position, final int lgth, final double r) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public long quantileLong(final int position, final int lgth, final double r) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public int quantileInt(final int position, final int lgth, final double r) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public int quantileIndex(final int position, final int lgth, final double r) {
		throw new RuntimeException("The current block has been release");
	}

	@Override
	public AllocationType getAllocation() {
		// This block is made to replace direct memory
		return AllocationType.DIRECT;
	}
}
