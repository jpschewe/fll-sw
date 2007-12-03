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

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.xml.ChallengeParser;
import fll.xml.ChallengeParserTest;

/**
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class ScoreEntryTest extends TestCase {

  /**
   * Test method for
   * {@link fll.web.scoreEntry.ScoreEntry#generateCheckRestrictionsBody(java.io.Writer, org.w3c.dom.Document)}.
   * 
   * <p>
   * Load all-elements.xml (from {@link ChallengeParserTest}) and make sure
   * there are no errors.
   * </p>
   * 
   */
  @Test
  public void testGenerateCheckRestrictionsBody() throws IOException, ParseException {
    final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/all-elements.xml");
    Assert.assertNotNull(stream);
    final Document document = ChallengeParser.parse(new InputStreamReader(stream));
    Assert.assertNotNull(document);
    final StringWriter writer = new StringWriter();
    ScoreEntry.generateCheckRestrictionsBody(writer, document);
    Assert.assertTrue(writer.toString().length() > 0);
  }
}
