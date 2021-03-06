/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fll.TestUtils;

/**
 * Tests for {@link FP}.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class FPTest {

  /**
   * Test {@link FP#isFinite(double)}.
   */
  @Test
  public void testIsFinite0() {
    assertTrue(FP.isFinite(10D));
    assertTrue(FP.isFinite(Double.MAX_VALUE));
    assertTrue(FP.isFinite(Double.MIN_VALUE));

    assertFalse(FP.isFinite(Double.NEGATIVE_INFINITY));
    assertFalse(FP.isFinite(Double.POSITIVE_INFINITY));
    assertFalse(FP.isFinite(Double.NaN));
  }

  /**
   * Test {@link FP#equals(double, double, double)}.
   */
  @Test
  public void testEquals0() {
    assertTrue(FP.equals(10D, 10D, 0D));

    // CHECKSTYLE:OFF constants for testing tolerance
    assertTrue(FP.equals(10.1D, 10D, 1D));
    assertTrue(FP.equals(10.00001D, 10D, 1E-4));
    // CHECKSTYLE:ON

    assertTrue(FP.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0D));

    assertFalse(FP.equals(Double.POSITIVE_INFINITY, 10D, 0D));
    assertFalse(FP.equals(10D, Double.POSITIVE_INFINITY, 0D));
  }

  /**
   * Test {@link FP#greaterThan(double, double, double)}.
   */
  @Test
  public void testGreaterThan0() {
    assertTrue(FP.greaterThan(10D, 1D, 0D));

    // CHECKSTYLE:OFF constants for testing tolerance
    assertTrue(FP.greaterThan(10.0001D, 10D, 1E-5));
    assertFalse(FP.greaterThan(10.0001D, 10D, 1E-4));
    // CHECKSTYLE:ON

    assertTrue(FP.greaterThan(Double.POSITIVE_INFINITY, 10D, 0D));
    assertTrue(FP.greaterThan(10D, Double.NEGATIVE_INFINITY, 0D));
  }

  /**
   * Test {@link FP#lessThan(double, double, double)}.
   */
  @Test
  public void testLessThan0() {
    assertFalse(FP.lessThan(10D, 1D, 0D));

    // CHECKSTYLE:OFF constants for testing tolerance
    assertFalse(FP.lessThan(10.0001D, 10D, 1E-5));
    assertFalse(FP.lessThan(10D, 10.0001D, 1E-4));
    // CHECKSTYLE:ON

    assertFalse(FP.lessThan(Double.POSITIVE_INFINITY, 10D, 0D));
    assertFalse(FP.lessThan(10D, Double.NEGATIVE_INFINITY, 0D));
  }

}
