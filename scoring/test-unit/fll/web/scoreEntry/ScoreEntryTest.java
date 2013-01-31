/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.DummyServletContext;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeParserTest;

/**
 * @author jpschewe
 * @version $Revision$
 */
public class ScoreEntryTest {

  /**
   * Just returns the document when asked.
   * TODO use a mocking library to do this.
   */
  private static class TestServletContext extends DummyServletContext {
    @Override
    public Object getAttribute(final String attr) {
      if (ApplicationAttributes.CHALLENGE_DOCUMENT.equals(attr)) {
        final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/all-elements.xml");
        Assert.assertNotNull(stream);
        final Document document = ChallengeParser.parse(new InputStreamReader(stream));
        Assert.assertNotNull(document);
        return document;
      } else if (ApplicationAttributes.CHALLENGE_DESCRIPTION.equals(attr)) {
        final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/all-elements.xml");
        Assert.assertNotNull(stream);
        final Document document = ChallengeParser.parse(new InputStreamReader(stream));
        Assert.assertNotNull(document);
        return new ChallengeDescription(document.getDocumentElement());
      } else {
        return null;
      }
    }
  }

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Test method for
   * {@link fll.web.scoreEntry.ScoreEntry#generateCheckRestrictionsBody(java.io.Writer, org.w3c.dom.Document)}
   * .
   * <p>
   * Load all-elements.xml (from {@link ChallengeParserTest}) and make sure
   * there are no errors.
   * </p>
   */
  @Test
  public void testGenerateCheckRestrictionsBody() throws IOException, ParseException {
    final StringWriter writer = new StringWriter();

    ScoreEntry.generateCheckRestrictionsBody(writer, new TestServletContext());
    Assert.assertTrue(writer.toString().length() > 0);
  }

}
