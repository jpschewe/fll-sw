/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
import fll.util.FormatterUtils;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeXMLException;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;
import net.mtu.eggplant.util.BasicFileFilter;
import net.mtu.eggplant.util.gui.GraphicsUtils;

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

    try (
        final InputStream stream = ChallengeDescriptionEditor.class.getResourceAsStream("/fll/resources/challenge-descriptors/fll-2015_trash-trek.xml")) {

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

  private final JFormattedTextField mTitleEditor;

  private final JFormattedTextField mRevisionEditor;

  private final JFormattedTextField mCopyrightEditor;

  private final JComboBox<WinnerType> mWinnerEditor;

  private final PerformanceEditor mPerformanceEditor;

  private final List<SubjectiveCategoryEditor> mSubjectiveEditors = new LinkedList<>();

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

    // properties specific to the challenge description
    final JPanel challengePanel = new JPanel(new GridBagLayout());
    topPanel.add(challengePanel);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Title: "), gbc);

    mTitleEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mTitleEditor, gbc);

    mTitleEditor.addPropertyChangeListener("value", e -> {
      if (null != mDescription) {
        mDescription.setTitle(mTitleEditor.getText());
      }
    });

    mTitleEditor.setColumns(80);
    mTitleEditor.setMaximumSize(mTitleEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Revision: "), gbc);

    mRevisionEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mRevisionEditor, gbc);

    mRevisionEditor.addPropertyChangeListener("value", e -> {
      if (null != mDescription) {
        mDescription.setRevision(mRevisionEditor.getText());
      }
    });

    mRevisionEditor.setColumns(20);
    mRevisionEditor.setMaximumSize(mRevisionEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Copyright: "), gbc);

    mCopyrightEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mCopyrightEditor, gbc);

    mCopyrightEditor.addPropertyChangeListener("value", e -> {
      if (null != mDescription) {
        mDescription.setCopyright(mCopyrightEditor.getText());
      }
    });

    mCopyrightEditor.setColumns(80);
    mCopyrightEditor.setMaximumSize(mCopyrightEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Best score: "), gbc);

    mWinnerEditor = new JComboBox<>(WinnerType.values());
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mWinnerEditor, gbc);

    mWinnerEditor.addActionListener(e -> {
      if (null != mDescription) {
        final WinnerType winner = mWinnerEditor.getItemAt(mWinnerEditor.getSelectedIndex());
        mDescription.setWinner(winner);
      }
    });

    // child elements of the challenge description
    mPerformanceEditor = new PerformanceEditor();
    mPerformanceEditor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    final MovableExpandablePanel performance = new MovableExpandablePanel("Performance", mPerformanceEditor, false);
    topPanel.add(performance);

    final Box subjectiveTopContainer = Box.createVerticalBox();
    topPanel.add(subjectiveTopContainer);

    subjectiveTopContainer.setBorder(BorderFactory.createTitledBorder("Subjective"));

    final Box subjectiveButtonBox = Box.createHorizontalBox();
    subjectiveTopContainer.add(subjectiveButtonBox);

    final JButton addSubjectiveCategory = new JButton("Add Subjective Category");
    subjectiveButtonBox.add(addSubjectiveCategory);
    addSubjectiveCategory.addActionListener(l -> addNewSubjectiveCategory());

    subjectiveButtonBox.add(Box.createHorizontalGlue());

    mSubjectiveContainer = Box.createVerticalBox();
    subjectiveTopContainer.add(mSubjectiveContainer);

    mSubjectiveMoveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        final int oldIndex = Utilities.getIndexOfComponent(mSubjectiveContainer, e.getComponent());
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

        // update editor list
        final SubjectiveCategoryEditor editor = mSubjectiveEditors.remove(oldIndex);
        mSubjectiveEditors.add(newIndex, editor);
                
        // update the UI
        mSubjectiveContainer.add(editor, newIndex);
        mSubjectiveContainer.validate();

        // update the order in the challenge description
        final SubjectiveScoreCategory category = mDescription.removeSubjectiveCategory(oldIndex);
        mDescription.addSubjectiveCategory(newIndex, category);
      }
    };

    // fill in the bottom of the panel
    topPanel.add(Box.createVerticalGlue());

    final JScrollPane scroller = new JScrollPane(topPanel);
    scroller.getVerticalScrollBar().setUnitIncrement(20);
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
   * Set the current file. Separate from
   * {@link #setChallengeDescription(File, ChallengeDescription)} for working with
   * {@link #mSaveAsAction}
   * 
   * @param file the file where the challenge is saved
   */
  private void setCurrentFile(final File file) {
    mCurrentFile = file;

    if (null != mCurrentFile) {
      setTitle(mCurrentFile.getName());
    } else {
      setTitle("<no file>");
    }
  }

  /**
   * @param file the file that the challenge was loaded from, may be null; passed
   *          to {@link #setCurrentFile(File)}
   * @param description the description to edit, if null create a new challenge
   *          description
   */
  private void setChallengeDescription(final File file,
                                       final ChallengeDescription description) {
    setCurrentFile(file);

    mDescription = description == null ? new ChallengeDescription("New Challenge") : description;

    mTitleEditor.setValue(mDescription.getTitle());
    mRevisionEditor.setValue(mDescription.getRevision());
    mCopyrightEditor.setValue(mDescription.getCopyright());
    mWinnerEditor.setSelectedItem(mDescription.getWinner());

    mSubjectiveContainer.removeAll();
    mSubjectiveEditors.clear();

    mPerformanceEditor.setCategory(mDescription.getPerformance());

    mDescription.getSubjectiveCategories().forEach(this::addSubjectiveCategory);

    validate();
  }

  private void addNewSubjectiveCategory() {
    final String name = String.format("category_%d", mSubjectiveEditors.size());
    final String title = String.format("Category %d", mSubjectiveEditors.size());

    final SubjectiveScoreCategory cat = new SubjectiveScoreCategory(name, title);
    mDescription.addSubjectiveCategory(cat);

    addSubjectiveCategory(cat);
  }

  private void addSubjectiveCategory(final SubjectiveScoreCategory cat) {
    final SubjectiveCategoryEditor editor = new SubjectiveCategoryEditor();
    editor.setCategory(cat);
    editor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    final MovableExpandablePanel container = new MovableExpandablePanel(cat.getTitle(), editor);
    container.addMoveEventListener(mSubjectiveMoveListener);

    editor.addPropertyChangeListener("title", e -> {
      final String newTitle = (String) e.getNewValue();
      container.setTitle(newTitle);
    });

    mSubjectiveContainer.add(container);
    mSubjectiveContainer.validate();

    mSubjectiveEditors.add(editor);
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

        setCurrentFile(file);
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
    try {
      mTitleEditor.commitEdit();
    } catch (final ParseException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got parse exception committing title changes, assuming bad value and ignoring", e);
      }
    }

    try {
      mRevisionEditor.commitEdit();
    } catch (final ParseException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got parse exception committing revision changes, assuming bad value and ignoring", e);
      }
    }

    try {
      mCopyrightEditor.commitEdit();
    } catch (final ParseException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got parse exception committing copyright changes, assuming bad value and ignoring", e);
      }
    }

    mPerformanceEditor.commitChanges();
    mSubjectiveEditors.forEach(e -> e.commitChanges());
  }

}
