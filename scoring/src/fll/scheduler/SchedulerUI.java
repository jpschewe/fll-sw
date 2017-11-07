/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JCheckBox;
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
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.w3c.dom.Document;

import com.itextpdf.text.DocumentException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Utilities;
import fll.scheduler.SchedParams.InvalidParametersException;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FormatterUtils;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import fll.util.ProgressDialog;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ScoreCategory;
import net.mtu.eggplant.util.BasicFileFilter;
import net.mtu.eggplant.util.gui.GraphicsUtils;

/**
 * UI for the scheduler.
 */
public class SchedulerUI extends JFrame {

  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    GuiExceptionHandler.registerExceptionHandler();

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

    try {
      final SchedulerUI frame = new SchedulerUI();

      frame.addWindowListener(new WindowAdapter() {
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
      frame.addComponentListener(new ComponentAdapter() {
        @Override
        @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting from main is OK")
        public void componentHidden(final ComponentEvent e) {
          System.exit(0);
        }
      });

      GraphicsUtils.centerWindow(frame);

      frame.setVisible(true);
    } catch (final Exception e) {
      LOGGER.fatal("Unexpected error", e);
      JOptionPane.showMessageDialog(null,
                                    "An unexpected error occurred. Please send the log file and a description of what you were doing to the developer. Error message: "
                                        + e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private final ChooseChallengeDescriptor chooseChallengeDescriptor = new ChooseChallengeDescriptor(SchedulerUI.this);

  private static final String BASE_TITLE = "FLL Scheduler";

  public SchedulerUI() {
    super(BASE_TITLE);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    _progressDialog = new ProgressDialog(SchedulerUI.this, "Please Wait");
    _progressDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    setJMenuBar(createMenubar());

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    mTabbedPane = new JTabbedPane();
    cpane.add(mTabbedPane, BorderLayout.CENTER);

    // --- schedule description panel
    final JPanel scheduleDescriptionPanel = new JPanel(new BorderLayout());
    mTabbedPane.addTab("Description", scheduleDescriptionPanel);

    mDescriptionFilename = new JLabel("");
    scheduleDescriptionPanel.add(createDescriptionToolbar(), BorderLayout.PAGE_START);

    mScheduleDescriptionEditor = new SolverParamsEditor();
    final JScrollPane editorScroller = new JScrollPane(mScheduleDescriptionEditor);
    scheduleDescriptionPanel.add(editorScroller, BorderLayout.CENTER);

    // start out with default values
    mScheduleDescriptionEditor.setParams(new SolverParams());

    // --- end schedule description panel

    // --- schedule panel
    final JPanel schedulePanel = new JPanel(new BorderLayout());
    mTabbedPane.addTab("Schedule", schedulePanel);

    mScheduleFilename = new JLabel("");
    schedulePanel.add(createScheduleToolbar(), BorderLayout.NORTH);

    mScheduleTable = new JTable();
    mScheduleTable.setAutoCreateRowSorter(true);
    mScheduleTable.setDefaultRenderer(Date.class, schedTableRenderer);
    mScheduleTable.setDefaultRenderer(String.class, schedTableRenderer);
    mScheduleTable.setDefaultRenderer(Integer.class, schedTableRenderer);
    mScheduleTable.setDefaultRenderer(Object.class, schedTableRenderer);
    final JScrollPane dataScroller = new JScrollPane(mScheduleTable);

    violationTable = new JTable();
    violationTable.setDefaultRenderer(String.class, violationTableRenderer);
    violationTable.getSelectionModel().addListSelectionListener(violationSelectionListener);
    final JScrollPane violationScroller = new JScrollPane(violationTable);

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dataScroller, violationScroller);
    schedulePanel.add(splitPane, BorderLayout.CENTER);

    final JPanel constraintsPanel = new JPanel(new GridBagLayout());
    schedulePanel.add(constraintsPanel, BorderLayout.SOUTH);

    changeDuration = FormatterUtils.createIntegerField(0, 1000);
    changeDuration.setToolTipText("The number of minutes that a team has between any 2 activities");
    addRow(constraintsPanel, new JLabel("Change time duration:"), changeDuration);

    performanceChangeDuration = FormatterUtils.createIntegerField(0, 1000);
    performanceChangeDuration.setToolTipText("The number of minutes that a team has between any 2 performance runs");
    addRow(constraintsPanel, new JLabel("Performance change time duration:"), performanceChangeDuration);

    performanceDuration = FormatterUtils.createIntegerField(1, 1000);
    performanceDuration.setToolTipText("The amount of time that the team is expected to be at the table");
    addRow(constraintsPanel, new JLabel("Performance duration:"), performanceDuration);

    addRow(constraintsPanel, new JLabel("Make sure to pass these values onto your computer person with the schedule!"));

    // --- end schedule panel

    // initial state
    mWriteSchedulesAction.setEnabled(false);
    mDisplayGeneralScheduleAction.setEnabled(false);
    mRunOptimizerAction.setEnabled(false);
    mReloadFileAction.setEnabled(false);

    setSchedParams(new SchedParams());

    pack();
  }

  @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "There is no state needed to be kept here")
  private final transient ListSelectionListener violationSelectionListener = new ListSelectionListener() {
    public void valueChanged(final ListSelectionEvent e) {
      final int selectedRow = getViolationTable().getSelectedRow();
      if (selectedRow == -1) {
        return;
      }

      final ConstraintViolation selected = getViolationsModel().getViolation(selectedRow);
      if (Team.NULL_TEAM_NUMBER != selected.getTeam()) {
        final int teamIndex = getScheduleModel().getIndexOfTeam(selected.getTeam());
        final int displayIndex = getScheduleTable().convertRowIndexToView(teamIndex);
        getScheduleTable().changeSelection(displayIndex, 1, false, false);
      }
    }
  };

  void saveScheduleDescription() {
    if (null == mScheduleDescriptionFile) {
      // prompt the user for a filename to save to

      final String startingDirectory = PREFS.get(DESCRIPTION_STARTING_DIRECTORY_PREF, null);

      final JFileChooser fileChooser = new JFileChooser();
      final FileFilter filter = new BasicFileFilter("FLL Schedule Description (properties)",
                                                    new String[] { "properties" });
      fileChooser.setFileFilter(filter);
      if (null != startingDirectory) {
        fileChooser.setCurrentDirectory(new File(startingDirectory));
      }

      final int returnVal = fileChooser.showSaveDialog(SchedulerUI.this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File currentDirectory = fileChooser.getCurrentDirectory();
        PREFS.put(DESCRIPTION_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());
        
        File selectedFile = fileChooser.getSelectedFile();
        if(!selectedFile.getName().endsWith(".properties")) {
          selectedFile = new File(selectedFile.getAbsolutePath() + ".properties");
        }

        mScheduleDescriptionFile = selectedFile;
        mDescriptionFilename.setText(mScheduleDescriptionFile.getName());
      } else {
        // user canceled
        return;
      }
    }

    try (final Writer writer = new OutputStreamWriter(new FileOutputStream(mScheduleDescriptionFile),
                                                      Utilities.DEFAULT_CHARSET)) {
      final SolverParams params = mScheduleDescriptionEditor.getParams();
      final List<String> errors = params.isValid();
      if (!errors.isEmpty()) {
        final String formattedErrors = errors.stream().collect(Collectors.joining("\n"));
        JOptionPane.showMessageDialog(SchedulerUI.this,
                                      "There are errors that need to be corrected before the description can be saved: "
                                          + formattedErrors,
                                      "Error saving file", JOptionPane.ERROR_MESSAGE);
      } else {
        final Properties properties = new Properties();
        params.save(properties);
        properties.store(writer, null);
      }
    } catch (final IOException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error saving file: %s", e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error saving file", JOptionPane.ERROR_MESSAGE);
    }
  }

  private final Action mSaveScheduleDescriptionAction = new AbstractAction("Save Schedule Description") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Save16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Save24.gif"));
      putValue(SHORT_DESCRIPTION, "Save the schedule description file");
      putValue(MNEMONIC_KEY, KeyEvent.VK_S);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      saveScheduleDescription();
    }
  };

  private final Action mNewScheduleDescriptionAction = new AbstractAction("New Schedule Description") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/New16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/New24.gif"));
      putValue(SHORT_DESCRIPTION, "Createa a new schedule description file");
      putValue(MNEMONIC_KEY, KeyEvent.VK_N);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      newScheduleDescription();
    }
  };

  private void newScheduleDescription() {
    final int result = JOptionPane.showConfirmDialog(SchedulerUI.this,
                                                     "This action will remove any changes to the current schedule and load the defaults. Do you want to continue?",
                                                     "Question", JOptionPane.YES_NO_OPTION);
    if (JOptionPane.YES_OPTION == result) {
      mScheduleDescriptionFile = null;
      mDescriptionFilename.setText("");

      final SolverParams params = new SolverParams();
      mScheduleDescriptionEditor.setParams(params);
    }
  }

  /**
   * Run the scheduler and optionally the table optimizer.
   */
  private void runScheduler() {
    try {
      saveScheduleDescription();

      final SchedulerWorker worker = new SchedulerWorker();

      // make sure the task doesn't start until the window is up
      _progressDialog.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(final ComponentEvent e) {
          _progressDialog.removeComponentListener(this);
          worker.execute();
        }
      });

      _progressDialog.setLocationRelativeTo(SchedulerUI.this);
      _progressDialog.setNote("Running Scheduler");
      _progressDialog.setVisible(true);

    } catch (final IOException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error reading description file: %s", e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error Running Scheduler",
                                    JOptionPane.ERROR_MESSAGE);
    } catch (final ParseException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error parsing description file: %s", e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error Running Scheduler",
                                    JOptionPane.ERROR_MESSAGE);
    } catch (final InvalidParametersException e) {
      LOGGER.error(e.getMessage(), e);
      JOptionPane.showMessageDialog(SchedulerUI.this, e.getMessage(), "Error Running Scheduler",
                                    JOptionPane.ERROR_MESSAGE);
    }

  }

  private final class SchedulerWorker extends SwingWorker<Integer, Void> {
    private final GreedySolver solver;

    public SchedulerWorker() throws IOException, ParseException, InvalidParametersException {
      this.solver = new GreedySolver(mScheduleDescriptionFile, false);
    }

    @Override
    protected Integer doInBackground() {
      return solver.solve(_progressDialog);
    }

    @Override
    protected void done() {
      _progressDialog.setVisible(false);

      try {
        final int numSolutions = this.get();

        if (numSolutions < 1) {
          if (_progressDialog.isCanceled()) {
            JOptionPane.showMessageDialog(SchedulerUI.this, "Scheduler was canceled");
            return;
          }

          JOptionPane.showMessageDialog(SchedulerUI.this, "No solution found");
          return;
        }

        final List<SubjectiveStation> subjectiveStations = solver.getParameters().getSubjectiveStations();

        // this causes mSchedParams, mScheduleData and mScheduleFile to be set
        final File solutionFile = solver.getBestSchedule();
        if (null == solutionFile) {
          JOptionPane.showMessageDialog(SchedulerUI.this, "No valid schedule found", "Error Running Scheduler",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }

        // use the same parameters for checking that the solver used for scheduling
        setSchedParams(solver.getParameters());

        loadScheduleFile(solutionFile, subjectiveStations);

        final int result = JOptionPane.showConfirmDialog(SchedulerUI.this, "Would you like to run the table optimizer?",
                                                         "Question", JOptionPane.YES_NO_OPTION);
        if (JOptionPane.YES_OPTION == result) {
          runTableOptimizer();
        }

      } catch (final ExecutionException e) {
        LOGGER.error("Error executing scheduler", e);
        JOptionPane.showMessageDialog(SchedulerUI.this, e.getMessage(), "Error running scheduler",
                                      JOptionPane.ERROR_MESSAGE);
      } catch (final InterruptedException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Received interrupted exception running scheduler");
        }
        JOptionPane.showMessageDialog(SchedulerUI.this, "Scheduler was interrupted before completing");
        return;
      }

    }
  }

  private final ProgressDialog _progressDialog;

  private final class OptimizerWorker extends SwingWorker<Void, Void> {
    private final TableOptimizer optimizer;

    public OptimizerWorker(final TableOptimizer optimizer) {
      this.optimizer = optimizer;
    }

    @Override
    protected Void doInBackground() {
      // see if we can get a better solution
      optimizer.optimize(_progressDialog);

      return null;
    }

    @Override
    protected void done() {
      _progressDialog.setVisible(false);

      try {
        this.get();

        final File optimizedFile = optimizer.getBestScheduleOutputFile();
        if (null != optimizedFile) {
          loadScheduleFile(optimizedFile, mSchedParams.getSubjectiveStations());
        } else {
          JOptionPane.showMessageDialog(SchedulerUI.this, "No better schedule found");
        }

      } catch (final ExecutionException e) {
        LOGGER.error("Error executing table optimizer", e);
        JOptionPane.showMessageDialog(SchedulerUI.this, e.getMessage(), "Error running table optimizer",
                                      JOptionPane.ERROR_MESSAGE);
      } catch (final InterruptedException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Received interrupted exception running table optimizer");
        }
        return;
      }

    }
  }

  private final Action mRunSchedulerAction = new AbstractAction("Run Scheduler") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/TipOfTheDay16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/TipOfTheDay24.gif"));
      putValue(SHORT_DESCRIPTION, "Run the scheduler on the current description");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_S);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      runScheduler();
    }
  };

  private final Action mOpenScheduleDescriptionAction = new AbstractAction("Open Schedule Description") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open24.gif"));
      putValue(SHORT_DESCRIPTION, "Open the schedule description file");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_S);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final String startingDirectory = PREFS.get(DESCRIPTION_STARTING_DIRECTORY_PREF, null);

      final JFileChooser fileChooser = new JFileChooser();
      final FileFilter filter = new BasicFileFilter("FLL Schedule Description (properties)",
                                                    new String[] { "properties" });
      fileChooser.setFileFilter(filter);
      if (null != startingDirectory) {
        fileChooser.setCurrentDirectory(new File(startingDirectory));
      }

      final int returnVal = fileChooser.showOpenDialog(SchedulerUI.this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File currentDirectory = fileChooser.getCurrentDirectory();
        PREFS.put(DESCRIPTION_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());

        final File selectedFile = fileChooser.getSelectedFile();
        if (null != selectedFile
            && selectedFile.isFile()
            && selectedFile.canRead()) {
          loadScheduleDescription(selectedFile);
        } else if (null != selectedFile) {
          JOptionPane.showMessageDialog(SchedulerUI.this,
                                        new Formatter().format("%s is not a file or is not readable",
                                                               selectedFile.getAbsolutePath()),
                                        "Error reading file", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  };

  private void loadScheduleDescription(final File file) {
    final Properties properties = new Properties();
    try (final Reader reader = new InputStreamReader(new FileInputStream(file), Utilities.DEFAULT_CHARSET)) {
      properties.load(reader);
    } catch (final IOException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error loading file: %s", e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error loading file", JOptionPane.ERROR_MESSAGE);
      return;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(properties.toString());
    }

    try {
      final SolverParams params = new SolverParams();
      params.load(properties);
      mScheduleDescriptionEditor.setParams(params);
    } catch (final ParseException pe) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error parsing file: %s", pe.getMessage());
      LOGGER.error(errorFormatter, pe);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error parsing file", JOptionPane.ERROR_MESSAGE);
      return;
    }
    mScheduleDescriptionFile = file;

    mDescriptionFilename.setText(file.getName());
  }

  private JToolBar createDescriptionToolbar() {
    final JToolBar toolbar = new JToolBar("Description Toolbar");
    toolbar.setFloatable(false);

    toolbar.add(mDescriptionFilename);
    toolbar.addSeparator();
    toolbar.add(mNewScheduleDescriptionAction);
    toolbar.add(mOpenScheduleDescriptionAction);
    toolbar.add(mSaveScheduleDescriptionAction);
    toolbar.add(mRunSchedulerAction);

    return toolbar;
  }

  private JToolBar createScheduleToolbar() {
    final JToolBar toolbar = new JToolBar("Schedule Toolbar");
    toolbar.setFloatable(false);

    toolbar.add(mScheduleFilename);
    toolbar.addSeparator();
    toolbar.add(mOpenScheduleAction);
    toolbar.add(mReloadFileAction);
    toolbar.add(mWriteSchedulesAction);
    toolbar.add(mDisplayGeneralScheduleAction);

    return toolbar;
  }

  private JMenuBar createMenubar() {
    final JMenuBar menubar = new JMenuBar();

    menubar.add(createFileMenu());

    menubar.add(createDescriptionMenu());

    menubar.add(createScheduleMenu());

    return menubar;
  }

  private JMenu createScheduleMenu() {
    final JMenu menu = new JMenu("Schedule");
    menu.setMnemonic('s');

    menu.add(mOpenScheduleAction);
    menu.add(mReloadFileAction);
    menu.add(mRunOptimizerAction);
    menu.add(mWriteSchedulesAction);
    menu.add(mDisplayGeneralScheduleAction);

    return menu;
  }

  private JMenu createDescriptionMenu() {
    final JMenu menu = new JMenu("Description");
    menu.setMnemonic('d');

    menu.add(mNewScheduleDescriptionAction);
    menu.add(mOpenScheduleDescriptionAction);
    menu.add(mSaveScheduleDescriptionAction);
    menu.add(mRunSchedulerAction);

    return menu;
  }

  private JMenu createFileMenu() {
    final JMenu menu = new JMenu("File");
    menu.setMnemonic('f');

    menu.add(mPreferencesAction);
    menu.add(mExitAction);

    return menu;
  }

  private final Action mReloadFileAction = new AbstractAction("Reload Schedule") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Refresh16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Refresh24.gif"));
      putValue(SHORT_DESCRIPTION, "Reload the file and check for violations");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      FileInputStream fis = null;
      try {
        final File selectedFile = getScheduleFile();
        final String sheetName = getCurrentSheetName();
        final String name = Utilities.extractBasename(selectedFile);

        final TournamentSchedule newData;
        if (null == sheetName) {
          // if no sheet name, assume CSV file
          newData = new TournamentSchedule(name, selectedFile, mScheduleData.getSubjectiveStations());
        } else {
          fis = new FileInputStream(selectedFile);
          newData = new TournamentSchedule(name, fis, sheetName, mScheduleData.getSubjectiveStations());
        }
        setScheduleData(newData);
      } catch (final IOException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error reloading file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reloading file",
                                      JOptionPane.ERROR_MESSAGE);
      } catch (ParseException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error reloading file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reloading file",
                                      JOptionPane.ERROR_MESSAGE);
      } catch (final InvalidFormatException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error reloading file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reloading file",
                                      JOptionPane.ERROR_MESSAGE);
      } catch (final ScheduleParseException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error parsing file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error parsing file",
                                      JOptionPane.ERROR_MESSAGE);
      } finally {
        try {
          if (null != fis) {
            fis.close();
          }
        } catch (final IOException e) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exception closing stream", e);
          }
        }

      }
    }
  };

  private final Action mPreferencesAction = new AbstractAction("Preferences") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Preferences16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Preferences24.gif"));
      putValue(SHORT_DESCRIPTION, "Set scheduling preferences");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      JOptionPane.showMessageDialog(SchedulerUI.this, "Not implemented yet");
    }
  };

  private final Action mExitAction = new AbstractAction("Exit") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop24.gif"));
      putValue(SHORT_DESCRIPTION, "Exit the application");
      putValue(MNEMONIC_KEY, KeyEvent.VK_X);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
    }

    public void actionPerformed(final ActionEvent ae) {
      SchedulerUI.this.setVisible(false);
    }
  };

  private final Action mDisplayGeneralScheduleAction = new AbstractAction("General Schedule") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/History16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/History24.gif"));
      putValue(SHORT_DESCRIPTION, "Display the general schedule");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final String schedule = getScheduleData().computeGeneralSchedule();
      JOptionPane.showMessageDialog(SchedulerUI.this, schedule, "General Schedule", JOptionPane.INFORMATION_MESSAGE);
    }
  };

  private final Action mWriteSchedulesAction = new AbstractAction("Write Detailed Schedules") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Export16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Export24.gif"));
      putValue(SHORT_DESCRIPTION, "Write out the detailed schedules as a PDF");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      FileOutputStream scoresheetFos = null;
      try {
        final File directory = getScheduleFile().getParentFile();
        final String baseFilename = Utilities.extractBasename(getScheduleFile());
        LOGGER.info("Writing detailed schedules to "
            + directory.getAbsolutePath());

        getScheduleData().outputDetailedSchedules(getSchedParams(), getScheduleFile().getParentFile(), baseFilename);
        JOptionPane.showMessageDialog(SchedulerUI.this, "Detailed schedule written '"
            + directory.getAbsolutePath()
            + "'", "Information", JOptionPane.INFORMATION_MESSAGE);

        final int answer = JOptionPane.showConfirmDialog(SchedulerUI.this,
                                                         "Would you like to write out the score sheets as well?",
                                                         "Write Out Scoresheets?", JOptionPane.YES_NO_OPTION);
        if (JOptionPane.YES_OPTION == answer) {
          chooseChallengeDescriptor.setLocationRelativeTo(SchedulerUI.this);
          chooseChallengeDescriptor.setVisible(true);
          final URL descriptorLocation = chooseChallengeDescriptor.getSelectedDescription();
          if (null != descriptorLocation) {
            final Reader descriptorReader = new InputStreamReader(descriptorLocation.openStream(),
                                                                  Utilities.DEFAULT_CHARSET);

            final String tournamentName = JOptionPane.showInputDialog(SchedulerUI.this,
                                                                      "What is the name of the tournament to put on the score sheets?");
            if (null != tournamentName) {

              final Document document = ChallengeParser.parse(descriptorReader);
              final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());

              final File scoresheetFile = new File(directory, baseFilename
                  + "-scoresheets.pdf");
              scoresheetFos = new FileOutputStream(scoresheetFile);

              getScheduleData().outputPerformanceSheets(tournamentName, scoresheetFos, description);

              final MapSubjectiveHeaders mapDialog = new MapSubjectiveHeaders(SchedulerUI.this, description,
                                                                              getScheduleData());
              mapDialog.setLocationRelativeTo(SchedulerUI.this);
              mapDialog.setVisible(true);
              if (!mapDialog.isCanceled()) {
                final Map<ScoreCategory, String> categoryToSchedule = new HashMap<>();
                for (final ScoreCategory scoreCategory : description.getSubjectiveCategories()) {
                  final String scheduleColumn = mapDialog.getSubjectiveHeaderForCategory(scoreCategory);
                  if (null == scheduleColumn) {
                    throw new FLLInternalException("Did not find a schedule column for "
                        + scoreCategory.getTitle());
                  }
                  categoryToSchedule.put(scoreCategory, scheduleColumn);
                }

                getScheduleData().outputSubjectiveSheets(tournamentName, directory.getAbsolutePath(), baseFilename,
                                                         description, categoryToSchedule);

                JOptionPane.showMessageDialog(SchedulerUI.this, "Scoresheets written '"
                    + scoresheetFile.getAbsolutePath()
                    + "'", "Information", JOptionPane.INFORMATION_MESSAGE);
              } // not canceled
            } // tournament name
          } // valid descriptor location
        } // yes to write score sheets
      } catch (final DocumentException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error writing detailed schedules: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error", JOptionPane.ERROR_MESSAGE);
        return;
      } catch (final IOException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error writing detailed schedules: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error", JOptionPane.ERROR_MESSAGE);
        return;
      } catch (final SQLException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Unexpected Error writing detailed schedules: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error", JOptionPane.ERROR_MESSAGE);
        return;
      } finally {
        IOUtils.closeQuietly(scoresheetFos);
      }
    }
  };

  /**
   * Run the table optimizer on the current schedule and open the resulting
   * file.
   */
  private void runTableOptimizer() {
    try {
      final TableOptimizer optimizer = new TableOptimizer(mSchedParams, mScheduleData,
                                                          mScheduleFile.getAbsoluteFile().getParentFile());

      final OptimizerWorker optimizerWorker = new OptimizerWorker(optimizer);

      // make sure the task doesn't start until the window is up
      _progressDialog.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(final ComponentEvent e) {
          _progressDialog.removeComponentListener(this);
          optimizerWorker.execute();
        }
      });

      _progressDialog.setLocationRelativeTo(SchedulerUI.this);
      _progressDialog.setNote("Running Table Optimizer");
      _progressDialog.setVisible(true);
    } catch (final IllegalArgumentException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e, e);
      }
      JOptionPane.showMessageDialog(SchedulerUI.this,
                                    "There are errors in the schedule, the table optimizer cannot be run: "
                                        + e.getMessage());
    }

  }

  /**
   * Load the specified schedule file and select the schedule tab.
   * 
   * @param selectedFile
   * @param subjectiveStations if not null, use as the subjective stations,
   *          otherwise prompt the user for the subjective stations
   */
  private void loadScheduleFile(final File selectedFile,
                                final List<SubjectiveStation> subjectiveStations) {
    FileInputStream fis = null;
    try {
      final boolean csv = !ExcelCellReader.isExcelFile(selectedFile);
      final CellFileReader reader;
      final String sheetName;
      if (csv) {
        reader = new CSVCellReader(selectedFile);
        sheetName = null;
      } else {
        sheetName = promptForSheetName(selectedFile);
        if (null == sheetName) {
          return;
        }
        fis = new FileInputStream(selectedFile);
        reader = new ExcelCellReader(fis, sheetName);
      }

      final List<SubjectiveStation> newSubjectiveStations;
      if (null == subjectiveStations) {
        final ColumnInformation columnInfo = TournamentSchedule.findColumns(reader, new LinkedList<String>());
        newSubjectiveStations = gatherSubjectiveStationInformation(SchedulerUI.this, columnInfo);
      } else {
        newSubjectiveStations = subjectiveStations;
      }

      if (null != fis) {
        fis.close();
        fis = null;
      }

      mSchedParams.setSubjectiveStations(newSubjectiveStations);

      final List<String> subjectiveHeaders = new LinkedList<String>();
      for (final SubjectiveStation station : newSubjectiveStations) {
        subjectiveHeaders.add(station.getName());
      }

      final String name = Utilities.extractBasename(selectedFile);

      final TournamentSchedule schedule;
      if (csv) {
        schedule = new TournamentSchedule(name, selectedFile, subjectiveHeaders);
      } else {
        fis = new FileInputStream(selectedFile);
        schedule = new TournamentSchedule(name, fis, sheetName, subjectiveHeaders);
      }
      mScheduleFile = selectedFile;
      mScheduleSheetName = sheetName;
      setScheduleData(schedule);

      setTitle(BASE_TITLE
          + " - "
          + mScheduleFile.getName()
          + ":"
          + mScheduleSheetName);

      mWriteSchedulesAction.setEnabled(true);
      mDisplayGeneralScheduleAction.setEnabled(true);
      mRunOptimizerAction.setEnabled(true);
      mReloadFileAction.setEnabled(true);
      mScheduleFilename.setText(mScheduleFile.getName());

      mTabbedPane.setSelectedIndex(1);
    } catch (final ParseException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error reading file %s: %s", selectedFile.getAbsolutePath(), e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reading file", JOptionPane.ERROR_MESSAGE);
      return;
    } catch (final IOException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error reading file %s: %s", selectedFile.getAbsolutePath(), e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reading file", JOptionPane.ERROR_MESSAGE);
      return;
    } catch (final InvalidFormatException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Unknown file format %s: %s", selectedFile.getAbsolutePath(), e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reading file", JOptionPane.ERROR_MESSAGE);
      return;
    } catch (final ScheduleParseException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error parsing file %s: %s", selectedFile.getAbsolutePath(), e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error parsing file", JOptionPane.ERROR_MESSAGE);
      return;
    } catch (final FLLRuntimeException e) {
      final Formatter errorFormatter = new Formatter();
      errorFormatter.format("Error parsing file %s: %s", selectedFile.getAbsolutePath(), e.getMessage());
      LOGGER.error(errorFormatter, e);
      JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error parsing file", JOptionPane.ERROR_MESSAGE);
      return;
    } finally {
      try {
        if (null != fis) {
          fis.close();
        }
      } catch (final IOException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Error closing stream", e);
        }
      }
    }
  }

  private final Action mRunOptimizerAction = new AbstractAction("Run Table Optimizer") {
    @Override
    public void actionPerformed(final ActionEvent ae) {
      runTableOptimizer();
    }
  };

  private final Action mOpenScheduleAction = new AbstractAction("Open Schedule") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open24.gif"));
      putValue(SHORT_DESCRIPTION, "Open a schedule file");
      putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final String startingDirectory = PREFS.get(SCHEDULE_STARTING_DIRECTORY_PREF, null);

      final JFileChooser fileChooser = new JFileChooser();
      final FileFilter filter = new BasicFileFilter("FLL Schedule (xls, xlsx, csv)",
                                                    new String[] { "xls", "xlsx", "csv" });
      fileChooser.setFileFilter(filter);
      if (null != startingDirectory) {
        fileChooser.setCurrentDirectory(new File(startingDirectory));
      }

      final int returnVal = fileChooser.showOpenDialog(SchedulerUI.this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File currentDirectory = fileChooser.getCurrentDirectory();
        PREFS.put(SCHEDULE_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());

        final File selectedFile = fileChooser.getSelectedFile();
        if (null != selectedFile
            && selectedFile.isFile()
            && selectedFile.canRead()) {

          // use default sched params until the user changes them
          setSchedParams(new SchedParams());

          loadScheduleFile(selectedFile, null);
        } else if (null != selectedFile) {
          JOptionPane.showMessageDialog(SchedulerUI.this,
                                        new Formatter().format("%s is not a file or is not readable",
                                                               selectedFile.getAbsolutePath()),
                                        "Error reading file", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  };

  /**
   * If there is more than 1 sheet, prompt, otherwise just use the sheet.
   * 
   * @return the sheet name or null if the user canceled
   * @throws IOException
   * @throws InvalidFormatException
   */
  public static String promptForSheetName(final File selectedFile) throws InvalidFormatException, IOException {
    final List<String> sheetNames = ExcelCellReader.getAllSheetNames(selectedFile);
    if (sheetNames.size() == 1) {
      return sheetNames.get(0);
    } else {
      final String[] options = sheetNames.toArray(new String[sheetNames.size()]);
      final int choosenOption = JOptionPane.showOptionDialog(null, "Choose which sheet to work with", "Choose Sheet",
                                                             JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                                                             null, options, options[0]);
      if (JOptionPane.CLOSED_OPTION == choosenOption) {
        return null;
      }
      return options[choosenOption];
    }
  }

  private static final Preferences PREFS = Preferences.userNodeForPackage(TournamentSchedule.class);

  private static final String SCHEDULE_STARTING_DIRECTORY_PREF = "scheduleStartingDirectory";

  private static final String DESCRIPTION_STARTING_DIRECTORY_PREF = "descriptionStartingDirectory";

  @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This calss isn't going to be serialized")
  private TournamentSchedule mScheduleData;

  /* package */TournamentSchedule getScheduleData() {
    return mScheduleData;
  }

  private SchedParams mSchedParams;

  public SchedParams getSchedParams() {
    return mSchedParams;
  }

  private void setSchedParams(final SchedParams params) {
    mSchedParams = params;
    changeDuration.setValue(mSchedParams.getChangetimeMinutes());
    performanceChangeDuration.setValue(mSchedParams.getPerformanceChangetimeMinutes());
    performanceDuration.setValue(mSchedParams.getPerformanceMinutes());
  }

  private SchedulerTableModel mScheduleModel;

  SchedulerTableModel getScheduleModel() {
    return mScheduleModel;
  }

  private ViolationTableModel mViolationsModel;

  /* package */ViolationTableModel getViolationsModel() {
    return mViolationsModel;
  }

  private void setScheduleData(final TournamentSchedule sd) {
    mScheduleTable.clearSelection();

    mScheduleData = sd;
    mScheduleModel = new SchedulerTableModel(mScheduleData);
    mScheduleTable.setModel(mScheduleModel);

    checkSchedule();
  }

  /**
   * Verify the existing schedule and update the violations.
   */
  private void checkSchedule() {
    violationTable.clearSelection();

    // make sure sched params are updated based on the UI elements
    final SchedParams params = getSchedParams();

    params.setPerformanceMinutes((Integer) performanceDuration.getValue());
    params.setChangetimeMinutes((Integer) changeDuration.getValue());
    params.setPerformanceChangetimeMinutes((Integer) performanceChangeDuration.getValue());

    final ScheduleChecker checker = new ScheduleChecker(params, getScheduleData());
    mViolationsModel = new ViolationTableModel(checker.verifySchedule());
    violationTable.setModel(mViolationsModel);
  }

  private final JLabel mDescriptionFilename;

  private final JLabel mScheduleFilename;

  private final JTabbedPane mTabbedPane;

  private final SolverParamsEditor mScheduleDescriptionEditor;

  private final JTable mScheduleTable;

  private JTable getScheduleTable() {
    return mScheduleTable;
  }

  private final JTable violationTable;

  JTable getViolationTable() {
    return violationTable;
  }

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final Color HARD_CONSTRAINT_COLOR = Color.RED;

  private static final Color SOFT_CONSTRAINT_COLOR = Color.YELLOW;

  private TableCellRenderer schedTableRenderer = new DefaultTableCellRenderer() {

    @Override
    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      setHorizontalAlignment(CENTER);

      final int tmRow = table.convertRowIndexToModel(row);
      final int tmCol = table.convertColumnIndexToModel(column);
      final TeamScheduleInfo schedInfo = getScheduleModel().getSchedInfo(tmRow);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Checking for violations against team: "
            + schedInfo.getTeamNumber() //
            + " column: "
            + tmCol //
            + " row: "
            + tmRow //
        );
      }

      final SortedSet<ConstraintViolation.Type> violationTypes = new TreeSet<>();
      for (final ConstraintViolation violation : getViolationsModel().getViolations()) {
        if (violation.getTeam() == schedInfo.getTeamNumber()) {
          Collection<SubjectiveTime> subjectiveTimes = violation.getSubjectiveTimes();
          if (tmCol <= SchedulerTableModel.JUDGE_COLUMN
              && subjectiveTimes.isEmpty()
              && null == violation.getPerformance()) {
            // there is an error for this team and the team information fields
            // should be highlighted
            violationTypes.add(violation.getType());
          } else if (null != violation.getPerformance()) {
            // need to check round which round
            int round = 0;
            // using Math.min to handle extra round
            while (!violation.getPerformance().equals(schedInfo.getPerfTime(Math.min(schedInfo.getNumberOfRounds()
                - 1, round)))
                && round < schedInfo.getNumberOfRounds()) {
              ++round;
              if (round > schedInfo.getNumberOfRounds()) {
                throw new RuntimeException("Internal error, walked off the end of the round list");
              }
            }
            // handle extra run
            if (round >= schedInfo.getNumberOfRounds()) {
              round = schedInfo.getNumberOfRounds()
                  - 1;
            }
            final int firstIdx = getScheduleModel().getFirstPerformanceColumn()
                + (round
                    * SchedulerTableModel.NUM_COLUMNS_PER_ROUND);
            final int lastIdx = firstIdx
                + SchedulerTableModel.NUM_COLUMNS_PER_ROUND
                - 1;
            if (firstIdx <= tmCol
                && tmCol <= lastIdx) {
              violationTypes.add(violation.getType());
            }

            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Violation is in performance round: "
                  + round //
                  + " team: "
                  + schedInfo.getTeamNumber() //
                  + " firstIdx: "
                  + firstIdx //
                  + " lastIdx: "
                  + lastIdx //
                  + " column: "
                  + tmCol //
                  + " types: "
                  + violationTypes //
              );
            }
          } else {
            for (final SubjectiveTime subj : subjectiveTimes) {
              if (tmCol == getScheduleModel().getColumnForSubjective(subj.getName())) {
                violationTypes.add(violation.getType());
              }
            }
          }
        }
      }

      // set the background based on the error state
      setForeground(null);
      setBackground(null);
      if (!violationTypes.isEmpty()) {
        final ConstraintViolation.Type maxViolationType = violationTypes.last(); // highest
                                                                                 // number
        final Color violationColor = colorForViolation(maxViolationType);
        if (isSelected) {
          setForeground(violationColor);
        } else {
          setBackground(violationColor);
        }
      }

      if (value instanceof LocalTime) {
        final String strValue = TournamentSchedule.formatTime((LocalTime) value);
        return super.getTableCellRendererComponent(table, strValue, isSelected, hasFocus, row, column);
      } else {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
    }
  };

  private static Color colorForViolation(final ConstraintViolation.Type type) {
    switch (type) {
    case HARD:
      return HARD_CONSTRAINT_COLOR;
    case SOFT:
      return SOFT_CONSTRAINT_COLOR;
    default:
      throw new IllegalArgumentException("Unknown constraint type: "
          + type);
    }
  }

  private TableCellRenderer violationTableRenderer = new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      setHorizontalAlignment(CENTER);

      final int tmRow = table.convertRowIndexToModel(row);

      final ConstraintViolation violation = getViolationsModel().getViolation(tmRow);

      final Color violationColor = colorForViolation(violation.getType());
      setForeground(null);
      setBackground(null);
      if (isSelected) {
        setForeground(violationColor);
      } else {
        setBackground(violationColor);
      }

      setHorizontalAlignment(SwingConstants.LEFT);

      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
  };

  private File mScheduleDescriptionFile = null;

  private File mScheduleFile;

  protected File getScheduleFile() {
    return mScheduleFile;
  }

  private String mScheduleSheetName;

  protected String getCurrentSheetName() {
    return mScheduleSheetName;
  }

  /**
   * Prompt the user for which columns represent subjective categories.
   * 
   * @param parentComponent the parent for the dialog
   * @param columnInfo the column information
   * @return the list of subjective information the user choose
   */
  public static List<SubjectiveStation> gatherSubjectiveStationInformation(final Component parentComponent,
                                                                           final ColumnInformation columnInfo) {
    final List<String> unusedColumns = columnInfo.getUnusedColumns();
    final List<JCheckBox> checkboxes = new LinkedList<JCheckBox>();
    final List<JFormattedTextField> subjectiveDurations = new LinkedList<JFormattedTextField>();
    final Box optionPanel = Box.createVerticalBox();

    optionPanel.add(new JLabel("Specify which columns in the data file are for subjective judging"));

    final JPanel grid = new JPanel(new GridLayout(0, 2));
    optionPanel.add(grid);
    grid.add(new JLabel("Data file column"));
    grid.add(new JLabel("Duration (minutes)"));

    for (final String column : unusedColumns) {
      if (null != column
          && column.length() > 0) {
        final JCheckBox checkbox = new JCheckBox(column);
        checkboxes.add(checkbox);
        final JFormattedTextField duration = new JFormattedTextField(Integer.valueOf(SchedParams.DEFAULT_SUBJECTIVE_MINUTES));
        duration.setColumns(4);
        subjectiveDurations.add(duration);
        grid.add(checkbox);
        grid.add(duration);
      }
    }
    final List<SubjectiveStation> subjectiveHeaders;
    if (!checkboxes.isEmpty()) {
      JOptionPane.showMessageDialog(parentComponent, optionPanel, "Choose Subjective Columns",
                                    JOptionPane.QUESTION_MESSAGE);
      subjectiveHeaders = new LinkedList<SubjectiveStation>();
      for (int i = 0; i < checkboxes.size(); ++i) {
        final JCheckBox box = checkboxes.get(i);
        final JFormattedTextField duration = subjectiveDurations.get(i);
        if (box.isSelected()) {
          subjectiveHeaders.add(new SubjectiveStation(box.getText(), ((Number) duration.getValue()).intValue()));
        }
      }
    } else {
      subjectiveHeaders = Collections.emptyList();
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Subjective headers selected: "
          + subjectiveHeaders);
    }
    return subjectiveHeaders;
  }

  private final JFormattedTextField changeDuration;

  private final JFormattedTextField performanceChangeDuration;

  private final JFormattedTextField performanceDuration;

  /**
   * Add a row of components to the specified container and then add a spacer to
   * the end of the row.
   * The container must have it's layout set to {@link GridBagLayout}.
   * 
   * @param components the components to add
   * @param container where to add the components to
   */
  private static void addRow(final JComponent container,
                             final JComponent... components) {
    GridBagConstraints gbc;

    // for (final JComponent comp : components) {
    for (int i = 0; i < components.length
        - 1; ++i) {
      final JComponent comp = components[i];
      gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weighty = 0;
      container.add(comp, gbc);
    }

    // end of line spacer
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    // add(new JPanel(), gbc);
    container.add(components[components.length
        - 1], gbc);
  }

}
