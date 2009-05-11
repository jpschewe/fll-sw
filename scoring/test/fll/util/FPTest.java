/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;


import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link FP}.
 */
public class FPTest {
  
  @Test
  public void testIsFinite0() {
    Assert.assertTrue(FP.isFinite(10D));
    Assert.assertTrue(FP.isFinite(Double.MAX_VALUE));
    Assert.assertTrue(FP.isFinite(Double.MIN_VALUE));
    
    Assert.assertFalse(FP.isFinite(Double.NEGATIVE_INFINITY));
    Assert.assertFalse(FP.isFinite(Double.POSITIVE_INFINITY));
    Assert.assertFalse(FP.isFinite(Double.NaN));
  }
  
  @Test
  public void testEquals0() {
    Assert.assertTrue(FP.equals(10D, 10D, 0D));
    
    Assert.assertTrue(FP.equals(10.1D, 10D, 1D));
    Assert.assertTrue(FP.equals(10.00001D, 10D, 1E-4));
    
    Assert.assertTrue(FP.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0D));
    
    Assert.assertFalse(FP.equals(Double.POSITIVE_INFINITY, 10D, 0D));
    Assert.assertFalse(FP.equals(10D, Double.POSITIVE_INFINITY, 0D));
  }
  
  @Test
  public void testGreaterThan0() {
    Assert.assertTrue(FP.greaterThan(10D, 1D, 0D));
    Assert.assertTrue(FP.greaterThan(10.0001D, 10D, 1E-5));
    Assert.assertFalse(FP.greaterThan(10.0001D, 10D, 1E-4));
    Assert.assertTrue(FP.greaterThan(Double.POSITIVE_INFINITY, 10D, 0D));
    Assert.assertTrue(FP.greaterThan(10D, Double.NEGATIVE_INFINITY, 0D));
  }

  @Test
  public void testLessThan0() {
    Assert.assertFalse(FP.lessThan(10D, 1D, 0D));
    Assert.assertFalse(FP.lessThan(10.0001D, 10D, 1E-5));
    Assert.assertFalse(FP.lessThan(10D, 10.0001D, 1E-4));
    Assert.assertFalse(FP.lessThan(Double.POSITIVE_INFINITY, 10D, 0D));
    Assert.assertFalse(FP.lessThan(10D, Double.NEGATIVE_INFINITY, 0D));
  }
  
  
}
