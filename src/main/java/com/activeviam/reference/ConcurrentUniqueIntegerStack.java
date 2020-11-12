package com.activeviam.reference;

import com.activeviam.UnsafeUtil;
import java.util.AbstractCollection;
import java.util.Arrays;

/**
 * A stack that accepts an element only if the element is not already in the stack i.e if two
 * concurrent calls try to insert the same element in the stack, only one will succeed.
 *
 * @author ActiveViam
 */
public class ConcurrentUniqueIntegerStack {

  /**
   * We use a long to take advantages of its extra bits for ABA protection
   *
   * @see #tagHead(long, int)
   */
  protected volatile long head;

  /**
   * Array that stores stack elements. There are four types of values:
   *
   * <ul>
   *   <li>{@link #free}
   *   <li>{@link #tail}
   *   <li>a positive integer
   * </ul>
   */
  protected int[] table;

  /**
   * The value of a {@link #table} element indicating a positive value can be stored in place of it.
   */
  protected static final int free = -1; // must be an negative value

  /**
   * Must be a positive integer that will not be used as an array index.
   *
   * <p>See {@link AbstractCollection} <code>MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;</code>
   */
  protected static final int tail = Integer.MAX_VALUE;

  /**
   * The value to use after calling {@link #pop()} to check if the stack is empty. In such a case,
   * the value returned by {@link #pop()} is equal to this value.
   */
  public static final int NULL = tail;

  /**
   * Constuctor.
   *
   * @param capacity the maximum capacity of this stack.
   */
  public ConcurrentUniqueIntegerStack(int capacity) {
    this.table = new int[capacity];
    this.head = tail; // initial value
    Arrays.fill(table, free);
  }

  /**
   * Add 1 to head value (protected from ABA problem).
   *
   * @param h the current head (tagged)
   * @param uh the new head to set (untagged)
   * @return the tagged value of the head
   */
  protected long tagHead(long h, int uh) {
    return pack(unpack1(h) + 1, uh);
  }

  /**
   * Sugar syntax for <code>Bits.unpack2(h)</code>
   *
   * @param h the current tagged head
   * @return the untagged value of the head
   */
  protected int untagHead(long h) {
    return unpack2(h);
  }

  /**
   * Push a new element in the stack. The value of the element must be between 0 (inclusive) and the
   * capacity of the stack (exclusive).
   *
   * @param elem the element to push in the stack
   * @return true if the element has been added to the stack (i.e it was not initially present),
   *     false otherwise.
   */
  public boolean push(final int elem) {
    // elem is likely to become head
    long h = this.head;
    int uh = untagHead(h);

    // Note: only one CAS on the array element will be done per push operation.
    if (!compareAndSet(table, elem, free, uh)) return false; // Someone has put elem before me.

    while (!casHead(
        this,
        h /* Use the tagged value for CAS (ABA protection) */,
        tagHead(
            h, elem) /* If the CAS succeeds, set the new head with the tag value incremented */)) {
      h = head; // Read the head, can be tagged
      uh = untagHead(h);

      // CAS failed, the head has changed so the table
      // value needs to be updated.
      // The happens-before relationship guarantee this write
      // is visible once the CAS on the head succeeds.
      table[elem] = uh;
    }

    return true;
  }

  /**
   * Pop the head of the list and change it with the next one. If the stack is empty, it returns
   * {@link #NULL}.
   *
   * @return the popped head.
   */
  public int pop() {
    // Pop the top element
    long h;
    int newh = tail;
    int uh;
    do {
      h = head; // read the head

      uh = untagHead(h); // read the untagged head
      if (uh != tail) {
        newh = getNewHead(uh); // New head
      } else {
        return NULL;
      }
    } while (!casHead(
        this,
        h /* Use the tagged value for CAS (ABA protection) */,
        newh /* Don't need to tag the head when popping */));

    // At this point, the element at index uh is not anymore accessible from the
    // linked list so we can safely change the value (no Free can occur).
    // The value at index uh may not be visible by other threads at this point.
    // However, the value is not accessible from the head so a pop operation
    // cannot take place. A push operation may not see that the value at index uh
    // is freed so it will returned immediately.
    // To sum up, it is OK to do this.
    table[uh] = free;

    return uh;
  }

  // For testing purpose

  /**
   * @param uh the index
   * @return the value at the given index
   */
  protected int getNewHead(int uh) {
    return table[uh];
  }

  protected static final long headOffset =
      UnsafeUtil.getFieldOffset(ConcurrentUniqueIntegerStack.class, "head");

  /**
   * Static wrappers for UNSAFE methods {@link UnsafeUtil#compareAndSwapInt(Object, long, int,
   * int)}.
   *
   * <p>Hopefully it will be in-lined ...
   *
   * @param table the array
   * @param i the index to CAS
   * @param expect the expected value
   * @param update the new value
   * @return true if successful. false return indicates that the actual value was not equal to the
   *     expected value.
   */
  protected static final boolean compareAndSet(int[] table, int i, int expect, int update) {
    return UnsafeUtil.compareAndSwapInt(
        table,
        (((long) i) << UnsafeUtil.INT_ARRAY_SHIFT) + UnsafeUtil.INT_ARRAY_BASE,
        expect,
        update);
  }

  /**
   * Static wrappers for UNSAFE methods {@link UnsafeUtil#compareAndSwapLong(Object, long, long,
   * long)} with <code>offset = </code>{@link #headOffset}.
   *
   * <p>Hopefully it will be in-lined ...
   *
   * @param stack the {@link ConcurrentUniqueIntegerStack}
   * @param expected the expected value
   * @param updated the new value
   * @return true if successful. false return indicates that the actual value was not equal to the
   *     expected value.
   */
  protected static final boolean casHead(
      final ConcurrentUniqueIntegerStack stack, final long expected, final long updated) {
    return UnsafeUtil.compareAndSwapLong(stack, headOffset, expected, updated);
  }

  /**
   * Returns the current number of element. The returned value is <em>NOT</em> an atomic snapshot;
   * invocation in the absence of concurrent updates returns an accurate result, but concurrent
   * updates that occur while the size is being calculated might not be incorporated.
   *
   * @return the number of element in the stack.
   */
  public int size() {
    int uh = untagHead(head);
    int size = 0;
    int i = uh;
    if (i != tail) {
      do {
        i = table[i];
        size++;
      } while (i != tail);
    }
    return size;
  }

  @Override
  public String toString() {
    int uh = untagHead(head);
    int size = size();

    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName())
        .append(" [head=")
        .append(uh)
        .append(", table=")
        .append("(capacity=")
        .append(table.length)
        .append("; size=")
        .append(size)
        .append(")")
        //				.append(Arrays.toString(table))
        .append("]");
    return sb.toString();
  }

  /**
   * Pack two integers in a long
   *
   * @param i1 first value to pack
   * @param i2 second value to pack
   * @return long value representing two integers
   */
  private static long pack(int i1, int i2) {
    long packed1 = ((long) i1) << 32;
    long packed2 = i2 & 0xFFFFFFFFL;
    return packed1 | packed2;
  }

  /**
   * Unpacks first value packed by {@link #pack(int, int)}.
   *
   * @param packed packed value
   * @return unpacked first value
   */
  private static int unpack1(long packed) {
    return (int) (packed >>> 32);
  }

  /**
   * Unpacks second value packed by {@link #pack(int, int)}.
   *
   * @param packed packed value
   * @return unpacked second value
   */
  private static int unpack2(long packed) {
    return (int) (packed & 0xFFFFFFFFL);
  }
}
