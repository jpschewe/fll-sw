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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeXMLException;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.XMLUtils;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;
import net.mtu.eggplant.util.BasicFileFilter;
import net.mtu.eggplant.util.gui.GraphicsUtils;

/*
 * TODO
 * - add validity check 
 *   - call from the UI
 *     - display indicator on each element if it's valid or not?
 *   - ensure that all names are unique
 *   - see the validity check that is run after parsing the XML
 *   - non-null name for AbstractGoal title and name
 *   - SwitchStatement must have a default case
 *   - look at the XML validation code
 *   - unique names for goals in each category
 *   - unique names for categories
 *   - Goal min/max
 *   - Restriction upperBound/lowerBound
 *   - RubricRange min/max
 *   - unique variable names inside ComputedGoal
 *   - SwitchStatement must have something in the default case
 * - ability to add subjective categories
 * - Ability to add goals
 * - Note that one can create a BasicPolynomial with variable references.
 *   - Perhaps BasicPolynomial and ComplexPolynomial can be merged?
 *   - What effect does this have on the XML? Is there always a scope available?
 *   - If we can't do this, then the UI needs to enforce the restriction.
 * - support grouping of goals, this is where DnD might be useful 
 *   - all goals in a group must be consecutive
 * - how to handle when being run from the launcher so that it doesn't exit?
 *   - maybe have a method that is called from main that creates everything
 * 
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

      final ChallengeDescriptionEditor editor = new ChallengeDescriptionEditor();
      editor.setChallengeDescription(null, description);

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

  private ChallengeDescription mDescription;

  private File mCurrentFile = null;

  private final ScoreCategoryEditor mPerformanceEditor;

  private final List<ScoreCategoryEditor> mSubjectiveEditors = new LinkedList<>();

  private final JComponent mSubjectiveContainer;

  private final MoveEventListener mSubjectiveMoveListener;

  public ChallengeDescriptionEditor() {
    super("Challenge Description Editor");
    mDescription = null;
    mCurrentFile = null;

    createMenuBar();

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    cpane.add(createToolBar(), BorderLayout.PAGE_START);

    final JComponent topPanel = Box.createVerticalBox();
    topPanel.setAlignmentX(LEFT_ALIGNMENT);

    mPerformanceEditor = new ScoreCategoryEditor();
    mPerformanceEditor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    final MovableExpandablePanel performance = new MovableExpandablePanel("Performance", mPerformanceEditor, false);
    topPanel.add(performance);

    mSubjectiveContainer = Box.createVerticalBox();
    mSubjectiveContainer.setBorder(BorderFactory.createTitledBorder("Subjective"));
    topPanel.add(mSubjectiveContainer);

    mSubjectiveMoveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        // find index of e.component in subjective
        int oldIndex = -1;
        for (int i = 0; oldIndex < 0
            && i < mSubjectiveContainer.getComponentCount(); ++i) {
          final Component c = mSubjectiveContainer.getComponent(i);
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
            || newIndex >= mSubjectiveContainer.getComponentCount()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Can't move component outside the container oldIndex: "
                + oldIndex
                + " newIndex: "
                + newIndex);
          }
          return;
        }

        // update the UI
        mSubjectiveContainer.add(e.getComponent(), newIndex);
        mSubjectiveContainer.validate();

        // update the order in the challenge description
        final SubjectiveScoreCategory category = mDescription.removeSubjectiveCategory(oldIndex);
        mDescription.addSubjectiveCategory(newIndex, category);
      }

    };

    // fill in the bottom of the panel
    topPanel.add(Box.createVerticalGlue());

    final JScrollPane scroller = new JScrollPane(topPanel);
    cpane.add(scroller, BorderLayout.CENTER);

    setSize(800, 600);
  }

  /**
   * Create the menu bar for the frame and set it on the frame.
   */
  private void createMenuBar() {
    final JMenuBar menubar = new JMenuBar();

    menubar.add(createFileMenu());

    this.setJMenuBar(menubar);
  }

  /**
   * @return the file menu
   */
  private JMenu createFileMenu() {
    final JMenu menu = new JMenu("File");
    menu.setMnemonic(KeyEvent.VK_F);

    menu.add(mNewAction);
    menu.add(mOpenAction);

    menu.addSeparator();

    menu.add(mSaveAction);
    menu.add(mSaveAsAction);

    menu.addSeparator();

    menu.add(mExitAction);

    return menu;
  }

  private final Action mOpenAction = new AbstractAction("Open...") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open24.gif"));
      putValue(SHORT_DESCRIPTION, "Open an existing challenge description");
      putValue(MNEMONIC_KEY, KeyEvent.VK_O);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      // FIXME prompt for saving of existing file

      openChallengeDescription();
    }
  };

  private final Action mNewAction = new AbstractAction("New") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/New16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/New24.gif"));
      putValue(SHORT_DESCRIPTION, "Create a new challenge description");
      putValue(MNEMONIC_KEY, KeyEvent.VK_N);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      // FIXME prompt for saving of existing file

      setChallengeDescription(null, null);
    }
  };

  private void openChallengeDescription() {
    final String startingDirectory = PREFS.get(DESCRIPTION_STARTING_DIRECTORY_PREF, null);

    final JFileChooser fileChooser = new JFileChooser();
    final FileFilter filter = new BasicFileFilter("FLL Challenge Description (xml)", new String[] { "xml" });
    fileChooser.setFileFilter(filter);
    if (null != startingDirectory) {
      fileChooser.setCurrentDirectory(new File(startingDirectory));
    }

    final int returnVal = fileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      final File currentDirectory = fileChooser.getCurrentDirectory();
      PREFS.put(DESCRIPTION_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());

      final File file = fileChooser.getSelectedFile();

      if (!file.exists()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("User asked to open a file that doesn't exist");
        }
        JOptionPane.showMessageDialog(this, String.format("The file %s doesn't exist", file.getAbsolutePath()), "Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }

      try (Reader stream = new FileReader(file)) {

        final Document challengeDocument = ChallengeParser.parse(stream);

        final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());

        setChallengeDescription(file, description);
      } catch (final IOException e) {
        LOGGER.error("Error loading "
            + file.getAbsolutePath(), e);

        JOptionPane.showMessageDialog(null, "Error reading the XML file. Error message: "
            + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

      } catch (final ChallengeXMLException e) {
        LOGGER.error("Malformed XML "
            + file.getAbsolutePath(), e);

        JOptionPane.showMessageDialog(null, "The XML file has an error in it. Error message: "
            + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

      }
    }
  }

  /**
   * @param file the file that the challenge was loaded from, may be null
   * @param description the description to edit, if null create a new challenge
   *          description
   */
  private void setChallengeDescription(final File file,
                                       final ChallengeDescription description) {
    mCurrentFile = file;
    mDescription = description == null ? new ChallengeDescription("New Challenge") : description;

    if (null != mCurrentFile) {
      setTitle(mCurrentFile.getName());
    }

    mSubjectiveContainer.removeAll();
    mSubjectiveEditors.clear();

    mPerformanceEditor.setCategory(mDescription.getPerformance());

    for (final SubjectiveScoreCategory cat : mDescription.getSubjectiveCategories()) {
      final ScoreCategoryEditor editor = new ScoreCategoryEditor();
      editor.setCategory(cat);
      editor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

      final MovableExpandablePanel container = new MovableExpandablePanel(cat.getTitle(), editor);
      container.addMoveEventListener(mSubjectiveMoveListener);

      mSubjectiveContainer.add(container);

      mSubjectiveEditors.add(editor);
    }
    
    validate();
  }

  private final Action mExitAction = new AbstractAction("Exit") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop24.gif"));
      putValue(SHORT_DESCRIPTION, "Exit the application");
      putValue(MNEMONIC_KEY, KeyEvent.VK_Q);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      exit();
    }
  };

  private void exit() {
    // FIXME: check that everything is saved

    // only hide the frame so that when being called from elsewhere the system
    // doesn't exit
    setVisible(false);
  }

  private final Action mSaveAction = new AbstractAction("Save") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Save16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Save24.gif"));
      putValue(SHORT_DESCRIPTION, "Save the challenge description file with the current name");
      putValue(MNEMONIC_KEY, KeyEvent.VK_S);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      saveChallengeDescription();
    }
  };

  private final Action mSaveAsAction = new AbstractAction("Save As...") {
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
      saveAsChallengeDescription();
    }
  };

  private static final Preferences PREFS = Preferences.userNodeForPackage(ChallengeDescriptionEditor.class);

  private static final String DESCRIPTION_STARTING_DIRECTORY_PREF = "descriptionStartingDirectory";

  /**
   * Save the challenge description to the current filename. If the current file
   * is null, then call {@link #saveAsChallengeDescription()}.
   */
  private void saveChallengeDescription() {
    if (null == mCurrentFile) {
      saveAsChallengeDescription();
      return;
    }

    commitChanges();

    try (final Writer writer = new FileWriter(mCurrentFile)) {
      final Document saveDoc = mDescription.toXml();
      XMLUtils.writeXML(saveDoc, writer, Utilities.DEFAULT_CHARSET.name());
    } catch (final IOException e) {
      LOGGER.error("Error writing document", e);

      JOptionPane.showMessageDialog(null,
                                    "An unexpected error occurred. Please send the log file and a description of what you were doing to the developer. Error message: "
                                        + e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Save the challenge description and prompt the user for the filename.
   */
  private void saveAsChallengeDescription() {
    commitChanges();

    final String startingDirectory = PREFS.get(DESCRIPTION_STARTING_DIRECTORY_PREF, null);

    final JFileChooser fileChooser = new JFileChooser();
    final FileFilter filter = new BasicFileFilter("FLL Challenge Description (xml)", new String[] { "xml" });
    fileChooser.setFileFilter(filter);
    if (null != startingDirectory) {
      fileChooser.setCurrentDirectory(new File(startingDirectory));
    }

    final int returnVal = fileChooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      final File currentDirectory = fileChooser.getCurrentDirectory();
      PREFS.put(DESCRIPTION_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());

      File file = fileChooser.getSelectedFile();
      if (!file.getName().endsWith(".xml")) {
        file = new File(file.getAbsolutePath()
            + ".xml");
      }

      if (file.exists()) {
        final int result = JOptionPane.showConfirmDialog(this,
                                                         String.format("The file %s already exists, do you want to overwrite it?",
                                                                       file.getName()),
                                                         "Overwrite file?", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.NO_OPTION) {
          return;
        }
      }

      try (final Writer writer = new FileWriter(file)) {
        final Document saveDoc = mDescription.toXml();
        XMLUtils.writeXML(saveDoc, writer, Utilities.DEFAULT_CHARSET.name());

        mCurrentFile = file;
      } catch (final IOException e) {
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

    toolbar.add(mNewAction);
    toolbar.add(mOpenAction);
    toolbar.add(mSaveAction);
    toolbar.add(mSaveAsAction);

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
