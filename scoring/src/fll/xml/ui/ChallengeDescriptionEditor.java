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
 * - Support modifying the ChallengeDescription
 *   - CaseStatement
 *   - AbstractConditionStatement ...
 * - Be able to create all elements without an XML element
 * - add validity check somewhere to ensure that all names are unique, perhaps this is part of the UI
 *   - look at the XML validation code
 *   - unique names for goals in each category
 *   - unique names for categories
 *   - Goal min/max
 *   - Restriction upperBound/lowerBound
 *   - RubricRange min/max
 *   - unique variable names inside ComputedGoal
 *   - SwitchStatement must have something in the default case
 * - save to XML
 * - UI support for reordering
 *   - just put in up/down arrows; this would be much easier to implement than DnD
 * - choose challenge description to edit
 * - close handler on editor to exit when run stand alone
 * - support grouping of goals, this is where DnD might be useful 
 *   - all goals in a group must be consecutive
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
