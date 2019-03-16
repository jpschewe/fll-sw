/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;

import fll.TestUtils;
import fll.Utilities;
import fll.util.LogUtils;

/**
 * Test various aspects of the XML document parsing.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class ChallengeParserTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Load illegal-restriction.xml and ensure an exception is thrown on the
   * illegal reference. This checks for a bad reference in a term element of a
   * restriction.
   * 
   * @throws IOException Test error
   */
  @Test
  public void testIllegalRestriction() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-restriction.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final RuntimeException e) {
      if (e.getMessage().contains("not found for identity constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to a missing reference");
  }

  /**
   * Load illegal-tiebreaker-ref.xml and ensure an exception is thrown on the
   * illegal reference. This checks for a bad reference in a term element of a
   * tiebreaker.
   * 
   * @throws IOException test error
   */
  @Test
  public void testIllegalTiebreakerRef() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-tiebreaker-ref.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("not found for identity constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to a missing reference in a tiebreaker");
  }

  /**
   * Load all-elements.xml and make sure there are no errors.
   * 
   * @throws IOException test error
   */
  @Test
  public void testAllElements() throws IOException {
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/all-elements.xml")) {
      assertNotNull(stream);
      try (InputStreamReader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET)) {
        ChallengeParser.parse(reader);
      }
    }
  }

  /**
   * Load illegal-computed-enumgoal-ref.xml and ensure an exception is thrown on
   * the illegal reference. This checks for a bad reference in a goalRef element
   * of a computed goal.
   * 
   * @throws IOException test error
   */
  @Test
  public void testIllegalComputedEnumGoal() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-computed-enumgoal-ref.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("not found for identity constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to a missing reference");
  }

  /**
   * Load illegal-computed-goal-ref.xml and ensure an exception is thrown on the
   * illegal reference. This checks for a bad reference in a term element of a
   * computed goal.
   * 
   * @throws IOException test error
   */
  @Test
  public void testIllegalComputedGoal() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/illegal-computed-goal-ref.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("not found for identity constraint")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to a missing reference");
  }

  /**
   * Load subjective-computed-goal-ref-other-category. This ensure that a
   * computed goal in one subjective category cannot reference a goal in another
   * subjective category.
   * 
   * @throws IOException test error
   */
  @Test
  public void testSubjectiveComputedRefOtherCategory() throws IOException {
    boolean exception = false;
    try (
        InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/subjective-computed-goal-ref-other-category.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("not found for identity constraint")
          && e.getMessage().contains("Performance")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to a missing reference");
  }

  /**
   * Load subjective-duplicate-goals.xml. This checks if two goals having the
   * same name in the same category is caught as an error.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDuplicateSubjectiveGoals() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/subjective-duplicate-goals.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("Duplicate key value")
          && e.getMessage().contains("subjectiveCategory")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to duplicate goals");
  }

  /**
   * Load subjective-two-categories-same-goal.xml. This ensure that two
   * subjective categories can have goals with the same name.
   * 
   * @throws IOException test error
   */
  @Test
  public void testSubjectiveSameGoal() throws IOException {
    try (
        InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/subjective-two-categories-same-goal.xml")) {
      assertNotNull(stream);
      try (InputStreamReader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET)) {
        ChallengeParser.parse(reader);
      }
    }
  }

  /**
   * Check that enum goals referenced in a polynomial of a computed goal use the
   * computed score and not the raw score.
   * 
   * @throws IOException test error
   */
  @Test
  public void testEnumRawScoreComputed() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/enum-using-raw-score-computed.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final IllegalScoreTypeUseException e) {
      exception = true;
    }
    assertTrue(exception,
               "Expected a runtime exception due a reference to the raw score of an enum in a computed goal polynomial");
  }

  /**
   * Check that enum goals referenced in a polynomial of a tiebreaker use the
   * computed score and not the raw score.
   * 
   * @throws IOException test error
   */
  @Test
  public void testEnumRawScoreTiebreaker() throws IOException {
    boolean exception = false;
    try (
        InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/enum-using-raw-score-tiebreaker.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final IllegalScoreTypeUseException e) {
      exception = true;
    }
    assertTrue(exception,
               "Expected a runtime exception due a reference to the raw score of an enum in a tiebreaker polynomial");
  }

  /**
   * Check that enum goals referenced in a polynomial of a restriction use the
   * computed score and not the raw score.
   * 
   * @throws IOException test error
   */
  @Test
  public void testEnumRawScoreRestriction() throws IOException {
    boolean exception = false;
    try (
        InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/enum-using-raw-score-restriction.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final IllegalScoreTypeUseException e) {
      exception = true;
    }
    assertTrue(exception,
               "Expected a runtime exception due a reference to the raw score of an enum in a restriction polynomial");
  }

  /**
   * Check that a regular goal cannot show up in an enum condition.
   * 
   * @throws IOException test error
   */
  @Test
  public void testNonEnumInEnumCond() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/non-enum-in-enumcond.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final InvalidEnumCondition e) {
      exception = true;
    }
    assertTrue(exception, "Expected a runtime exception due a reference to non-enum goal inside enumCond");
  }

  /**
   * Check that setting an initial value below the minimum for a goal fails.
   * 
   * @throws IOException test error
   */
  @Test
  public void testInitialValueBelowMin() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/initial-value-below-min.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final InvalidInitialValue e) {
      exception = true;
    }
    assertTrue(exception, "Expected a runtime exception due the initial value being below the min");
  }

  /**
   * Check that setting an initial value above the maximum for a goal fails.
   * 
   * @throws IOException test error
   */
  @Test
  public void testInitialValueAboveMax() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/initial-value-above-max.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final InvalidInitialValue e) {
      exception = true;
    }
    assertTrue(exception, "Expected a runtime exception due the initial value being above the max");
  }

  /**
   * Check that setting an initial value for an enum goal that doesn't match an
   * enum value fails.
   * 
   * @throws IOException test error
   */
  @Test
  public void testInitialValueEnumNoMatch() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/initial-value-enum-no-match.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final InvalidInitialValue e) {
      exception = true;
    }
    assertTrue(exception,
               "Expected a runtime exception due the initial value being set to something that doesn't match an enum value");
  }

  /**
   * Check that a variableRef is not allowed in a tiebreaker.
   * 
   * @throws IOException test error
   */
  @Test
  public void testVariableRefInTiebreaker() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/variableRef-in-tiebreaker.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-complex-type")
          && e.getMessage().contains("variableRef")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to a variableRef in a tiebreaker");
  }

  /**
   * Check that a variableRef is not allowed in a restriction.
   * 
   * @throws IOException test error
   */
  @Test
  public void testVariableRefInRestriction() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/variableRef-in-restriction.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("cvc-complex-type")
          && e.getMessage().contains("variableRef")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to a variableRef in a restriction");
  }

  /**
   * Check that a two variables in a computed goal may not have the same name.
   * 
   * @throws IOException test error
   */
  @Test
  public void testDuplicateVariable() throws IOException {
    boolean exception = false;
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/duplicate-variable.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final ChallengeXMLException e) {
      if (e.getMessage().contains("Duplicate key value")) {
        exception = true;
      } else {
        throw e;
      }
    }
    assertTrue(exception, "Expected a runtime exception due to two variables having the same name in a computed goal.");
  }

  /**
   * Check that all known challenge descriptors are still valid.
   * 
   * @throws IOException
   */
  @Test
  public void testAllDescriptors() throws IOException {
    final Collection<URL> urls = ChallengeParser.getAllKnownChallengeDescriptorURLs();

    for (final URL u : urls) {
      LOGGER.info("Challenge: "
          + u.toString());

      try (InputStream stream = u.openStream();
          Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET)) {
        final Document document = ChallengeParser.parse(reader);
        reader.close();
        new ChallengeDescription(document.getDocumentElement());
      }
    }
  }

  /**
   * Check that computed goals referencing computed goals is OK.
   * 
   * @throws IOException test error
   */
  @Test
  public void testComputedGoalReference() throws IOException {
    try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/computed-goal-reference.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    }
  }

  /**
   * Check that computed goals with circular reference are not allowed.
   * 
   * @throws IOException test error
   */
  @Test
  public void testCircularComputedGoalReference() throws IOException {
    boolean exception = false;
    try (
        InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/computed-goal-circular-reference.xml")) {
      assertNotNull(stream);
      ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    } catch (final CircularComputedGoalException e) {
      exception = true;
    }
    assertTrue(exception, "Expected an exception due to circular references.");
  }

}
