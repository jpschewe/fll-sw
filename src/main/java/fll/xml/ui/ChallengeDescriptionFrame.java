/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
import net.mtu.eggplant.util.BasicFileFilter;
import net.mtu.eggplant.util.gui.GraphicsUtils;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Frame that the challenge description editor lives in.
 */
public class ChallengeDescriptionFrame extends JFrame {
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
        // final InputStream stream =
        // ChallengeDescriptionEditor.class.getResourceAsStream("/fll/resources/challenge-descriptors/fll-2015_trash-trek.xml"))
        // {
        final InputStream stream = ChallengeDescriptionEditor.class.getResourceAsStream("/fll/resources/challenge-descriptors/fll-2014-world_class.xml")) {

      final Document challengeDocument = ChallengeParser.parse(new InputStreamReader(stream,
                                                                                     Utilities.DEFAULT_CHARSET));

      final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());

      final ChallengeDescriptionFrame editor = new ChallengeDescriptionFrame();
      editor.setCurrentFile(null, description);

      editor.addWindowListener(new WindowAdapter() {
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

      GraphicsUtils.centerWindow(editor);
      editor.setVisible(true);
    } catch (final Throwable e) {
      JOptionPane.showMessageDialog(null, "Unexpected error: "
          + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      LOGGER.fatal("Unexpected error", e);
      System.exit(1);
    }
  }

  private File mCurrentFile = null;

  private final JScrollPane scroller;

  private ChallengeDescriptionEditor editor = null;

  public ChallengeDescriptionFrame() {
    super("Challenge Description Editor");

    mCurrentFile = null;

    createMenuBar();

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    cpane.add(createToolBar(), BorderLayout.PAGE_START);

    scroller = new JScrollPane();
    scroller.getVerticalScrollBar().setUnitIncrement(20);
    cpane.add(scroller, BorderLayout.CENTER);

    setSize(800, 600);

    // initial state
    setCurrentFile(mCurrentFile, null);

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

    menu.add(validateAction);

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
      promptForSaveOfCurrentDescription();
      openChallengeDescription();
    }
  };

  private void promptForSaveOfCurrentDescription() {
    final int result = JOptionPane.showConfirmDialog(this, "Do you want to save the current challenge description?",
                                                     "Overwrite file?", JOptionPane.YES_NO_OPTION);
    if (result == JOptionPane.YES_OPTION) {
      saveChallengeDescription();
    }
  }

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
      promptForSaveOfCurrentDescription();
      setCurrentFile(null, null);
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

      try (Reader stream = new InputStreamReader(new FileInputStream(file), Utilities.DEFAULT_CHARSET)) {

        final Document challengeDocument = ChallengeParser.parse(stream);

        final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());

        setCurrentFile(file, description);
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
  private void setCurrentFile(final File file,
                              final ChallengeDescription description) {
    mCurrentFile = file;

    if (null == description) {
      editor = new ChallengeDescriptionEditor(new ChallengeDescription("New"));
    } else {
      editor = new ChallengeDescriptionEditor(description);
    }

    scroller.setViewportView(editor);

    if (null != mCurrentFile) {
      setTitle(mCurrentFile.getName());
    } else {
      setTitle("<no file>");
    }
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
    promptForSaveOfCurrentDescription();

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

  private final Action validateAction = new AbstractAction("Validate") {
    {
      // putValue(SMALL_ICON,
      // GraphicsUtils.getIcon("toolbarButtonGraphics/general/SaveAs16.gif"));
      // putValue(LARGE_ICON_KEY,
      // GraphicsUtils.getIcon("toolbarButtonGraphics/general/SaveAs24.gif"));
      putValue(SHORT_DESCRIPTION, "Check the challenge for validity");
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      validateChallengeDescription();

    }
  };

  private boolean validateChallengeDescription() {
    final Collection<String> messages = new LinkedList<>();
    final boolean valid = editor.checkValidity(messages);
    if (!valid) {
      JOptionPane.showMessageDialog(null,
                                    "Some part of the challenge is invalid, hover over the red boxes to see the issues",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }

    return valid;
  }

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

    editor.commitChanges();

    final boolean valid = validateChallengeDescription();
    if (!valid) {
      return;
    }

    try (final Writer writer = new OutputStreamWriter(new FileOutputStream(mCurrentFile), Utilities.DEFAULT_CHARSET)) {
      final Document saveDoc = editor.getDescription().toXml();
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
    editor.commitChanges();

    final boolean valid = validateChallengeDescription();
    if (!valid) {
      return;
    }

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

      try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file), Utilities.DEFAULT_CHARSET)) {
        final Document saveDoc = editor.getDescription().toXml();
        XMLUtils.writeXML(saveDoc, writer, Utilities.DEFAULT_CHARSET.name());

        setCurrentFile(file, editor.getDescription());
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

}
