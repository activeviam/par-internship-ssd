/*
 * (C) ActiveViam 2014
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.iterator.IPrimitiveIterator;
import java.util.EnumMap;
import java.util.NoSuchElementException;

/**
 * A vector of size 0.
 *
 * @author ActiveViam
 */
public class EmptyVector implements ITransientVector, Cloneable, IPrimitiveIterator {

	/**
	 * The map component class -> singleton {@link EmptyVector} instance.
	 */
	public static final EnumMap<Types, EmptyVector> EMPTY_VECTORS = new EnumMap<>(Types.class);

	static {
		// Init the map with usual types
		emptyVector(Types.DOUBLE);
		emptyVector(Types.INTEGER);
	}

	/** The underlying component type of this vector. */
	public final Types componentTypes;

	/**
	 * Creates the singleton empty vector, with the given type.
	 *
	 * @param componentTypes the component type of the empty vector
	 * @return the singleton empty vector
	 */
	public static ITransientVector emptyVector(final Types componentTypes) {
		EmptyVector result = EMPTY_VECTORS.get(componentTypes);
		if (result == null) {
			EMPTY_VECTORS.putIfAbsent(componentTypes, new EmptyVector(componentTypes));
			result = EMPTY_VECTORS.get(componentTypes);
		}
		return result;
	}

	private EmptyVector(final Types componentTypes) {
		this.componentTypes = componentTypes;
	}

	@Override
	public Types getComponentType() {
		return this.componentTypes;
	}

	@Override
	public ITransientVector sort() {
		return this;
	}

	@Override
	public void acquireReference() {
		/* Do nothing for this kind of vector */
	}

	@Override
	public void releaseReference() {
		/* Do nothing for this kind of vector */
	}

	@Override
	public ITransientVector subVector(final int start, final int length) {
		if (start > 0 || length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
		return this;
	}

	@Override
	public IVector cloneOnHeap() {
		return this;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public int hashCode() {
		return 1;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IVector)) {
			return false;
		}
		final IVector v = (IVector) obj;
		return v.size() == 0 && v.getComponentType() == componentTypes;
	}

	@Override
	public void plus(final IVector vector) {
		if (vector.size() > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void plusPositiveValues(final IVector vector) {
		if (vector.size() > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void plusNegativeValues(final IVector vector) {
		if (vector.size() > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void minus(final IVector vector) {
		if (vector.size() > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void minusPositiveValues(final IVector vector) {
		if (vector.size() > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void minusNegativeValues(final IVector vector) {
		if (vector.size() > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public IPrimitiveIterator topK(final int k) {
		if (k > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
		return this;
	}

	@Override
	public IPrimitiveIterator bottomK(final int k) {
		if (k > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
		return this;
	}

	@Override
	public int[] topKIndices(final int k) {
		if (k > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}

		return new int[0];
	}

	@Override
	public int[] bottomKIndices(final int k) {
		if (k > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}

		return new int[0];
	}

	@Override
	public Object[] toArray() {
		return new Object[0];
	}

	@Override
	public Object read(final int index) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public double readDouble(final int index) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public float readFloat(final int index) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public long readLong(final int index) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public int readInt(final int index) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public boolean readBoolean(final int position) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void write(final int index, final Object dst) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void writeDouble(final int index, final double value) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void writeFloat(final int index, final float value) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void writeLong(final int index, final long value) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void writeInt(final int index, final int value) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void writeBoolean(final int position, final boolean value) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void addDouble(final int position, final double addedValue) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void addFloat(final int position, final float addedValue) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void addLong(final int position, final long addedValue) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void addInt(final int position, final int addedValue) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void copyTo(final Object[] dst) {
		if (dst.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyTo(final double[] dst) {
		if (dst.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyTo(final float[] dst) {
		if (dst.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyTo(final long[] dst) {
		if (dst.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyTo(final int[] dst) {
		if (dst.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyFrom(final IVector vector) {
		if (vector.size() > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyFrom(final Object[] src) {
		if (src.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyFrom(final double[] src) {
		if (src.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyFrom(final float[] src) {
		if (src.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyFrom(final long[] src) {
		if (src.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public void copyFrom(final int[] src) {
		if (src.length > 0) {
			throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
		}
	}

	@Override
	public IVector clone() {
		return this;
	}

	@Override
	public double[] toDoubleArray() {
		return new double[0];
	}

	@Override
	public float[] toFloatArray() {
		return new float[0];
	}

	@Override
	public long[] toLongArray() {
		return new long[0];
	}

	@Override
	public int[] toIntArray() {
		return new int[0];
	}

	@Override
	public AllocationType getAllocation() {
		return AllocationType.ON_HEAP;
	}

	@Override
	public boolean isNull(final int position) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public void fillDouble(double value) {
		// Nothing to do
	}

	@Override
	public void fillFloat(float value) {
		// Nothing to do
	}

	@Override
	public void fillLong(long value) {
		// Nothing to do
	}

	@Override
	public void fillInt(int value) {
		// Nothing to do
	}

	@Override
	public void scale(final double v) {
		// Nothing to do
	}

	@Override
	public void scale(final float v) {
		// Nothing to do
	}

	@Override
	public void scale(final long v) {
		// Nothing to do
	}

	@Override
	public void scale(final int v) {
		// Nothing to do
	}

	@Override
	public void divide(final long v) {
		// Nothing to do
	}

	@Override
	public void divide(final int v) {
		// Nothing to do
	}

	@Override
	public void translate(final double v) {
		// Nothing to do
	}

	@Override
	public void translate(final float v) {
		// Nothing to do
	}

	@Override
	public void translate(final long v) {
		// Nothing to do
	}

	@Override
	public void translate(final int v) {
		// Nothing to do
	}

	@Override
	public void sortInPlace() {
		// Nothing to do
	}

	@Override
	public void reverse() {
		// Nothing to do
	}

	@Override
	public double average() {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public double sumDouble() {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public float sumFloat() {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public long sumLong() {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public int sumInt() {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public double variance() {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public double quantileDouble(final double r) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public float quantileFloat(final double r) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public long quantileLong(final double r) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public int quantileInt(final double r) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public int quantileIndex(final double r) {
		throw new IndexOutOfBoundsException("Tried to access the content of an empty vector.");
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public double nextDouble() {
		throw new NoSuchElementException("Cannot access next double in the vector");
	}

	@Override
	public float nextFloat() {
		throw new NoSuchElementException("Cannot access next float in the vector");
	}

	@Override
	public long nextLong() {
		throw new NoSuchElementException("Cannot access next long in the vector");
	}

	@Override
	public int nextInt() {
		throw new NoSuchElementException("Cannot access next int in the vector");
	}

	@Override
	public ITransientVector getUnderlyingArray() {
		return this;
	}

}
