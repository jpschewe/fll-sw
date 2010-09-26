/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.util.LogUtils;

/**
 * Tests for comparing documents.
 */
public class DescriptorComparisonTest {

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Test that a different number of performance goals is caught as an error.
   */
  @Test
  public void testDifferentNumPerfGoals() {
    doComparison("data/import-document.xml", "data/import-document-diff-num-perf-goals.xml", true);
  }

  /**
   * Test that a different performance goal name is an error.
   */
  @Test
  public void testDifferentPerfGoalName() {
    doComparison("data/import-document.xml", "data/id-diff-goal.xml", true);
  }
 
  /**
   * Test that enumerated vs. non-enumerated performance goal name is an error. 
   */
  @Test
  public void testDifferentPerfGoalTypes() {
    doComparison("data/import-document.xml", "data/id-non-enum.xml", true);
  }
  
  /**
   * Test that number of computed performance goals is NOT an error.
   */
  @Test
  public void testDifferentNumComputedGoals() {
    doComparison("data/import-document.xml", "data/id-diff-computed-goals.xml", false);
  }
    
  
  /**
   * Test different number of subjective categories is an error.
   */
  @Test
  public void testDifferentNumSubjCats() {
    doComparison("data/import-document.xml", "data/id-diff-num-subj-cats.xml", true);
  }
   
  /**
   * Test different name of subjective categories is an error.
   */
  @Test
  public void testDifferentNameSubjCats() {
    doComparison("data/import-document.xml", "data/id-diff-name-subj-cats.xml", true);
  }
  
  
  /**
   * Test that a different subjective goal name is an error.
   */
  @Test
  public void testDifferentNumSubjGoals() {
    doComparison("data/import-document.xml", "data/id-diff-num-subj-goal.xml", true);
  }
  
  /**
   * Test that a different subjective goal type is an error.
   */
  @Test
  public void testDifferentTypeSubjGoals() {
    doComparison("data/import-document.xml", "data/id-diff-type-subj-goal.xml", true);
  }
  
  private void doComparison(final String curDocRes, final String newDocRes, final boolean differencesExpected) {
    final InputStream curDocStream = DescriptorComparisonTest.class.getResourceAsStream(curDocRes);
    Assert.assertNotNull("Could not find '" + curDocRes + "'", curDocStream);
    final Document curDoc = ChallengeParser.parse(new InputStreamReader(curDocStream));
    Assert.assertNotNull("Error parsing '" + curDocRes + "'", curDoc);
    final InputStream newDocStream = DescriptorComparisonTest.class.getResourceAsStream(newDocRes);
    Assert.assertNotNull("Could not find '" + newDocRes + "'", newDocStream);
    final Document newDoc = ChallengeParser.parse(new InputStreamReader(newDocStream));    
    Assert.assertNotNull("Error parsing '" + newDocRes + "'", newDoc);
    
    final String message = ChallengeParser.compareStructure(curDoc, newDoc);
    if(differencesExpected) {
      Assert.assertNotNull("There should be differences", message);
    } else {
      Assert.assertNull("There should NOT be differences", message);
    }
  }
}
