/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreEntry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.ParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fll.TestUtils;
import fll.Utilities;
import fll.web.ApplicationAttributes;
import fll.web.DummyServletContext;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeParserTest;

/**
 * @author jpschewe
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class ScoreEntryTest {

  /**
   * Just returns the document when asked.
   * TODO use a mocking library to do this.
   */
  private static class TestServletContext extends DummyServletContext {
    @Override
    public Object getAttribute(final String attr) {
      try {
        if (ApplicationAttributes.CHALLENGE_DESCRIPTION.equals(attr)) {
          try (InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/all-elements.xml")) {
            assertNotNull(stream);
            final ChallengeDescription description = ChallengeParser.parse(new InputStreamReader(stream,
                                                                                                 Utilities.DEFAULT_CHARSET));
            assertNotNull(description);
            return description;
          }
        } else {
          return null;
        }
      } catch (final IOException e) {
        throw new RuntimeException("Internal test error", e);
      }
    }
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
    assertTrue(writer.toString().length() > 0);
  }

}
