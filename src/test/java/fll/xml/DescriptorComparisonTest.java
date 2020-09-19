/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fll.TestUtils;
import fll.Utilities;

/**
 * Tests for comparing documents.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class DescriptorComparisonTest {

  /**
   * Test that a different number of performance goals is caught as an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentNumPerfGoals() throws IOException {
    doComparison("data/import-document.xml", "data/import-document-diff-num-perf-goals.xml", true);
  }

  /**
   * Test that a different performance goal name is an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentPerfGoalName() throws IOException {
    doComparison("data/import-document.xml", "data/id-diff-goal.xml", true);
  }

  /**
   * Test that enumerated vs. non-enumerated performance goal name is an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentPerfGoalTypes() throws IOException {
    doComparison("data/import-document.xml", "data/id-non-enum.xml", true);
  }

  /**
   * Test that number of computed performance goals is NOT an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentNumComputedGoals() throws IOException {
    doComparison("data/import-document.xml", "data/id-diff-computed-goals.xml", false);
  }

  /**
   * Test different number of subjective categories is an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentNumSubjCats() throws IOException {
    doComparison("data/import-document.xml", "data/id-diff-num-subj-cats.xml", true);
  }

  /**
   * Test different name of subjective categories is an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentNameSubjCats() throws IOException {
    doComparison("data/import-document.xml", "data/id-diff-name-subj-cats.xml", true);
  }

  /**
   * Test that a different subjective goal name is an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentNumSubjGoals() throws IOException {
    doComparison("data/import-document.xml", "data/id-diff-num-subj-goal.xml", true);
  }

  /**
   * Test that a different subjective goal type is an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDifferentTypeSubjGoals() throws IOException {
    doComparison("data/import-document.xml", "data/id-diff-type-subj-goal.xml", true);
  }

  private void doComparison(final String curDocRes,
                            final String newDocRes,
                            final boolean differencesExpected)
      throws IOException {
    try (InputStream curDocStream = DescriptorComparisonTest.class.getResourceAsStream(curDocRes)) {
      assertNotNull(curDocStream, "Could not find '"
          + curDocRes
          + "'");
      final ChallengeDescription curDescription = ChallengeParser.parse(new InputStreamReader(curDocStream,
                                                                                             Utilities.DEFAULT_CHARSET));
      assertNotNull(curDescription, "Error parsing '"
          + curDocRes
          + "'");
      try (InputStream newDocStream = DescriptorComparisonTest.class.getResourceAsStream(newDocRes)) {
        assertNotNull(newDocStream, "Could not find '"
            + newDocRes
            + "'");
        final ChallengeDescription newDescription = ChallengeParser.parse(new InputStreamReader(newDocStream,
                                                                                                Utilities.DEFAULT_CHARSET));
        assertNotNull(newDescription, "Error parsing '"
            + newDocRes
            + "'");

        final String message = ChallengeParser.compareStructure(curDescription, newDescription);
        if (differencesExpected) {
          assertNotNull(message, "There should be differences");
        } else {
          assertNull(message, "There should NOT be differences");
        }
      } // newDocStream
    } // curDocStream
  }
}
