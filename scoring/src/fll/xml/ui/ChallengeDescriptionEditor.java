/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.Container;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import org.w3c.dom.Document;

import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeParserTest;

/*
 * TODO
 * - Support modifying the ChallengeDescription, should there be an immutable version?
 *   - Make sure all Collections are copied or marked unmodifiable or documented properly
 * - save support
 * - Determine where TransferHandler class needs to live based on what it needs to know (perhaps a stand alone class)
 *   - maybe just put in up/down arrows; this would be much easier to implement
 * - How to reorder goals in the UI, needs to be used in TransferHandler.exportDone
 * - choose challenge description to edit
 * - close handler on editor to exit when run stand alone
 * 
 */

/**
 * Application to edit {@link ChallengeDescription} objects.
 */
public class ChallengeDescriptionEditor extends JFrame {

  /**
   * @param args
   * @throws IOException
   */
  public static void main(final String[] args) throws IOException {

    // FIXME will need to allow the user to choose the description
    try (
        final InputStream stream = ChallengeParserTest.class.getResourceAsStream("/fll/resources/challenge-descriptors/fll-2016_animal-allies.xml")) {

      final Document challengeDocument = ChallengeParser.parse(new InputStreamReader(stream));

      final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());

      final ChallengeDescriptionEditor editor = new ChallengeDescriptionEditor(description);

      // FIXME need close handler, think about running from launcher and how
      // that will work

      editor.setVisible(true);
    }
  }

  public ChallengeDescriptionEditor(final ChallengeDescription description) {
    super("Challenge Description Editor");

    final Container cpane = getContentPane();
    cpane.setLayout(new BoxLayout(cpane, BoxLayout.Y_AXIS));

    final ScoreCategoryEditor performanceEditor = new ScoreCategoryEditor(description.getPerformance());
    cpane.add(performanceEditor);

    pack();
  }

}
