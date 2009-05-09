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
 * @version $Revision$
 */
public class FP {

  private FP() {
    // no instances
  }

  /**
   * Check if two values are equal given a tolerance.
   */
  public static boolean equals(final double a, final double b, final double tolerance) {
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
   */
  public static boolean greaterThan(final double a, final double b, final double tolerance) {
    if (isFinite(a)
        && isFinite(b)) {
      return (a - b) > tolerance;
    } else {
      return a > b;
    }
  }

  /**
   * Check for less than, calls greater than with params swapped.
   */
  public static boolean lessThan(final double a, final double b, final double tolerance) {
    return greaterThan(b, a, tolerance);
  }

  /**
   * Check that the value is not {@link Double#NaN} and is
   * not {@link Double#isInfinite(double)}.
   */
  public static boolean isFinite(final double value) {
    return !Double.isInfinite(value)
        && !Double.isNaN(value);
  }
}
