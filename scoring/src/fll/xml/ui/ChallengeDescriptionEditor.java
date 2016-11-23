/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeParserTest;
import fll.xml.ScoreCategory;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;

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

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param args
   * @throws IOException
   */
  public static void main(final String[] args) throws IOException {
    LogUtils.initializeLogging();

    Thread.setDefaultUncaughtExceptionHandler(new GuiExceptionHandler());

    // Use cross platform look and feel so that things look right all of the
    // time
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (final ClassNotFoundException e) {
      LOGGER.warn("Could not find cross platform look and feel class", e);
    } catch (final InstantiationException e) {
      LOGGER.warn("Could not instantiate cross platform look and feel class", e);
    } catch (final IllegalAccessException e) {
      LOGGER.warn("Error loading cross platform look and feel", e);
    } catch (final UnsupportedLookAndFeelException e) {
      LOGGER.warn("Cross platform look and feel unsupported?", e);
    }

    // FIXME will need to allow the user to choose the description
    try (
        final InputStream stream = ChallengeParserTest.class.getResourceAsStream("/fll/resources/challenge-descriptors/fll-2016_animal-allies-MN.xml")) {

      final Document challengeDocument = ChallengeParser.parse(new InputStreamReader(stream));

      final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());

      final ChallengeDescriptionEditor editor = new ChallengeDescriptionEditor(description);

      // FIXME need close handler, think about running from launcher and how
      // that will work

      editor.addWindowListener(new WindowAdapter() {
        @Override
        @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting from main is OK")
        public void windowClosing(final WindowEvent e) {
          System.exit(0);
        }

        @Override
        @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting from main is OK")
        public void windowClosed(final WindowEvent e) {
          System.exit(0);
        }
      });
      // should be able to watch for window closing, but hidden works
      editor.addComponentListener(new ComponentAdapter() {
        @Override
        @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting from main is OK")
        public void componentHidden(final ComponentEvent e) {
          System.exit(0);
        }
      });

      editor.setVisible(true);
    }
  }

  public ChallengeDescriptionEditor(final ChallengeDescription description) {
    super("Challenge Description Editor");

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    final JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
    topPanel.setAlignmentX(LEFT_ALIGNMENT);

    final ScoreCategoryEditor performanceEditor = new ScoreCategoryEditor(description.getPerformance());
    final MovableExpandablePanel performance = new MovableExpandablePanel("Performance", performanceEditor, false);
    topPanel.add(performance);

    final JPanel subjective = new JPanel();
    subjective.setBorder(BorderFactory.createTitledBorder("Subjective"));
    subjective.setLayout(new BoxLayout(subjective, BoxLayout.PAGE_AXIS));
    topPanel.add(subjective);

    final MoveEventListener subjectiveMoveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        // find index of e.component in subjective
        int oldIndex = -1;
        for (int i = 0; oldIndex < 0
            && i < subjective.getComponentCount(); ++i) {
          final Component c = subjective.getComponent(i);
          if (c == e.getComponent()) {
            oldIndex = i;
          }
        }
        if (oldIndex < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of move event in subjective container");
            return;
          }
        }

        final int newIndex;
        if (e.getDirection() == MoveDirection.DOWN) {
          newIndex = oldIndex
              + 1;
        } else {
          newIndex = oldIndex
              - 1;
        }
        
        if(newIndex < 0 || newIndex >= subjective.getComponentCount()) {
          if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Can't move component outside the container oldIndex: " + oldIndex + " newIndex: " + newIndex);
          }
          return;
        }
        
        subjective.add(e.getComponent(), newIndex);
        subjective.validate();
        
        //FIXME need to update the order in the challenge description
      }

    };
    for (final ScoreCategory cat : description.getSubjectiveCategories()) {
      final ScoreCategoryEditor editor = new ScoreCategoryEditor(cat);

      final MovableExpandablePanel container = new MovableExpandablePanel(cat.getTitle(), editor);
      container.addMoveEventListener(subjectiveMoveListener);
      
      subjective.add(container);
    }

    final JScrollPane scroller = new JScrollPane(topPanel);
    cpane.add(scroller, BorderLayout.CENTER);

    pack();
  }

}
