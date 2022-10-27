/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.platform;

import com.sun.jna.Native;
import java.util.Collections;
import java.util.Map;

/**
 * Simple wrapper around {@link Native} and its versions of loadLibrary(..).
 *
 * <p>This ensures that the class {@link Native} itself is properly initialized. Otherwise, any
 * thrown Exception may be misinterpreted as loading failure of the requested library.
 */
public class SaferNative {

  private SaferNative() {}

  static {
    // Execute a simple call on the Native class to force its initialization
    // If the class could not be properly initialize, it would throw before any important call.
    try {
      Native.isProtected();
    } catch (RuntimeException e) {
      throw new RuntimeException("Failed to initialize the class " + Native.class.getName(), e);
    }
  }

  /**
   * Map a library interface to the given shared library, providing the explicit interface class.
   *
   * <p>If <code>name</code> is null, attempts to map onto the current process. Native libraries
   * loaded via this method may be found in <a
   * href="NativeLibrary.html#library_search_paths">several locations</a>.
   *
   * @param <T> Types of expected wrapper
   * @param name Library base name
   * @param interfaceClass The implementation wrapper interface
   * @return an instance of the requested interface, mapped to the indicated native library.
   * @throws UnsatisfiedLinkError if the library cannot be found or dependent libraries are missing.
   * @see #loadLibrary(String, Class, Map)
   */
  public static <T> T loadLibrary(String name, Class<T> interfaceClass)
      throws UnsatisfiedLinkError {
    return Native.loadLibrary(name, interfaceClass, Collections.emptyMap());
  }

  /**
   * Load a library interface from the given shared library, providing the explicit interface class
   * and a map of options for the library.
   *
   * <p>If no library options are detected the map is interpreted as a map of Java method names to
   * native function names.
   *
   * <p>If <code>name</code> is null, attempts to map onto the current process. Native libraries
   * loaded via this method may be found in <a
   * href="NativeLibrary.html#library_search_paths">several locations</a>.
   *
   * @param <T> Types of expected wrapper
   * @param name Library base name
   * @param interfaceClass The implementation wrapper interface
   * @param options Map of library options
   * @return an instance of the requested interface, mapped to the indicated native library.
   * @throws UnsatisfiedLinkError if the library cannot be found or dependent libraries are missing.
   */
  public static <T> T loadLibrary(String name, Class<T> interfaceClass, Map<String, ?> options)
      throws UnsatisfiedLinkError {
    return Native.loadLibrary(name, interfaceClass, options);
  }

  /**
   * Retrieve last error set by the OS.
   *
   * <p>This corresponds to <code>GetLastError()</code> on Windows, and <code>errno</code> on most
   * other platforms. The value is preserved per-thread, but whether the original value is
   * per-thread depends on the underlying OS.
   *
   * @return the last error
   */
  public static int getLastError() {
    return Native.getLastError();
  }
}
