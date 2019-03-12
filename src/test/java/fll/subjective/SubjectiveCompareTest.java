/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fll.Utilities;
import fll.util.LogUtils;
import fll.web.admin.DownloadSubjectiveData;
import fll.xml.ChallengeParser;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Test comparing subjective score documents.
 */
public class SubjectiveCompareTest {

  private Document challengeDocument;

  @BeforeEach
  public void setUp() {
    LogUtils.initializeLogging();

    final InputStream stream = SubjectiveCompareTest.class.getResourceAsStream("challenge.xml");
    assertNotNull(stream);
    challengeDocument = ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
  }

  @AfterEach
  public void tearDown() {
    challengeDocument = null;
  }

  private Document loadDocument(final String resourceName) throws SAXException, IOException {
    final InputStream scoreStream = SubjectiveCompareTest.class.getResourceAsStream(resourceName);
    assertNotNull(scoreStream);
    final Document scoreDocument = XMLUtils.parseXMLDocument(scoreStream);
    assertNotNull(scoreDocument);

    DownloadSubjectiveData.validateXML(scoreDocument);

    return scoreDocument;
  }

  /**
   * Compare a document to itself and make sure there are no differences.
   * 
   * @throws SAXException
   * @throws IOException
   */
  @Test
  public void simpleTestWithNoDifferences() throws SAXException, IOException {
    final Document scoreDocument = loadDocument("master-score.xml");
    final Collection<SubjectiveScoreDifference> diffs = SubjectiveUtils.compareScoreDocuments(challengeDocument,
                                                                                              scoreDocument,
                                                                                              scoreDocument);
    assertNotNull(diffs);
    assertTrue( diffs.isEmpty(), "Should not be any differences");
  }

  /**
   * Basic difference test. One difference.
   * 
   * @throws SAXException
   * @throws IOException
   */
  @Test
  public void simpleTestWithOneDifference() throws SAXException, IOException {
    final Document masterDocument = loadDocument("master-score.xml");
    final Document compareDocument = loadDocument("single-diff.xml");
    final Collection<SubjectiveScoreDifference> diffs = SubjectiveUtils.compareScoreDocuments(challengeDocument,
                                                                                              masterDocument,
                                                                                              compareDocument);
    assertNotNull(diffs);
    assertEquals( 1, diffs.size(), "There be exactly 1 difference: "
        + diffs);
    final SubjectiveScoreDifference diff = diffs.iterator().next();
    assertEquals("Teamwork", diff.getCategory());
    assertEquals("Confidence & Enthusiasm", diff.getSubcategory());
    assertEquals("DEB_JOHNSON", diff.getJudge());
    assertEquals(793, diff.getTeamNumber());

    assertEquals( DoubleSubjectiveScoreDifference.class, diff.getClass(), "Should be a double difference");
    final DoubleSubjectiveScoreDifference doubleDiff = (DoubleSubjectiveScoreDifference) diff;
    assertEquals(15D, doubleDiff.getMasterValue(), 0D);
    assertEquals(13D, doubleDiff.getCompareValue(), 0D);
  }
}
