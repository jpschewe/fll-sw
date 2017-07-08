/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.XMLUtils;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;
import net.mtu.eggplant.util.BasicFileFilter;
import net.mtu.eggplant.util.gui.GraphicsUtils;

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
 * - choose challenge description to edit
 * - support grouping of goals, this is where DnD might be useful 
 *   - all goals in a group must be consecutive
 * - how to handle when being run from the launcher so that it doesn't exit?
 *   - maybe have a method that is called from main that creates everything
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
        final InputStream stream = ChallengeDescriptionEditor.class.getResourceAsStream("/fll/resources/challenge-descriptors/fll-2016_animal-allies-MN.xml")) {

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

  private final ChallengeDescription mDescription;
  private final ScoreCategoryEditor mPerformanceEditor;
  private final List<ScoreCategoryEditor> mSubjectiveEditors = new LinkedList<>();

  public ChallengeDescriptionEditor(final ChallengeDescription description) {
    super("Challenge Description Editor");
    mDescription = description;

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    cpane.add(createToolBar(), BorderLayout.PAGE_START);

    final JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
    topPanel.setAlignmentX(LEFT_ALIGNMENT);

    mPerformanceEditor = new ScoreCategoryEditor(mDescription.getPerformance());
    final MovableExpandablePanel performance = new MovableExpandablePanel("Performance", mPerformanceEditor, false);
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

        if (newIndex < 0
            || newIndex >= subjective.getComponentCount()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Can't move component outside the container oldIndex: "
                + oldIndex + " newIndex: " + newIndex);
          }
          return;
        }

        // update the UI
        subjective.add(e.getComponent(), newIndex);
        subjective.validate();

        // update the order in the challenge description
        final SubjectiveScoreCategory category = mDescription.removeSubjectiveCategory(oldIndex);
        mDescription.addSubjectiveCategory(newIndex, category);
      }

    };

    for (final SubjectiveScoreCategory cat : mDescription.getSubjectiveCategories()) {
      final ScoreCategoryEditor editor = new ScoreCategoryEditor(cat);

      final MovableExpandablePanel container = new MovableExpandablePanel(cat.getTitle(), editor);
      container.addMoveEventListener(subjectiveMoveListener);

      subjective.add(container);
      
      mSubjectiveEditors.add(editor);
    }

    final JScrollPane scroller = new JScrollPane(topPanel);
    cpane.add(scroller, BorderLayout.CENTER);

    pack();
  }

  private final Action mSaveAction = new AbstractAction("Save As...") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/SaveAs16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/SaveAs24.gif"));
      putValue(SHORT_DESCRIPTION, "Save the challenge description file with a new name");
      putValue(MNEMONIC_KEY, KeyEvent.VK_A);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK
          | ActionEvent.SHIFT_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      saveChallengeDescription();
    }
  };

  private void saveChallengeDescription() {
    commitChanges();
    
    //FIXME needs work!!!
    
    // final String startingDirectory =
    // PREFS.get(DESCRIPTION_STARTING_DIRECTORY_PREF, null);

    final JFileChooser fileChooser = new JFileChooser();
    final FileFilter filter = new BasicFileFilter("FLL Challenge Description (xml)", new String[] { "xml" });
    fileChooser.setFileFilter(filter);
    // if (null != startingDirectory) {
    // fileChooser.setCurrentDirectory(new File(startingDirectory));
    // }

    final int returnVal = fileChooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      // final File currentDirectory = fileChooser.getCurrentDirectory();
      // PREFS.put(DESCRIPTION_STARTING_DIRECTORY_PREF,
      // currentDirectory.getAbsolutePath());

      final File file = fileChooser.getSelectedFile();
      // mDescriptionFilename.setText(mScheduleDescriptionFile.getName());

      try (final Writer writer = new FileWriter(file)) {
        final Document saveDoc = mDescription.toXml();
        XMLUtils.writeXML(saveDoc, writer, Utilities.DEFAULT_CHARSET.name());
      } catch(final IOException e) {
        LOGGER.error("Error writing document", e);
        
        JOptionPane.showMessageDialog(null,
                                      "An unexpected error occurred. Please send the log file and a description of what you were doing to the developer. Error message: "
                                          + e.getMessage(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }

    } else {
      // user canceled
      return;
    }
  }

  private JToolBar createToolBar() {
    final JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);

    toolbar.add(mSaveAction);

    return toolbar;
  }
  
  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    mPerformanceEditor.commitChanges();
    mSubjectiveEditors.forEach(e -> e.commitChanges());    
  }
  
}
