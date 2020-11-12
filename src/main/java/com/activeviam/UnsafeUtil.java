package com.activeviam;

import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public final class UnsafeUtil {

  /** The Unsafe singleton. */
  private static final sun.misc.Unsafe UNSAFE = retrieveUnsafe();

  /**
   * Get the cached {@link sun.misc.Unsafe} instance.
   *
   * @return {@link sun.misc.Unsafe} instance
   */
  public static final sun.misc.Unsafe getUnsafe() {
    return UNSAFE;
  }

  /**
   * Returns a sun.misc.Unsafe. Suitable for use in a 3rd party package.
   *
   * @return a sun.misc.Unsafe
   */
  private static final sun.misc.Unsafe retrieveUnsafe() {
    try {
      return sun.misc.Unsafe.getUnsafe();
    } catch (SecurityException se) {
      try {
        return java.security.AccessController.doPrivileged(
            (PrivilegedExceptionAction<Unsafe>)
                () -> {
                  final Field f = Unsafe.class.getDeclaredField("theUnsafe");
                  f.setAccessible(true);
                  return (Unsafe) f.get(null);
                });
      } catch (java.security.PrivilegedActionException e) {
        throw new RuntimeException("Could not initialize intrinsics", e.getCause());
      }
    }
  }

  /** Private constructor to avoid instantiation. */
  private UnsafeUtil() {}

  ///////////////////////////
  // Public unsafe methods //
  ///////////////////////////

  /// peek and poke operations
  /// (compilers should optimize these to memory ops)

  // These work on object fields in the Java heap.
  // They will not work on elements of packed arrays.

  /**
   * Fetches a value from a given Java variable. More specifically, fetches a field or array element
   * within the given object <code>o</code> at the given offset, or (if <code>o</code> is null) from
   * the memory address whose numerical value is the given offset.
   *
   * <p>The results are undefined unless one of the following cases is true:
   *
   * <ul>
   *   <li>The offset was obtained from {@link #objectFieldOffset} on the {@link
   *       java.lang.reflect.Field} of some Java field and the object referred to by <code>o</code>
   *       is of a class compatible with that field's class.
   *   <li>The offset and object reference <code>o</code> (either null or non-null) were both
   *       obtained via {@link #staticFieldOffset} and {@link #staticFieldBase} (respectively) from
   *       the reflective {@link Field} representation of some Java field.
   *   <li>The object referred to by <code>o</code> is an array, and the offset is an integer of the
   *       form <code>B+N*S</code>, where <code>N</code> is a valid index into the array, and <code>
   *       B</code> and <code>S</code> are the values obtained by {@link #arrayBaseOffset} and
   *       {@link #arrayIndexScale} (respectively) from the array's class. The value referred to is
   *       the <code>N</code><em>th</em> element of the array.
   * </ul>
   *
   * <p>If one of the above cases is true, the call references a specific Java variable (field or
   * array element). However, the results are undefined if that variable is not in fact of the type
   * returned by this method.
   *
   * <p>This method refers to a variable by means of two parameters, and so it provides (in effect)
   * a <em>double-register</em> addressing mode for Java variables. When the object reference is
   * null, this method uses its offset as an absolute address. This is similar in operation to
   * methods such as {@link #getInt(long)}, which provide (in effect) a <em>single-register</em>
   * addressing mode for non-Java variables. However, because Java variables may have a different
   * layout in memory from non-Java variables, programmers should not assume that these two
   * addressing modes are ever equivalent. Also, programmers should remember that offsets from the
   * double-register addressing mode cannot be portably confused with longs used in the
   * single-register addressing mode.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   */
  public static final int getInt(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getInt(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * <p>The first two parameters are interpreted exactly as with {@link #getInt(Object, long)} to
   * refer to a specific Java variable (field or array element). The given value is stored into that
   * variable.
   *
   * <p>The variable must be of the same type as the method parameter <code>x</code>.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   */
  public static final void putInt(final Object o, final long offset, final int x)
      throws RuntimeException {
    UNSAFE.putInt(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final Object getObject(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getObject(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable.
   *
   * <p>Unless the reference <code>x</code> being stored is either null or matches the field type,
   * the results are undefined. If the reference <code>o</code> is non-null, car marks or other
   * store barriers for that object (if the VM requires them) are updated.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putObject(final Object o, final long offset, final Object x)
      throws RuntimeException {
    UNSAFE.putObject(o, offset, x);
  }

  /**
   * Fetches a value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final boolean getBoolean(final Object o, final long offset)
      throws RuntimeException {
    return UNSAFE.getBoolean(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putBoolean(final Object o, final long offset, final boolean x)
      throws RuntimeException {
    UNSAFE.putBoolean(o, offset, x);
  }

  /**
   * Fetches a value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final byte getByte(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getByte(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putByte(final Object o, final long offset, final byte x)
      throws RuntimeException {
    UNSAFE.putByte(o, offset, x);
  }

  /**
   * Fetches a value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final short getShort(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getShort(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putShort(final Object o, final long offset, final short x)
      throws RuntimeException {
    UNSAFE.putShort(o, offset, x);
  }

  /**
   * Fetches a value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final char getChar(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getChar(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putChar(final Object o, final long offset, final char x)
      throws RuntimeException {
    UNSAFE.putChar(o, offset, x);
  }

  /**
   * Fetches a value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final long getLong(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getLong(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putLong(final Object o, final long offset, final long x)
      throws RuntimeException {
    UNSAFE.putLong(o, offset, x);
  }

  /**
   * Fetches a value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final float getFloat(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getFloat(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putFloat(final Object o, final long offset, final float x)
      throws RuntimeException {
    UNSAFE.putFloat(o, offset, x);
  }

  /**
   * Fetches a value from a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #getInt(Object, long)
   */
  public static final double getDouble(final Object o, final long offset) throws RuntimeException {
    return UNSAFE.getDouble(o, offset);
  }

  /**
   * Stores a value into a given Java variable.
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @throws RuntimeException No defined exceptions are thrown, not even {@link
   *     NullPointerException}
   * @see #putInt(Object, long, int)
   */
  public static final void putDouble(final Object o, final long offset, final double x)
      throws RuntimeException {
    UNSAFE.putDouble(o, offset, x);
  }

  // These work on values in the C heap.

  /**
   * Fetches a value from a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to fetch the data from
   * @return The value stored at the given address
   * @see #allocateMemory
   */
  public static final byte getByte(final long address) {
    return UNSAFE.getByte(address);
  }

  /**
   * Stores a value into a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to store the data to
   * @param x The data to store
   * @see #getByte(long)
   */
  public static final void putByte(final long address, final byte x) {
    UNSAFE.putByte(address, x);
  }

  /**
   * Fetches a value from a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to fetch the data from
   * @return The value stored at the given address
   * @see #allocateMemory
   */
  public static final short getShort(final long address) {
    return UNSAFE.getShort(address);
  }

  /**
   * Stores a value into a given memory address.
   *
   * @param address The memory address to store the data to
   * @param x The data to store
   * @see #getShort(long)
   */
  public static final void putShort(final long address, final short x) {
    UNSAFE.putShort(address, x);
  }

  /**
   * Fetches a value from a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to fetch the data from
   * @return The value stored at the given address
   * @see #allocateMemory
   */
  public static final char getChar(final long address) {
    return UNSAFE.getChar(address);
  }

  /**
   * Stores a value into a given memory address.
   *
   * @param address The memory address to store the data to
   * @param x The data to store
   * @see #getChar(long)
   */
  public static final void putChar(final long address, final char x) {
    UNSAFE.putChar(address, x);
  }

  /**
   * Fetches a value from a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to fetch the data from
   * @return The value stored at the given address
   * @see #allocateMemory
   */
  public static final int getInt(final long address) {
    return UNSAFE.getInt(address);
  }

  /**
   * Stores a value into a given memory address.
   *
   * @param address The memory address to store the data to
   * @param x The data to store
   * @see #getInt(long)
   */
  public static final void putInt(final long address, final int x) {
    UNSAFE.putInt(address, x);
  }

  /**
   * Fetches a value from a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to fetch the data from
   * @return The value stored at the given address
   * @see #allocateMemory
   */
  public static final long getLong(final long address) {
    return UNSAFE.getLong(address);
  }

  /**
   * Stores a value into a given memory address.
   *
   * @param address The memory address to store the data to
   * @param x The data to store
   * @see #getLong(long)
   */
  public static final void putLong(final long address, final long x) {
    UNSAFE.putLong(address, x);
  }

  /**
   * Fetches a value from a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to fetch the data from
   * @return The value stored at the given address
   * @see #allocateMemory
   */
  public static final float getFloat(final long address) {
    return UNSAFE.getFloat(address);
  }

  /**
   * Stores a value into a given memory address.
   *
   * @param address The memory address to store the data to
   * @param x The data to store
   * @see #getFloat(long)
   */
  public static final void putFloat(final long address, final float x) {
    UNSAFE.putFloat(address, x);
  }

  /**
   * Fetches a value from a given memory address.
   *
   * <p>If the address is zero, or does not point into a block obtained from {@link
   * #allocateMemory}, the results are undefined.
   *
   * @param address The memory address to fetch the data from
   * @return The value stored at the given address
   * @see #allocateMemory
   */
  public static final double getDouble(final long address) {
    return UNSAFE.getDouble(address);
  }

  /**
   * Stores a value into a given memory address.
   *
   * @param address The memory address to store the data to
   * @param x The data to store
   * @see #getDouble(long)
   */
  public static final void putDouble(final long address, final double x) {
    UNSAFE.putDouble(address, x);
  }

  /**
   * Fetches a static final pointer from a given memory address. If the address is zero, or does not
   * point into a block obtained from {@link #allocateMemory}, the results are undefined.
   *
   * <p>If the static final pointer is less than 64 bits wide, it is extended as an unsigned number
   * to a Java long. The pointer may be indexed by any given byte offset, simply by adding that
   * offset (as a simple integer) to the long representing the pointer. The number of bytes actually
   * read from the target address maybe determined by consulting {@link #addressSize}.
   *
   * @param address A memory address
   * @return The pointer stored at that address
   * @see #allocateMemory
   */
  public static final long getAddress(final long address) {
    return UNSAFE.getAddress(address);
  }

  /**
   * Stores a static final pointer into a given memory address. If the address is zero, or does not
   * point into a block obtained from {@link #allocateMemory}, the results are undefined.
   *
   * <p>The number of bytes actually written at the target address maybe determined by consulting
   * {@link #addressSize}.
   *
   * @param address The memory address to write to
   * @param x The pointer to store at that address
   * @see #getAddress(long)
   */
  public static final void putAddress(final long address, final long x) {
    UNSAFE.putAddress(address, x);
  }

  /// wrappers for malloc, realloc, free:

  /**
   * Allocates a new block of static final memory, of the given size in bytes.
   *
   * <p>The contents of the memory are uninitialized; they will generally be garbage. The resulting
   * static final pointer will never be zero, and will be aligned for all value types.
   *
   * <p>Dispose of this memory by calling {@link #freeMemory}, or resize it with {@link
   * #reallocateMemory}.
   *
   * @param bytes The size (in bytes) of the block of memory to allocate
   * @return The pointer to this allocated memory.
   * @throws IllegalArgumentException if the size is negative or too large for the static final
   *     size_t type
   * @throws OutOfMemoryError if the allocation is refused by the system
   * @see #getByte(long)
   * @see #putByte(long, byte)
   */
  public static final long allocateMemory(final long bytes)
      throws IllegalArgumentException, OutOfMemoryError {
    return UNSAFE.allocateMemory(bytes);
  }

  /**
   * Resizes a new block of static final memory, to the given size in bytes.
   *
   * <p>The contents of the new block past the size of the old block are uninitialized; they will
   * generally be garbage. The resulting static final pointer will be zero if and only if the
   * requested size is zero. The resulting static final pointer will be aligned for all value types.
   *
   * <p>Dispose of this memory by calling {@link #freeMemory}, or resize it with {@link
   * #reallocateMemory}.
   *
   * <p>The address passed to this method may be null, in which case an allocation will be
   * performed.
   *
   * @param address The address of the block to resize, or <code>null</code> to perform an
   *     allocation
   * @param bytes The size (in bytes) of the desired memory block
   * @return The pointer to the allocated memory
   * @throws IllegalArgumentException if the size is negative or too large for the static final
   *     size_t type
   * @throws OutOfMemoryError if the allocation is refused by the system
   * @see #allocateMemory
   */
  public static final long reallocateMemory(final long address, final long bytes)
      throws IllegalArgumentException, OutOfMemoryError {
    return UNSAFE.reallocateMemory(address, bytes);
  }

  /**
   * Sets all bytes in a given block of memory to a fixed value (usually zero).
   *
   * <p>This method determines a block's base address by means of two parameters, and so it provides
   * (in effect) a <em>double-register</em> addressing mode, as discussed in {@link #getInt(Object,
   * long)}. When the object reference is null, the offset supplies an absolute base address.
   *
   * <p>The stores are in coherent (atomic) units of a size determined by the address and length
   * parameters. If the effective address and length are all even modulo 8, the stores take place in
   * 'long' units. If the effective address and length are (resp.) even modulo 4 or 2, the stores
   * take place in units of 'int' or 'short'.
   *
   * @param o Java heap object in which the memory block resides, if any, else <code>null</code>
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param bytes The number of bytes to set to the given value
   * @param value The value to set all the target bytes to
   * @since 1.7
   */
  public static final void setMemory(
      final Object o, final long offset, final long bytes, final byte value) {
    UNSAFE.setMemory(o, offset, bytes, value);
  }

  /**
   * Sets all bytes in a given block of memory to a fixed value (usually zero). This provides a
   * <em>single-register</em> addressing mode, as discussed in {@link #getInt(Object, long)}.
   *
   * <p>Equivalent to <code>setMemory(null, address, bytes, value)</code>.
   *
   * @param address The address of the block of memory
   * @param bytes The number of bytes to set to the given value
   * @param value The value to set all the target bytes to
   */
  public static final void setMemory(final long address, final long bytes, final byte value) {
    UNSAFE.setMemory(address, bytes, value);
  }

  /**
   * Sets all bytes in a given block of memory to a copy of another block.
   *
   * <p>This method determines each block's base address by means of two parameters, and so it
   * provides (in effect) a <em>double-register</em> addressing mode, as discussed in {@link
   * #getInt(Object, long)}. When the object reference is null, the offset supplies an absolute base
   * address.
   *
   * <p>The transfers are in coherent (atomic) units of a size determined by the address and length
   * parameters. If the effective addresses and length are all even modulo 8, the transfer takes
   * place in 'long' units. If the effective addresses and length are (resp.) even modulo 4 or 2,
   * the transfer takes place in units of 'int' or 'short'.
   *
   * @param srcBase The source Java heap object in which the source memory block resides, if any,
   *     else <code>null</code>
   * @param srcOffset indication of where the source memory block resides in a Java heap object, if
   *     any, else a memory address locating the memory block statically
   * @param destBase The source Java heap object in which the destination memory block resides, if
   *     any, else <code>null</code>
   * @param destOffset indication of where the destination memory block resides in a Java heap
   *     object, if any, else a memory address locating the memory block statically
   * @param bytes The number of bytes to copy
   * @since 1.7
   */
  public static final void copyMemory(
      final Object srcBase,
      final long srcOffset,
      final Object destBase,
      final long destOffset,
      final long bytes) {
    UNSAFE.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
  }

  /**
   * Sets all bytes in a given block of memory to a copy of another block.
   *
   * <p>This provides a <em>single-register</em> addressing mode, as discussed in {@link
   * #getInt(Object, long)}.
   *
   * <p>Equivalent to <code>copyMemory(null, srcAddress, null, destAddress, bytes)</code>.
   *
   * @param srcAddress The address of the source memory block
   * @param destAddress The address of the destination memory block
   * @param bytes The number of bytes to copy
   */
  public static final void copyMemory(
      final long srcAddress, final long destAddress, final long bytes) {
    UNSAFE.copyMemory(srcAddress, destAddress, bytes);
  }

  /**
   * Disposes of a block of static final memory, as obtained from {@link #allocateMemory} or {@link
   * #reallocateMemory}. The address passed to this method may be null, in which case no action is
   * taken.
   *
   * @param address The address of the memory block to free
   * @see #allocateMemory
   */
  public static final void freeMemory(final long address) {
    UNSAFE.freeMemory(address);
  }

  /// random queries

  /**
   * This constant differs from all results that will ever be returned from {@link
   * #staticFieldOffset}, {@link #objectFieldOffset}, or {@link #arrayBaseOffset}.
   */
  public static final int INVALID_FIELD_OFFSET = sun.misc.Unsafe.INVALID_FIELD_OFFSET;

  /**
   * Report the location of a given field in the storage allocation of its class. Do not expect to
   * perform any sort of arithmetic on this offset; it is just a cookie which is passed to the
   * unsafe heap memory accessors.
   *
   * <p>Any given field will always have the same offset and base, and no two distinct fields of the
   * same class will ever have the same offset and base.
   *
   * <p>As of 1.4.1, offsets for fields are represented as long values, although the Sun JVM does
   * not use the most significant 32 bits. However, JVM implementations which store static fields at
   * absolute addresses can use long offsets and null base pointers to express the field locations
   * in a form usable by {@link #getInt(Object, long)}. Therefore, code which will be ported to such
   * JVMs on 64-bit platforms must preserve all bits of static field offsets.
   *
   * @param f A {@link Field field}
   * @return The location of this field in the storage allocation of its class (i.e. its offset)
   * @see #getInt(Object, long)
   */
  public static final long objectFieldOffset(final Field f) {
    return UNSAFE.objectFieldOffset(f);
  }

  /**
   * Report the location of a given static field, in conjunction with {@link #staticFieldBase}.
   *
   * <p>Do not expect to perform any sort of arithmetic on this offset; it is just a cookie which is
   * passed to the unsafe heap memory accessors.
   *
   * <p>Any given field will always have the same offset, and no two distinct fields of the same
   * class will ever have the same offset.
   *
   * <p>As of 1.4.1, offsets for fields are represented as long values, although the Sun JVM does
   * not use the most significant 32 bits. It is hard to imagine a JVM technology which needs more
   * than a few bits to encode an offset within a non-array object, However, for consistency with
   * other methods in this class, this method reports its result as a long value.
   *
   * @param f The field for which we want to get the position
   * @return The location of the given static field
   * @see #getInt(Object, long)
   */
  public static final long staticFieldOffset(final Field f) {
    return UNSAFE.staticFieldOffset(f);
  }

  /**
   * Report the location of a given static field, in conjunction with {@link #staticFieldOffset}.
   *
   * <p>Fetch the base "Object", if any, with which static fields of the given class can be accessed
   * via methods like {@link #getInt(Object, long)}. This value may be null. This value may refer to
   * an object which is a "cookie", not guaranteed to be a real Object, and it should not be used in
   * any way except as argument to the get and put routines in this class.
   *
   * @param f The field for which we want to get the position
   * @return The base Object with which static fields of the given class can be accessed
   */
  public static final Object staticFieldBase(final Field f) {
    return UNSAFE.staticFieldBase(f);
  }

  /**
   * Ensure the given class has been initialized. This is often needed in conjunction with obtaining
   * the static field base of a class.
   *
   * @param c The {@link Class} to check
   */
  public static final void ensureClassInitialized(final Class<?> c) {
    UNSAFE.ensureClassInitialized(c);
  }

  /**
   * Report the offset of the first element in the storage allocation of a given array class.
   *
   * <p>If {@link #arrayIndexScale} returns a non-zero value for the same class, you may use that
   * scale factor, together with this base offset, to form new offsets to access elements of arrays
   * of the given class.
   *
   * @param arrayClass An array {@link Class class} Object
   * @return The offset of the first element in the storage allocation of the given array class
   * @see #getInt(Object, long)
   * @see #putInt(Object, long, int)
   */
  public static final int arrayBaseOffset(final Class<?> arrayClass) {
    return UNSAFE.arrayBaseOffset(arrayClass);
  }

  /** The value of {@code arrayBaseOffset(boolean[].class)} */
  public static final int ARRAY_BOOLEAN_BASE_OFFSET = sun.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(byte[].class)} */
  public static final int ARRAY_BYTE_BASE_OFFSET = sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(short[].class)} */
  public static final int ARRAY_SHORT_BASE_OFFSET = sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(char[].class)} */
  public static final int ARRAY_CHAR_BASE_OFFSET = sun.misc.Unsafe.ARRAY_CHAR_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(int[].class)} */
  public static final int ARRAY_INT_BASE_OFFSET = sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(long[].class)} */
  public static final int ARRAY_LONG_BASE_OFFSET = sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(float[].class)} */
  public static final int ARRAY_FLOAT_BASE_OFFSET = sun.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(double[].class)} */
  public static final int ARRAY_DOUBLE_BASE_OFFSET = sun.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;

  /** The value of {@code arrayBaseOffset(Object[].class)} */
  public static final int ARRAY_OBJECT_BASE_OFFSET = sun.misc.Unsafe.ARRAY_OBJECT_BASE_OFFSET;

  /**
   * Report the scale factor for addressing elements in the storage allocation of a given array
   * class.
   *
   * <p>However, arrays of "narrow" types will generally not work properly with accessors like
   * {@link #getByte(Object, long)}, so the scale factor for such classes is reported as zero.
   *
   * @param arrayClass An array {@link Class class} Object
   * @return The scale factor for addressing elements in the storage allocation of a given array
   *     class.
   * @see #arrayBaseOffset
   * @see #getInt(Object, long)
   * @see #putInt(Object, long, int)
   */
  public static final int arrayIndexScale(final Class<?> arrayClass) {
    return UNSAFE.arrayIndexScale(arrayClass);
  }

  /** The value of {@code arrayIndexScale(boolean[].class)} */
  public static final int ARRAY_BOOLEAN_INDEX_SCALE = sun.misc.Unsafe.ARRAY_BOOLEAN_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(byte[].class)} */
  public static final int ARRAY_BYTE_INDEX_SCALE = sun.misc.Unsafe.ARRAY_BYTE_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(short[].class)} */
  public static final int ARRAY_SHORT_INDEX_SCALE = sun.misc.Unsafe.ARRAY_SHORT_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(char[].class)} */
  public static final int ARRAY_CHAR_INDEX_SCALE = sun.misc.Unsafe.ARRAY_CHAR_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(int[].class)} */
  public static final int ARRAY_INT_INDEX_SCALE = sun.misc.Unsafe.ARRAY_INT_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(long[].class)} */
  public static final int ARRAY_LONG_INDEX_SCALE = sun.misc.Unsafe.ARRAY_LONG_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(float[].class)} */
  public static final int ARRAY_FLOAT_INDEX_SCALE = sun.misc.Unsafe.ARRAY_FLOAT_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(double[].class)} */
  public static final int ARRAY_DOUBLE_INDEX_SCALE = sun.misc.Unsafe.ARRAY_DOUBLE_INDEX_SCALE;

  /** The value of {@code arrayIndexScale(Object[].class)} */
  public static final int ARRAY_OBJECT_INDEX_SCALE = sun.misc.Unsafe.ARRAY_OBJECT_INDEX_SCALE;

  /**
   * Report the size in bytes of a static final pointer, as stored via {@link #putAddress}. This
   * value will be either 4 or 8.
   *
   * <p>Note that the sizes of other primitive types (as stored in static final memory blocks) is
   * determined fully by their information content.
   *
   * @return The size in bytes of a static final pointer
   */
  public static final int addressSize() {
    return UNSAFE.addressSize();
  }

  /** The value of {@code addressSize()} */
  public static final int ADDRESS_SIZE = sun.misc.Unsafe.ADDRESS_SIZE;

  /**
   * Report the size in bytes of a static final memory page (whatever that is). This value will
   * always be a power of two.
   *
   * @return The size in bytes of a static final memory page
   */
  public static final int pageSize() {
    return UNSAFE.pageSize();
  }

  /// random trusted operations from JNI:

  /**
   * Throws the given {@link Throwable exception} without telling the verifier.
   *
   * @param ee The exception to throw
   */
  public static final void throwException(final Throwable ee) {
    UNSAFE.throwException(ee);
  }

  /**
   * Atomically sets the field of the target object to the given updated value if the current value
   * of the field {@code ==} the expected value.
   *
   * @param o The object to update
   * @param offset The offset of the field to update in the target object
   * @param expVal The expected value
   * @param updVal The new value
   * @return <code>true</code> if successful. <code>false</code> return indicates that the actual
   *     value was not equal to the expected value.
   */
  public static final boolean compareAndSwapInt(
      final Object o, final long offset, final int expVal, final int updVal) {
    return UNSAFE.compareAndSwapInt(o, offset, expVal, updVal);
  }

  /**
   * Atomically sets the field of the target object to the given updated value if the current value
   * of the field {@code ==} the expected value.
   *
   * @param o The object to update
   * @param offset The offset of the field to update in the target object
   * @param expVal The expected value
   * @param updVal The new value
   * @return <code>true</code> if successful. <code>false</code> return indicates that the actual
   *     value was not equal to the expected value.
   */
  public static final boolean compareAndSwapLong(
      final Object o, final long offset, final long expVal, final long updVal) {
    return UNSAFE.compareAndSwapLong(o, offset, expVal, updVal);
  }

  /**
   * Atomically sets the field of the target object to the given updated value if the current value
   * of the field {@code ==} the expected value.
   *
   * @param o The object to update
   * @param offset The offset of the field to update in the target object
   * @param expVal The expected value
   * @param updVal The new value
   * @return <code>true</code> if successful. <code>false</code> return indicates that the actual
   *     value was not equal to the expected value.
   */
  public static final boolean compareAndSwapObject(
      final Object o, final long offset, final Object expVal, final Object updVal) {
    return UNSAFE.compareAndSwapObject(o, offset, expVal, updVal);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getObject(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getObject(Object, long)
   */
  public static final Object getObjectVolatile(final Object o, final long offset) {
    return UNSAFE.getObjectVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putObject(Object, long, Object)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putObject(Object, long, Object)
   */
  public static final void putObjectVolatile(final Object o, final long offset, final Object x) {
    UNSAFE.putObjectVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getInt(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getInt(Object, long)
   */
  public static final int getIntVolatile(final Object o, final long offset) {
    return UNSAFE.getIntVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putInt(Object, long, int)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putInt(Object, long, int)
   */
  public static final void putIntVolatile(final Object o, final long offset, final int x) {
    UNSAFE.putIntVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getBoolean(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getBoolean(Object, long)
   */
  public static final boolean getBooleanVolatile(final Object o, final long offset) {
    return UNSAFE.getBooleanVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putBoolean(Object, long, boolean)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putBoolean(Object, long, boolean)
   */
  public static final void putBooleanVolatile(final Object o, final long offset, final boolean x) {
    UNSAFE.putBooleanVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getByte(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getByte(Object, long)
   */
  public static final byte getByteVolatile(final Object o, final long offset) {
    return UNSAFE.getByteVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putByte(Object, long, byte)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putByte(Object, long, byte)
   */
  public static final void putByteVolatile(final Object o, final long offset, final byte x) {
    UNSAFE.putByteVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getShort(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getShort(Object, long)
   */
  public static final short getShortVolatile(final Object o, final long offset) {
    return UNSAFE.getShortVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putShort(Object, long, short)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putShort(Object, long, short)
   */
  public static final void putShortVolatile(final Object o, final long offset, final short x) {
    UNSAFE.putShortVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getChar(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getChar(Object, long)
   */
  public static final char getCharVolatile(final Object o, final long offset) {
    return UNSAFE.getCharVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putChar(Object, long, char)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putChar(Object, long, char)
   */
  public static final void putCharVolatile(final Object o, final long offset, final char x) {
    UNSAFE.putCharVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getLong(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getLong(Object, long)
   */
  public static final long getLongVolatile(final Object o, final long offset) {
    return UNSAFE.getLongVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putLong(Object, long, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putLong(Object, long, long)
   */
  public static final void putLongVolatile(final Object o, final long offset, final long x) {
    UNSAFE.putLongVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getFloat(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getFloat(Object, long)
   */
  public static final float getFloatVolatile(final Object o, final long offset) {
    return UNSAFE.getFloatVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putFloat(Object, long, float)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putFloat(Object, long, float)
   */
  public static final void putFloatVolatile(final Object o, final long offset, final float x) {
    UNSAFE.putFloatVolatile(o, offset, x);
  }

  /**
   * Fetches a reference value from a given Java variable, with volatile load semantics.
   *
   * <p>Otherwise identical to {@link #getDouble(Object, long)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @return the value fetched from the indicated Java variable
   * @see #getDouble(Object, long)
   */
  public static final double getDoubleVolatile(final Object o, final long offset) {
    return UNSAFE.getDoubleVolatile(o, offset);
  }

  /**
   * Stores a reference value into a given Java variable, with volatile store semantics.
   *
   * <p>Otherwise identical to {@link #putDouble(Object, long, double)}
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putDouble(Object, long, double)
   */
  public static final void putDoubleVolatile(final Object o, final long offset, final double x) {
    UNSAFE.putDoubleVolatile(o, offset, x);
  }

  /**
   * Version of {@link #putObjectVolatile(Object, long, Object)} that does not guarantee immediate
   * visibility of the store to other threads.
   *
   * <p>This method is generally only useful if the underlying field is a Java volatile (or if an
   * array cell, one that is otherwise only accessed using volatile accesses).
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putObjectVolatile(Object, long, Object)
   */
  public static final void putOrderedObject(final Object o, final long offset, final Object x) {
    UNSAFE.putOrderedObject(o, offset, x);
  }

  /**
   * Version of {@link #putIntVolatile(Object, long, int)} that does not guarantee immediate
   * visibility of the store to other threads.
   *
   * <p>This method is generally only useful if the underlying field is a Java volatile (or if an
   * array cell, one that is otherwise only accessed using volatile accesses).
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putIntVolatile(Object, long, int)
   */
  public static final void putOrderedInt(final Object o, final long offset, final int x) {
    UNSAFE.putOrderedInt(o, offset, x);
  }

  /**
   * Version of {@link #putLongVolatile(Object, long, long)} that does not guarantee immediate
   * visibility of the store to other threads.
   *
   * <p>This method is generally only useful if the underlying field is a Java volatile (or if an
   * array cell, one that is otherwise only accessed using volatile accesses).
   *
   * @param o Java heap object in which the variable resides, if any, else null
   * @param offset indication of where the variable resides in a Java heap object, if any, else a
   *     memory address locating the variable statically
   * @param x the value to store into the indicated Java variable
   * @see #putLongVolatile(Object, long, long)
   */
  public static final void putOrderedLong(final Object o, final long offset, final long x) {
    UNSAFE.putOrderedLong(o, offset, x);
  }

  /**
   * Unblock the given thread blocked on <tt>park</tt>, or, if it is not blocked, cause the
   * subsequent call to <tt>park</tt> not to block.
   *
   * <p>Note: this operation is "unsafe" solely because the caller must somehow ensure that the
   * thread has not been destroyed. Nothing special is usually required to ensure this when called
   * from Java (in which there will ordinarily be a live reference to the thread) but this is not
   * nearly-automatically so when calling from static final code.
   *
   * @param thread the thread to unpark.
   * @see #park(boolean, long)
   */
  public static final void unpark(final Object thread) {
    UNSAFE.unpark(thread);
  }

  /**
   * Block current thread, returning when either:
   *
   * <ul>
   *   <li>a balancing <tt>unpark</tt> occurs
   *   <li>a balancing <tt>unpark</tt> has already occurred
   *   <li>the thread is interrupted
   *   <li>if not absolute and time is not zero, the given time nanoseconds have elapsed
   *   <li>if absolute, the given deadline in milliseconds since Epoch has passed
   *   <li>spuriously (i.e., returning for no "reason")
   * </ul>
   *
   * <p>Note: This operation is in the Unsafe class only because <tt>unpark</tt> is, so it would be
   * strange to place it elsewhere.
   *
   * @param isAbsolute Whether the given time is absolute (i.e. since Epoch) or relative to the
   *     current time
   * @param time (in ns) The time to sleep if not absolute, or the time to wake up if absolute
   * @see #unpark(Object)
   */
  public static final void park(final boolean isAbsolute, final long time) {
    UNSAFE.park(isAbsolute, time);
  }

  /**
   * Gets the load average in the system run queue assigned to the available processors averaged
   * over various periods of time. This method retrieves the given <tt>nelem</tt> samples and
   * assigns to the elements of the given <tt>loadavg</tt> array. The system imposes a maximum of 3
   * samples, representing averages over the last 1, 5, and 15 minutes, respectively.
   *
   * @param loadavg an array of double of size nelems
   * @param nelems the number of samples to be retrieved and must be 1 to 3.
   * @return the number of samples actually retrieved; or -1 if the load average is unobtainable.
   */
  public static int getLoadAverage(double[] loadavg, int nelems) {
    return UNSAFE.getLoadAverage(loadavg, nelems);
  }

  // The following contain CAS-based Java implementations used on
  // platforms not supporting native instructions

  /**
   * Atomically adds the given value to the current value of a field or array element within the
   * given object <code>o</code> at the given <code>offset</code>.
   *
   * @param o object/array to update the field/element in
   * @param offset field/element offset
   * @param delta the value to add
   * @return the previous value
   * @since 1.8
   */
  public static final int getAndAddInt(Object o, long offset, int delta) {
    return UNSAFE.getAndAddInt(o, offset, delta);
  }

  /**
   * Atomically adds the given value to the current value of a field or array element within the
   * given object <code>o</code> at the given <code>offset</code>.
   *
   * @param o object/array to update the field/element in
   * @param offset field/element offset
   * @param delta the value to add
   * @return the previous value
   * @since 1.8
   */
  public static final long getAndAddLong(Object o, long offset, long delta) {
    return UNSAFE.getAndAddLong(o, offset, delta);
  }

  /**
   * Atomically exchanges the given value with the current value of a field or array element within
   * the given object <code>o</code> at the given <code>offset</code>.
   *
   * @param o object/array to update the field/element in
   * @param offset field/element offset
   * @param newValue new value
   * @return the previous value
   * @since 1.8
   */
  public static final int getAndSetInt(Object o, long offset, int newValue) {
    return UNSAFE.getAndSetInt(o, offset, newValue);
  }

  /**
   * Atomically exchanges the given value with the current value of a field or array element within
   * the given object <code>o</code> at the given <code>offset</code>.
   *
   * @param o object/array to update the field/element in
   * @param offset field/element offset
   * @param newValue new value
   * @return the previous value
   * @since 1.8
   */
  public static final long getAndSetLong(Object o, long offset, long newValue) {
    return UNSAFE.getAndSetLong(o, offset, newValue);
  }

  /**
   * Atomically exchanges the given reference value with the current reference value of a field or
   * array element within the given object <code>o</code> at the given <code>offset</code>.
   *
   * @param o object/array to update the field/element in
   * @param offset field/element offset
   * @param newValue new value
   * @return the previous value
   * @since 1.8
   */
  public static final Object getAndSetObject(Object o, long offset, Object newValue) {
    return UNSAFE.getAndSetObject(o, offset, newValue);
  }

  /**
   * Ensures lack of reordering of loads before the fence with loads or stores after the fence.
   *
   * @since 1.8
   */
  public static void loadFence() {
    UNSAFE.loadFence();
  }

  /**
   * Ensures lack of reordering of stores before the fence with loads or stores after the fence.
   *
   * @since 1.8
   */
  public static void storeFence() {
    UNSAFE.storeFence();
  }

  /**
   * Ensures lack of reordering of loads or stores before the fence with loads or stores after the
   * fence.
   *
   * @since 1.8
   */
  public static void fullFence() {
    UNSAFE.fullFence();
  }

  //////////////////////////////////////////////////
  // Other utility methods that tend to be useful //
  // with Unsafe mechanics.                       //
  //////////////////////////////////////////////////

  /**
   * Returns the offset of a field in the given class.
   *
   * @param clazz The class in which the field has been declared
   * @param fieldName The name of the field
   * @return The offset of the field in this class
   */
  public static final long getFieldOffset(final Class<?> clazz, final String fieldName) {
    try {
      return UNSAFE.objectFieldOffset(clazz.getDeclaredField(fieldName));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  /**
   * This method is similar to {@link #arrayIndexScale(Class)}, except that it returns the power of
   * 2 order of the index scale (i.e. the integer k suck as 2^k is equal to the scale) that is
   * suitable for a bit shift.
   *
   * @param arrayClass An array class.
   * @return The power of 2 order of the array index scale.
   */
  public static final int arrayIndexShift(final Class<?> arrayClass) {
    final int scale = UNSAFE.arrayIndexScale(arrayClass);
    if ((scale & (scale - 1)) != 0)
      throw new Error("data type scale not a power of two for array class " + arrayClass.getName());
    return 31 - Integer.numberOfLeadingZeros(scale);
  }

  /**
   * Returns the smallest integer k such as 2^k &ge; value.
   *
   * @param value A positive integer
   * @return The smallest integer k such as 2^k &ge; value
   */
  public static final int getOrder(final int value) {
    return 32 - Integer.numberOfLeadingZeros(value - 1);
  }

  /**
   * Checks whether the given value is a power of 2.
   *
   * @param value A number to check
   * @return <code>true</code> iff the given value is a power of 2
   */
  public static final boolean isPowerOfTwo(final long value) {
    return (value & (value - 1)) == 0;
  }

  //////////////////////////////////////////////////
  // Unsafe mechanics specialized for Arrays.     //
  //////////////////////////////////////////////////

  /** The base offset for an {@code int} array */
  public static final int INT_ARRAY_BASE = UnsafeUtil.arrayBaseOffset(int[].class);

  /** The base shift for an {@code int} array */
  public static final int INT_ARRAY_SHIFT = UnsafeUtil.arrayIndexShift(int[].class);

  /**
   * Writes the given value at the given index in the array.
   *
   * @param intArray The integer array
   * @param index The index to write to
   * @param intValue The value to write
   */
  public static final void intArrayWrite(int[] intArray, int index, int intValue) {
    UnsafeUtil.putInt(intArray, (((long) index) << INT_ARRAY_SHIFT) + INT_ARRAY_BASE, intValue);
  }

  /**
   * Writes the given value at the given index in the array in an ordered fashion (see {@link
   * #putOrderedInt(Object, long, int)}).
   *
   * @param intArray The integer array
   * @param index The index to write to
   * @param intValue The value to write
   */
  public static final void intArrayWriteOrdered(int[] intArray, int index, int intValue) {
    UnsafeUtil.putOrderedInt(
        intArray, (((long) index) << INT_ARRAY_SHIFT) + INT_ARRAY_BASE, intValue);
  }

  /**
   * Writes the given value at the given index in the array in a volatile fashion (see {@link
   * #putIntVolatile(Object, long, int)})
   *
   * @param intArray The integer array
   * @param index The index to write to
   * @param intValue The value to write
   */
  public static final void intArrayWriteVolatile(int[] intArray, int index, int intValue) {
    UnsafeUtil.putIntVolatile(
        intArray, (((long) index) << INT_ARRAY_SHIFT) + INT_ARRAY_BASE, intValue);
  }

  /**
   * Reads the value stored at the given index in the array.
   *
   * @param intArray The integer array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final int intArrayRead(int[] intArray, int index) {
    return UnsafeUtil.getInt(intArray, (((long) index) << INT_ARRAY_SHIFT) + INT_ARRAY_BASE);
  }

  /**
   * Reads the value stored at the given index in the array in a volatile fashion (see {@link
   * #getIntVolatile(Object, long)}).
   *
   * @param intArray The integer array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final int intArrayReadVolatile(int[] intArray, int index) {
    return UnsafeUtil.getIntVolatile(
        intArray, (((long) index) << INT_ARRAY_SHIFT) + INT_ARRAY_BASE);
  }

  /** The base offset for an {@code long} array */
  public static final int LONG_ARRAY_BASE = UnsafeUtil.arrayBaseOffset(long[].class);

  /** The base shift for an {@code long} array */
  public static final int LONG_ARRAY_SHIFT = UnsafeUtil.arrayIndexShift(long[].class);

  /**
   * Writes the given value at the given index in the array.
   *
   * @param longArray The long array
   * @param index The index to write to
   * @param longValue The value to write
   */
  public static final void longArrayWrite(long[] longArray, int index, long longValue) {
    UnsafeUtil.putLong(
        longArray, (((long) index) << LONG_ARRAY_SHIFT) + LONG_ARRAY_BASE, longValue);
  }

  /**
   * Writes the given value at the given index in the array in an ordered fashion (see {@link
   * #putOrderedLong(Object, long, long)}).
   *
   * @param longArray The long array
   * @param index The index to write to
   * @param longValue The value to write
   */
  public static final void longArrayWriteOrdered(long[] longArray, int index, long longValue) {
    UnsafeUtil.putOrderedLong(
        longArray, (((long) index) << LONG_ARRAY_SHIFT) + LONG_ARRAY_BASE, longValue);
  }

  /**
   * Writes the given value at the given index in the array in a volatile fashion (see {@link
   * #putLongVolatile(Object, long, long)})
   *
   * @param longArray The long array
   * @param index The index to write to
   * @param longValue The value to write
   */
  public static final void longArrayWriteVolatile(long[] longArray, int index, long longValue) {
    UnsafeUtil.putLongVolatile(
        longArray, (((long) index) << LONG_ARRAY_SHIFT) + LONG_ARRAY_BASE, longValue);
  }

  /**
   * Reads the value stored at the given index in the array.
   *
   * @param longArray The long array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final long longArrayRead(long[] longArray, int index) {
    return UnsafeUtil.getLong(longArray, (((long) index) << LONG_ARRAY_SHIFT) + LONG_ARRAY_BASE);
  }

  /**
   * Reads the value stored at the given index in the array in a volatile fashion (see {@link
   * #getLongVolatile(Object, long)}).
   *
   * @param longArray The long array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final long longArrayReadVolatile(long[] longArray, int index) {
    return UnsafeUtil.getLongVolatile(
        longArray, (((long) index) << LONG_ARRAY_SHIFT) + LONG_ARRAY_BASE);
  }

  /** The base offset for an {@code float} array */
  public static final int FLOAT_ARRAY_BASE = UnsafeUtil.arrayBaseOffset(float[].class);

  /** The base shift for an {@code float} array */
  public static final int FLOAT_ARRAY_SHIFT = UnsafeUtil.arrayIndexShift(float[].class);

  /**
   * Writes the given value at the given index in the array.
   *
   * @param floatArray The float array
   * @param index The index to write to
   * @param floatValue The value to write
   */
  public static final void floatArrayWrite(float[] floatArray, int index, float floatValue) {
    UnsafeUtil.putFloat(
        floatArray, (((long) index) << FLOAT_ARRAY_SHIFT) + FLOAT_ARRAY_BASE, floatValue);
  }

  /**
   * Writes the given value at the given index in the array in a volatile fashion (see {@link
   * #putFloatVolatile(Object, long, float)})
   *
   * @param floatArray The float array
   * @param index The index to write to
   * @param floatValue The value to write
   */
  public static final void floatArrayWriteVolatile(
      float[] floatArray, int index, float floatValue) {
    UnsafeUtil.putFloatVolatile(
        floatArray, (((long) index) << FLOAT_ARRAY_SHIFT) + FLOAT_ARRAY_BASE, floatValue);
  }

  /**
   * Reads the value stored at the given index in the array.
   *
   * @param floatArray The float array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final float floatArrayRead(float[] floatArray, int index) {
    return UnsafeUtil.getFloat(
        floatArray, (((long) index) << FLOAT_ARRAY_SHIFT) + FLOAT_ARRAY_BASE);
  }

  /**
   * Reads the value stored at the given index in the array in a volatile fashion (see {@link
   * #getFloatVolatile(Object, long)}).
   *
   * @param floatArray The float array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final float floatArrayReadVolatile(float[] floatArray, int index) {
    return UnsafeUtil.getFloatVolatile(
        floatArray, (((long) index) << FLOAT_ARRAY_SHIFT) + FLOAT_ARRAY_BASE);
  }

  /** The base offset for an {@code double} array */
  public static final int DOUBLE_ARRAY_BASE = UnsafeUtil.arrayBaseOffset(double[].class);

  /** The base shift for an {@code double} array */
  public static final int DOUBLE_ARRAY_SHIFT = UnsafeUtil.arrayIndexShift(double[].class);

  /**
   * Writes the given value at the given index in the array.
   *
   * @param doubleArray The double array
   * @param index The index to write to
   * @param doubleValue The value to write
   */
  public static final void doubleArrayWrite(double[] doubleArray, int index, double doubleValue) {
    UnsafeUtil.putDouble(
        doubleArray, (((long) index) << DOUBLE_ARRAY_SHIFT) + DOUBLE_ARRAY_BASE, doubleValue);
  }

  /**
   * Writes the given value at the given index in the array in a volatile fashion.
   *
   * @param doubleArray The double array
   * @param index The index to write to
   * @param doubleValue The value to write
   */
  public static final void doubleArrayWriteVolatile(
      double[] doubleArray, int index, double doubleValue) {
    UnsafeUtil.putDoubleVolatile(
        doubleArray, (((long) index) << DOUBLE_ARRAY_SHIFT) + DOUBLE_ARRAY_BASE, doubleValue);
  }

  /**
   * Reads the value stored at the given index in the array.
   *
   * @param doubleArray The double array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final double doubleArrayRead(double[] doubleArray, int index) {
    return UnsafeUtil.getDouble(
        doubleArray, (((long) index) << DOUBLE_ARRAY_SHIFT) + DOUBLE_ARRAY_BASE);
  }

  /**
   * Reads the value stored at the given index in the array in a volatile fashion.
   *
   * @param doubleArray The double array
   * @param index The index to read from
   * @return The value stored at the given index
   */
  public static final double doubleArrayReadVolatile(double[] doubleArray, int index) {
    return UnsafeUtil.getDoubleVolatile(
        doubleArray, (((long) index) << DOUBLE_ARRAY_SHIFT) + DOUBLE_ARRAY_BASE);
  }

  /** The base offset for an {@code Object} array */
  public static final int OBJECT_ARRAY_BASE = UnsafeUtil.arrayBaseOffset(Object[].class);

  /** The index scale for an {@code Object} array */
  public static final int OBJECT_ARRAY_SCALE = UnsafeUtil.arrayIndexScale(Object[].class);

  /** The index shift for an {@code Object} array */
  public static final int OBJECT_ARRAY_SHIFT = UnsafeUtil.arrayIndexShift(Object[].class);

  /**
   * Writes the given value at the given index in the array.
   *
   * @param objectArray The Object array
   * @param index The index to write to
   * @param objectValue The value to write
   * @param <T> The type of Objects stored in the array
   */
  public static final <T> void objectArrayWrite(T[] objectArray, int index, T objectValue) {
    UnsafeUtil.putObject(
        objectArray, (((long) index) << OBJECT_ARRAY_SHIFT) + OBJECT_ARRAY_BASE, objectValue);
  }

  /**
   * Writes the given value at the given index in the array in an ordered fashion (see {@link
   * #putOrderedObject(Object, long, Object)}).
   *
   * @param objectArray The Object array
   * @param index The index to write to
   * @param objectValue The value to write
   * @param <T> The type of Objects stored in the array
   */
  public static final <T> void objectArrayWriteOrdered(T[] objectArray, int index, T objectValue) {
    UnsafeUtil.putOrderedObject(
        objectArray, (((long) index) << OBJECT_ARRAY_SHIFT) + OBJECT_ARRAY_BASE, objectValue);
  }

  /**
   * Writes the given value at the given index in the array in a volatile fashion (see {@link
   * #putObjectVolatile(Object, long, Object)})
   *
   * @param objectArray The Object array
   * @param index The index to write to
   * @param objectValue The value to write
   * @param <T> The type of Objects stored in the array
   */
  public static final <T> void objectArrayWriteVolatile(T[] objectArray, int index, T objectValue) {
    UnsafeUtil.putObjectVolatile(
        objectArray, (((long) index) << OBJECT_ARRAY_SHIFT) + OBJECT_ARRAY_BASE, objectValue);
  }

  /**
   * Reads the value stored at the given index in the array.
   *
   * @param objectArray The Object array
   * @param index The index to read from
   * @param <T> The type of Objects stored in the array
   * @return The value stored at the given index
   */
  @SuppressWarnings("unchecked")
  public static final <T> T objectArrayRead(T[] objectArray, int index) {
    return (T)
        UnsafeUtil.getObject(
            objectArray, (((long) index) << OBJECT_ARRAY_SHIFT) + OBJECT_ARRAY_BASE);
  }

  /**
   * Reads the value stored at the given index in the array in a volatile fashion (see {@link
   * #getObjectVolatile(Object, long)}).
   *
   * @param objectArray The Object array
   * @param index The index to read from
   * @param <T> The type of Objects stored in the array
   * @return The value stored at the given index
   */
  @SuppressWarnings("unchecked")
  public static final <T> T objectArrayReadVolatile(T[] objectArray, int index) {
    return (T)
        UnsafeUtil.getObjectVolatile(
            objectArray, (((long) index) << OBJECT_ARRAY_SHIFT) + OBJECT_ARRAY_BASE);
  }

  ////////////////////////////////////////////////////
  // Example utility methods.                       //
  // These methods are declared private             //
  // as they are simply intended to demonstrate     //
  // the usage of the public functions in the code. //
  ////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  private static int arrayIndexExample(Class<?> arrayClass, int index) {
    // WARNING: this does no boundary checks and therefore could read/write
    //         some random memory.
    final int arrayBase = arrayBaseOffset(arrayClass);
    final int arrayIndexShift = arrayIndexShift(arrayClass);
    return (index << arrayIndexShift) + arrayBase;
  }
}
