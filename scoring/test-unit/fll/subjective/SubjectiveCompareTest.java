/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

/**
 * Test comparing subjective score documents.
 */
public class SubjectiveCompareTest {

  private Document challengeDocument;

  @Before
  public void setUp() {
    final InputStream stream = SubjectiveCompareTest.class.getResourceAsStream("challenge.xml");
    Assert.assertNotNull(stream);
    challengeDocument = ChallengeParser.parse(new InputStreamReader(stream));
  }

  @After
  public void tearDown() {
    challengeDocument = null;
  }

  private Document loadDocument(final String resourceName) {
    final InputStream scoreStream = SubjectiveCompareTest.class.getResourceAsStream(resourceName);
    Assert.assertNotNull(scoreStream);
    final Document scoreDocument = XMLUtils.parseXMLDocument(scoreStream);
    Assert.assertNotNull(scoreDocument);
    return scoreDocument;
  }

  /**
   * Compare a document to itself and make sure there are no differences.
   */
  @Test
  public void simpleTestWithNoDifferences() {
    final Document scoreDocument = loadDocument("master-score.xml");
    final Collection<SubjectiveScoreDifference> diffs = SubjectiveUtils.compareScoreDocuments(challengeDocument, scoreDocument, scoreDocument);
    Assert.assertNotNull(diffs);
    Assert.assertTrue("Should not be any differences", diffs.isEmpty());
  }

  /**
   * Basic difference test. One difference.
   */
  @Test
  public void simpleTestWithOneDifference() {
    final Document masterDocument = loadDocument("master-score.xml");
    final Document compareDocument = loadDocument("single-diff.xml");
    final Collection<SubjectiveScoreDifference> diffs = SubjectiveUtils.compareScoreDocuments(challengeDocument, masterDocument, compareDocument);
    Assert.assertNotNull(diffs);
    Assert.assertEquals("There be exactly 1 difference: "
        + diffs, 1, diffs.size());
    final SubjectiveScoreDifference diff = diffs.iterator().next();
    Assert.assertEquals("Teamwork", diff.getCategory());
    Assert.assertEquals("Confidence & Enthusiasm", diff.getSubcategory());
    Assert.assertEquals("DEB_JOHNSON", diff.getJudge());
    Assert.assertEquals(793, diff.getTeamNumber());

    Assert.assertEquals("Should be a double difference", DoubleSubjectiveScoreDifference.class, diff.getClass());
    final DoubleSubjectiveScoreDifference doubleDiff = (DoubleSubjectiveScoreDifference) diff;
    Assert.assertEquals(15D, doubleDiff.getMasterValue(), 0D);
    Assert.assertEquals(13D, doubleDiff.getCompareValue(), 0D);
  }
}
