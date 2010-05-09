/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests for comparing documents.
 */
public class DescriptorComparisonTest {

  /**
   * Test that a different number of performance goals is caught as an error.
   */
  @Test
  public void testDifferentNumPerfGoals() {
      final InputStream curDocStream = DescriptorComparisonTest.class.getResourceAsStream("data/import-document.xml");
      Assert.assertNotNull(curDocStream);
      final Document curDoc = ChallengeParser.parse(new InputStreamReader(curDocStream));
      Assert.assertNotNull(curDoc);
      final InputStream newDocStream = DescriptorComparisonTest.class.getResourceAsStream("data/import-document-diff-num-perf-goals.xml");
      Assert.assertNotNull(newDocStream);
      final Document newDoc = ChallengeParser.parse(new InputStreamReader(newDocStream));
      final String message = ChallengeParser.compareStructure(curDoc, newDoc);
      Assert.assertNotNull("There should be differences", message);
  }

  /**
   * Test that a different performance goal name is an error.
   */
  @Test
  public void testDifferentGoalName() {
      final InputStream curDocStream = DescriptorComparisonTest.class.getResourceAsStream("data/import-document.xml");
      Assert.assertNotNull(curDocStream);
      final Document curDoc = ChallengeParser.parse(new InputStreamReader(curDocStream));
      Assert.assertNotNull(curDoc);
      final InputStream newDocStream = DescriptorComparisonTest.class.getResourceAsStream("data/id-diff-goal.xml");
      Assert.assertNotNull(newDocStream);
      final Document newDoc = ChallengeParser.parse(new InputStreamReader(newDocStream));
      final String message = ChallengeParser.compareStructure(curDoc, newDoc);
      Assert.assertNotNull("There should be differences", message);
  }
 
  /**
   * Test that enumerated vs. non-enumerated performance goal name is an error. 
   */
  
  /**
   * Test that number of computed performance goals is NOT an error.
   */
  
  /**
   * Test different number of subjective categories is an error.
   */
  
  
}
