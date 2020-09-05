/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

/**
 * Class to handle floating point comparisons.
 * 
 * @author jpschewe
 */
public final class FP {

  private FP() {
    // no instances
  }

  /**
   * Check if two values are equal given a tolerance.
   * 
   * @param a left hand side
   * @param b right hand side
   * @param tolerance how close the values need to be
   * @return true if {@code a} and {@code b} are within {@code tolerance} of each
   *         other
   */
  public static boolean equals(final double a,
                               final double b,
                               final double tolerance) {
    if (isFinite(a)
        && isFinite(b)) {
      return Math.abs(a
          - b) <= tolerance;
    } else {
      return a == b;
    }
  }

  /**
   * Check for greater than.
   * 
   * @param a left hand side
   * @param b right hand side
   * @param tolerance how close the values need to be
   * @return is {@code a} greater than {@code b} by {@code tolerance}.
   */
  public static boolean greaterThan(final double a,
                                    final double b,
                                    final double tolerance) {
    if (isFinite(a)
        && isFinite(b)) {
      return (a
          - b) > tolerance;
    } else {
      return a > b;
    }
  }

  /**
   * Check for greater than or equal.
   * 
   * @param a left hand side
   * @param b right hand side
   * @param tolerance how close the values need to be
   * @return is {@code a} greater than {@code b} by {@code tolerance} or is
   *         {@code a} within {@code tolerance} of {@code b}
   */
  public static boolean greaterThanOrEqual(final double a,
                                           final double b,
                                           final double tolerance) {
    return greaterThan(a, b, tolerance)
        || equals(a, b, tolerance);
  }

  /**
   * Check for less than, calls greater than with params swapped.
   * 
   * @param a left hand side
   * @param b right hand side
   * @param tolerance how close the values need to be
   * @return is {@code a} less than {@code b} by {@code tolerance}
   */
  public static boolean lessThan(final double a,
                                 final double b,
                                 final double tolerance) {
    return greaterThan(b, a, tolerance);
  }

  /**
   * Check for less than or equal.
   * 
   * @param a left hand side
   * @param b right hand side
   * @param tolerance how close the values need to be
   * @return is {@code a} less than {@code b} by {@code tolerance} or is {@code a}
   *         within {@code tolerance} of {@code b}
   */
  public static boolean lessThanOrEqual(final double a,
                                        final double b,
                                        final double tolerance) {
    return lessThan(a, b, tolerance)
        || equals(a, b, tolerance);
  }

  /**
   * Check that the value is not {@link Double#NaN} and is
   * not {@link Double#isInfinite(double)}.
   * 
   * @param value the value to check
   * @return if the value is finite (not infinity and not NaN)
   * @see Double#isInfinite(double)
   * @see Double#isNaN(double)
   */
  public static boolean isFinite(final double value) {
    return !Double.isInfinite(value)
        && !Double.isNaN(value);
  }
}
