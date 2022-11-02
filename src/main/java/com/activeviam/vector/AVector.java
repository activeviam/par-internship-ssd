/*
 * (C) ActiveViam 2014
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

/**
 * Base implementation of a vector, that doesn't support any read or write operation.
 *
 * @author ActiveViam
 */
public abstract class AVector implements IVector, IReleasableVector {

	@Override
	public Object[] toArray() {
		final Object[] res = new Object[size()];
		copyTo(res);
		return res;
	}

	@Override
	public final double[] toDoubleArray() {
		final double[] res = new double[size()];
		copyTo(res);
		return res;
	}

	@Override
	public final float[] toFloatArray() {
		final float[] res = new float[size()];
		copyTo(res);
		return res;
	}

	@Override
	public final long[] toLongArray() {
		final long[] res = new long[size()];
		copyTo(res);
		return res;
	}

	@Override
	public final int[] toIntArray() {
		final int[] res = new int[size()];
		copyTo(res);
		return res;
	}

	@Override
	public boolean isNull(final int position) {
		return read(position) == null;
	}

	//////////////////////////////////////////////////////////////////
	// Bounds checking functions.
	// One should always use those to check bounds, so that at we
	// can easily decide to add an option to drop bounds checking.
	/**
	 * Checks the given index is valid for this vector.
	 *
	 * @param i the index to check
	 * @return the index
	 * @throws IndexOutOfBoundsException if the index is not valid
	 */
	protected final int checkIndex(int i) throws IndexOutOfBoundsException {
		if (i < 0 || i >= size()) {
			throw new IndexOutOfBoundsException("Cannot access index " + i + " in a vector of size " + size());
		}
		return i;
	}

	/**
	 * Checks that the range {@code [i, i + nb]} is valid for this vector.
	 *
	 * @param i the index
	 * @param nb the size of the range
	 * @return the index
	 * @throws IndexOutOfBoundsException if the range is not valid
	 */
	public final int checkIndex(final int i, final int nb) throws IndexOutOfBoundsException {
		if (i < 0 || nb > (size() - i)) {
			throw new IndexOutOfBoundsException(
					"Cannot access between index " + i + " and " + (i + nb) + " in a vector of size " + size());
		}
		return i;
	}

	@Override
	public Object read(final int index) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public double readDouble(final int index) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public float readFloat(final int index) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public long readLong(final int index) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public int readInt(final int index) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public boolean readBoolean(final int position) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void writeDouble(final int index, final double value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void writeFloat(final int index, final float value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void writeLong(final int index, final long value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void writeInt(final int index, final int value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void writeBoolean(final int position, final boolean value) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void addDouble(final int position, final double addedValue) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void addFloat(final int position, final float addedValue) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void addLong(final int position, final long addedValue) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void addInt(final int position, final int addedValue) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}


	@Override
	public void copyTo(final Object[] dst) {
		final int lgth = dst.length;
		if (lgth > size()) {
			throw new IndexOutOfBoundsException(
					"Tried to read a chunk of size " + lgth + " from a vector of size " + size());
		}
		for (int i = 0; i < lgth; ++i) {
			dst[i] = read(i);
		}
	}

	@Override
	public void copyTo(final double[] dst) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void copyTo(final float[] dst) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void copyTo(final long[] dst) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void copyTo(final int[] dst) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void copyFrom(final Object[] src) {
		final int lgth = src.length;
		if (lgth > size()) {
			throw new IndexOutOfBoundsException(
					"Tried to write an array of size " + lgth + " to a vector of size " + size());
		}
		for (int i = 0; i < lgth; ++i) {
			write(i, src[i]);
		}
	}

	@Override
	public void copyFrom(final double[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void copyFrom(final float[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void copyFrom(final long[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void copyFrom(final int[] src) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void scale(final double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void scale(final float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void scale(final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void scale(final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void divide(final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void divide(final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void translate(final double v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void translate(final float v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void translate(final long v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public void translate(final int v) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public double sumDouble() {
		double sum = 0d;
		for (int i = 0; i < size(); i++) {
			sum += readDouble(i);
		}
		return sum;
	}

	@Override
	public float sumFloat() {
		float sum = 0f;
		for (int i = 0; i < size(); i++) {
			sum += readFloat(i);
		}
		return sum;
	}

	@Override
	public long sumLong() {
		long sum = 0L;
		for (int i = 0; i < size(); i++) {
			sum += readLong(i);
		}
		return sum;
	}

	@Override
	public int sumInt() {
		int sum = 0;
		for (int i = 0; i < size(); i++) {
			sum += readInt(i);
		}
		return sum;
	}

	@Override
	public double quantileDouble(final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public float quantileFloat(final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public long quantileLong(final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public int quantileInt(final double r) {
		throw new UnsupportedOperationException(
				"This method is not implemented for the vector " + getClass().getSimpleName());
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof IVector)) {
			return false;
		}
		final IVector other = (IVector) obj;
		if (other.size() != size()) {
			return false;
		}
		if (other.getComponentType() != getComponentType()) {
			return false;
		}

		return other.getClass() == this.getClass();
	}

	@Override
	public abstract int hashCode();

	@Override
	public String toString() {
		return toString(1);
	}

	/**
	 * Returns a String representation of this {@link AVector vector}, printing a maximum of maxPrintNumber
	 * components.
	 *
	 * @param maxPrintNumber the maximum number of components to print
	 * @return a {@link String} representation of this vector
	 */
	public String toString(final int maxPrintNumber) {
		final Object[] toPrint;
		final boolean cut;
		final String header = getComponentType().name() + "Vector[" + size() + "]";
		if (maxPrintNumber <= 0) {
			return header;
		}
		if (size() > maxPrintNumber) {
			toPrint = new Object[maxPrintNumber];
			cut = true;
		} else {
			toPrint = new Object[size()];
			cut = false;
		}
		copyTo(toPrint);
		return header + "{" + join(", ", toPrint) + (cut ? ", ..." : "") + "}";
	}

	/**
	 * Joins an array of objects as a String using the given separator. The toString representation of the object
	 * is used.
	 *
	 * @param separator The separator to join the array.
	 * @param args The array of object to join.
	 * @param <T> The type of the array components.
	 * @return A {@link CharSequence} representation of the object.
	 */
	@SafeVarargs
	public static <T> CharSequence join(final String separator, final T... args) {
		final int argsCnt;
		if (args == null || (argsCnt = args.length) == 0) {
			return "";
		}

		final StringBuilder sb = new StringBuilder();
		sb.append(args[0]);
		for (int i = 1; i < argsCnt; ++i) {
			sb.append(separator).append(args[i]);
		}
		return sb;
	}



}
