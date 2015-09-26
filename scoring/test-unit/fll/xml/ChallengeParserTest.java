/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.util.LogUtils;

/**
 * Test various aspects of the XML document parsing.
 */
public class ChallengeParserTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Load illegal-restriction.xml and ensure an exception is thrown on the
   * illegal reference. This checks for a bad reference in a term element of a
   * restriction.
   */
  @Test
  public void testIllegalRestriction() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-restriction.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final RuntimeException e) {
      if (e.getMessage().contains("cvc-identity-constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to a missing reference", exception);
  }

  /**
   * Load illegal-tiebreaker-ref.xml and ensure an exception is thrown on the
   * illegal reference. This checks for a bad reference in a term element of a
   * tiebreaker.
   */
  @Test
  public void testIllegalTiebreakerRef() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-tiebreaker-ref.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-identity-constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to a missing reference in a tiebreaker", exception);
  }

  /**
   * Load all-elements.xml and make sure there are no errors.
   */
  @Test
  public void testAllElements() {
    final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/all-elements.xml");
    Assert.assertNotNull(stream);
    ChallengeParser.parse(new InputStreamReader(stream));
  }

  /**
   * Load illegal-computed-enumgoal-ref.xml and ensure an exception is thrown on
   * the illegal reference. This checks for a bad reference in a goalRef element
   * of a computed goal.
   */
  @Test
  public void testIllegalComputedEnumGoal() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-computed-enumgoal-ref.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-identity-constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to a missing reference", exception);
  }

  /**
   * Load illegal-computed-goal-ref.xml and ensure an exception is thrown on the
   * illegal reference. This checks for a bad reference in a term element of a
   * computed goal.
   */
  @Test
  public void testIllegalComputedGoal() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-computed-goal-ref.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-identity-constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to a missing reference", exception);
  }

  /**
   * Load subjective-computed-goal-ref-other-category. This ensure that a
   * computed goal in one subjective category cannot reference a goal in another
   * subjective category.
   */
  @Test
  public void testSubjectiveComputedRefOtherCategory() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/subjective-computed-goal-ref-other-category.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-identity-constraint")
          && e.getMessage().contains("performanceGoalRef")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to a missing reference", exception);
  }

  /**
   * Load subjective-duplicate-goals.xml. This checks if two goals having the
   * same name in the same category is caught as an error.
   */
  @Test
  public void testDuplicateSubjectiveGoals() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/subjective-duplicate-goals.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-identity-constraint")
          && e.getMessage().contains("Duplicate key value") && e.getMessage().contains("subjectiveGoalKey")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to duplicate goals", exception);
  }

  /**
   * Load subjective-two-categories-same-goal.xml. This ensure that two
   * subjective categories can have goals with the same name.
   */
  @Test
  public void testSubjectiveSameGoal() {
    final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/subjective-two-categories-same-goal.xml");
    Assert.assertNotNull(stream);
    ChallengeParser.parse(new InputStreamReader(stream));
  }

  /**
   * Check that enum goals referenced in a polynomial of a computed goal use the
   * computed score and not the raw score.
   */
  @Test
  public void testEnumRawScoreComputed() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/enum-using-raw-score-computed.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final IllegalScoreTypeUseException e) {
      exception = true;
    }
    Assert.assertTrue("Expected a runtime exception due a reference to the raw score of an enum in a computed goal polynomial",
                      exception);
  }

  /**
   * Check that enum goals referenced in a polynomial of a tiebreaker use the
   * computed score and not the raw score.
   */
  @Test
  public void testEnumRawScoreTiebreaker() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/enum-using-raw-score-tiebreaker.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final IllegalScoreTypeUseException e) {
      exception = true;
    }
    Assert.assertTrue("Expected a runtime exception due a reference to the raw score of an enum in a tiebreaker polynomial",
                      exception);
  }

  /**
   * Check that enum goals referenced in a polynomial of a restriction use the
   * computed score and not the raw score.
   */
  @Test
  public void testEnumRawScoreRestriction() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/enum-using-raw-score-restriction.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final IllegalScoreTypeUseException e) {
      exception = true;
    }
    Assert.assertTrue("Expected a runtime exception due a reference to the raw score of an enum in a restriction polynomial",
                      exception);
  }

  /**
   * Check that a regular goal cannot show up in an enum condition.
   */
  @Test
  public void testNonEnumInEnumCond() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/non-enum-in-enumcond.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final InvalidEnumCondition e) {
      exception = true;
    }
    Assert.assertTrue("Expected a runtime exception due a reference to non-enum goal inside enumCond", exception);
  }

  /**
   * Check that setting an initial value below the minimum for a goal fails.
   */
  @Test
  public void testInitialValueBelowMin() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/initial-value-below-min.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final InvalidInitialValue e) {
      exception = true;
    }
    Assert.assertTrue("Expected a runtime exception due the initial value being below the min", exception);
  }

  /**
   * Check that setting an initial value above the maximum for a goal fails.
   */
  @Test
  public void testInitialValueAboveMax() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/initial-value-above-max.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final InvalidInitialValue e) {
      exception = true;
    }
    Assert.assertTrue("Expected a runtime exception due the initial value being above the max", exception);
  }

  /**
   * Check that setting an initial value for an enum goal that doesn't match an
   * enum value fails.
   */
  @Test
  public void testInitialValueEnumNoMatch() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/initial-value-enum-no-match.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final InvalidInitialValue e) {
      exception = true;
    }
    Assert.assertTrue("Expected a runtime exception due the initial value being set to something that doesn't match an enum value",
                      exception);
  }

  /**
   * Check that a variableRef is not allowed in a tiebreaker.
   */
  @Test
  public void testVariableRefInTiebreaker() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/variableRef-in-tiebreaker.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-complex-type")
          && e.getMessage().contains("variableRef")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to a variableRef in a tiebreaker", exception);
  }

  /**
   * Check that a variableRef is not allowed in a restriction.
   */
  @Test
  public void testVariableRefInRestriction() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/variableRef-in-restriction.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-complex-type")
          && e.getMessage().contains("variableRef")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to a variableRef in a restriction", exception);
  }

  /**
   * Check that a two variables in a computed goal may not have the same name.
   */
  @Test
  public void testDuplicateVariable() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/duplicate-variable.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-identity-constraint")
          && e.getMessage().contains("Duplicate key value")) {
        exception = true;
      } else {
        throw e;
      }
    }
    Assert.assertTrue("Expected a runtime exception due to two variables having the same name in a computed goal.",
                      exception);
  }

  /**
   * Check that all known challenge descriptors are still valid.
   * 
   * @throws IOException
   */
  @Test
  public void testAllDescriptors() throws IOException {
    final Collection<URL> urls = XMLUtils.getAllKnownChallengeDescriptorURLs();

    for (final URL u : urls) {
      LOGGER.info("Challenge: "
          + u.toString());

      final InputStream stream = u.openStream();
      final Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET);
      final Document document = ChallengeParser.parse(reader);
      reader.close();
      new ChallengeDescription(document.getDocumentElement());
    }
  }

  /**
   * Check that computed goals referencing computed goals is OK.
   */
  @Test
  public void testComputedGoalReference() {
    final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/computed-goal-reference.xml");
    Assert.assertNotNull(stream);
    ChallengeParser.parse(new InputStreamReader(stream));
  }

  /**
   * Check that computed goals with circular reference are not allowed.
   */
  @Test
  public void testCircularComputedGoalReference() {
    boolean exception = false;
    try {
      final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/computed-goal-circular-reference.xml");
      Assert.assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream));
    } catch (final CircularComputedGoalException e) {
      exception = true;
    }
    Assert.assertTrue("Expected an exception due to circular references.", exception);
  }

}
