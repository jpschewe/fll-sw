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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
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
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Utilities;
import fll.scheduler.SchedParams.InvalidParametersException;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLRuntimeException;
import fll.util.FormatterUtils;
import fll.util.GuiExceptionHandler;
import fll.util.ProgressDialog;
import fll.web.StoreColumnNames;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import net.mtu.eggplant.util.BasicFileFilter;

/**
 * UI for the scheduler.
 */
public class SchedulerUI extends JFrame {
  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final Color HARD_CONSTRAINT_COLOR = Color.RED;

  private static final Color SOFT_CONSTRAINT_COLOR = Color.YELLOW;

  private final JTable violationTable = new JTable();

  private final JLabel mDescriptionFilename;

  private final JLabel mScheduleFilename;

  private final JTabbedPane mTabbedPane;

  private final SolverParamsEditor mScheduleDescriptionEditor;

  private final JTable mScheduleTable = new JTable();

  @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This class isn't going to be serialized")
  private TournamentSchedule mScheduleData = new TournamentSchedule();

  private @MonotonicNonNull ColumnInformation columnInfo = null;

  private String tournamentName = "unnamed";

  private SchedulerTableModel mScheduleModel = new SchedulerTableModel(mScheduleData);

  private ViolationTableModel mViolationsModel = new ViolationTableModel(Collections.emptyList());

  /**
   * @param args ignored
   */
  public static void main(final String[] args) {
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

      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    } catch (final Exception e) {
      LOGGER.fatal("Unexpected error", e);
      JOptionPane.showMessageDialog(null,
                                    "An unexpected error occurred. Please send the log file and a description of what you were doing to the developer. Error message: "
                                        + e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private @Nullable ChallengeDescription challengeDescription;

  private final JLabel challengeDescriptionTitle;

  private static final String NO_DESCRIPTION_LOADED = "No Challenge Description Loaded";

  private static final String BASE_TITLE = "FLL Scheduler";

  /**
   * Build the UI.
   */
  public SchedulerUI() {
    super(BASE_TITLE);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    challengeDescription = null;

    progressDialog = new ProgressDialog(SchedulerUI.this, "Please Wait");
    progressDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    challengeDescriptionTitle = new JLabel();
    cpane.add(challengeDescriptionTitle, BorderLayout.NORTH);

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

    mScheduleTable.setAutoCreateRowSorter(true);
    mScheduleTable.setDefaultRenderer(LocalTime.class, schedTableRenderer);
    mScheduleTable.setDefaultRenderer(String.class, schedTableRenderer);
    mScheduleTable.setDefaultRenderer(Integer.class, schedTableRenderer);
    mScheduleTable.setDefaultRenderer(Object.class, schedTableRenderer);
    final JScrollPane dataScroller = new JScrollPane(mScheduleTable);

    violationTable.setDefaultRenderer(String.class, violationTableRenderer);
    violationTable.getSelectionModel().addListSelectionListener(violationSelectionListener);
    final JScrollPane violationScroller = new JScrollPane(violationTable);

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dataScroller, violationScroller);
    schedulePanel.add(splitPane, BorderLayout.CENTER);

    final JPanel constraintsPanel = new JPanel(new GridBagLayout());
    schedulePanel.add(constraintsPanel, BorderLayout.SOUTH);

    changeDuration = FormatterUtils.createIntegerField(0, 1000);
    changeDuration.setToolTipText("The number of minutes that a team has between any performance and subjective events");
    addRow(constraintsPanel, new JLabel("Change time duration:"), changeDuration);

    performanceChangeDuration = FormatterUtils.createIntegerField(0, 1000);
    performanceChangeDuration.setToolTipText("The number of minutes that a team has between any 2 performance events");
    addRow(constraintsPanel, new JLabel("Performance change time duration:"), performanceChangeDuration);

    subjectiveChangeDuration = FormatterUtils.createIntegerField(0, 1000);
    subjectiveChangeDuration.setToolTipText("The number of minutes that a team has between any 2 subjective events");
    addRow(constraintsPanel, new JLabel("Subjective change time duration:"), subjectiveChangeDuration);

    performanceDuration = FormatterUtils.createIntegerField(1, 1000);
    performanceDuration.setToolTipText("The amount of time that the team is expected to be at the table");
    addRow(constraintsPanel, new JLabel("Performance duration:"), performanceDuration);

    addRow(constraintsPanel, new JLabel("Make sure to pass these values onto your computer person with the schedule!"));

    // --- end schedule panel

    // object initialized
    final CheckSchedule durationChangeListener = new CheckSchedule();
    changeDuration.addPropertyChangeListener("value", durationChangeListener);
    performanceChangeDuration.addPropertyChangeListener("value", durationChangeListener);
    subjectiveChangeDuration.addPropertyChangeListener("value", durationChangeListener);
    performanceDuration.addPropertyChangeListener("value", durationChangeListener);

    // Work around https://github.com/typetools/checker-framework/issues/4667 by
    // putting all initialization inside the constructor
    // Working with checker more I suspect this issue is because of
    // https://github.com/typetools/checker-framework/issues/4667 and how
    // @NotOnlyInitialized works
    // final JMenuBar menubar = createMenubar();
    final JMenuBar menubar = new JMenuBar();

    final JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('f');
    fileMenu.add(loadChallengeDescriptionAction);
    fileMenu.add(mPreferencesAction);
    fileMenu.add(mExitAction);
    menubar.add(fileMenu);

    final JMenu descriptionMenu = new JMenu("Description");
    descriptionMenu.setMnemonic('d');
    descriptionMenu.add(mNewScheduleDescriptionAction);
    descriptionMenu.add(mOpenScheduleDescriptionAction);
    descriptionMenu.add(mSaveScheduleDescriptionAction);
    descriptionMenu.add(mSaveAsScheduleDescriptionAction);
    descriptionMenu.add(mRunSchedulerAction);
    menubar.add(descriptionMenu);

    final JMenu scheduleMenu = new JMenu("Schedule");
    scheduleMenu.setMnemonic('s');

    scheduleMenu.add(mOpenScheduleAction);
    scheduleMenu.add(mReloadFileAction);
    scheduleMenu.add(mRunOptimizerAction);
    scheduleMenu.add(mDisplayGeneralScheduleAction);
    scheduleMenu.add(saveScheduleAction);
    menubar.add(scheduleMenu);

    setJMenuBar(menubar);

    // initial state
    mDisplayGeneralScheduleAction.setEnabled(false);
    mRunOptimizerAction.setEnabled(false);
    mReloadFileAction.setEnabled(false);
    saveScheduleAction.setEnabled(false);

    // Once https://github.com/typetools/checker-framework/issues/4613 is resolved I
    // can try and figure out what is wrong here
    // until then I have copied the body of setSchedParams here
    // setSchedParams(mSchedParams);
    changeDuration.setValue(mSchedParams.getChangetimeMinutes());
    performanceChangeDuration.setValue(mSchedParams.getPerformanceChangetimeMinutes());
    subjectiveChangeDuration.setValue(mSchedParams.getSubjectiveChangetimeMinutes());
    performanceDuration.setValue(mSchedParams.getPerformanceMinutes());

    setChallengeDescriptionTitle();

    pack();
  }

  private final class CheckSchedule implements PropertyChangeListener {

    @Override
    public void propertyChange(final PropertyChangeEvent ignored) {
      checkSchedule();
    }

  }

  @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "There is no state needed to be kept here")
  private final transient ListSelectionListener violationSelectionListener = e -> {
    final int selectedRow = violationTable.getSelectedRow();
    if (selectedRow == -1) {
      return;
    }

    final ConstraintViolation selected = mViolationsModel.getViolation(selectedRow);
    if (Team.NULL_TEAM_NUMBER != selected.getTeam()) {
      final int teamIndex = mScheduleModel.getIndexOfTeam(selected.getTeam());
      final int displayIndex = mScheduleTable.convertRowIndexToView(teamIndex);
      mScheduleTable.changeSelection(displayIndex, 1, false, false);
    }
  };

  /**
   * @param chooseFilename if true, always prompt the user for the file name to
   *          save as
   */
  void saveScheduleDescription(final boolean chooseFilename) {
    if (chooseFilename
        || null == mScheduleDescriptionFile) {
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
        if (null != currentDirectory) {
          PREFS.put(DESCRIPTION_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (null == selectedFile) {
          return;
        }

        if (!selectedFile.getName().endsWith(".properties")) {
          selectedFile = new File(selectedFile.getAbsolutePath()
              + ".properties");
        }

        mScheduleDescriptionFile = selectedFile;
        mDescriptionFilename.setText(mScheduleDescriptionFile.getName());
      } else {
        // user canceled
        return;
      }
    }

    // mScheduleDescriptionFile needs to be non-null by now
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(castNonNull(mScheduleDescriptionFile)),
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

  private final Action mSaveScheduleDescriptionAction = new SaveScheduleDescriptionAction();

  private final class SaveScheduleDescriptionAction extends AbstractAction {
    SaveScheduleDescriptionAction() {
      super("Save Schedule Description");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/Save16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/Save24.gif"));
      putValue(SHORT_DESCRIPTION, "Save the schedule description file");
      putValue(MNEMONIC_KEY, KeyEvent.VK_S);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      saveScheduleDescription(false);
    }
  }

  private final Action mSaveAsScheduleDescriptionAction = new SaveAsScheduleDescriptionAction();

  private final class SaveAsScheduleDescriptionAction extends AbstractAction {
    SaveAsScheduleDescriptionAction() {
      super("Save As Schedule Description");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/SaveAs16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/SaveAs24.gif"));
      putValue(SHORT_DESCRIPTION, "Save the schedule description file to a new file");
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      saveScheduleDescription(true);
    }
  }

  private final Action mNewScheduleDescriptionAction = new NewScheduleDescriptionAction();

  private final class NewScheduleDescriptionAction extends AbstractAction {
    NewScheduleDescriptionAction() {
      super("New Schedule Description");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/New16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/New24.gif"));
      putValue(SHORT_DESCRIPTION, "Createa a new schedule description file");
      putValue(MNEMONIC_KEY, KeyEvent.VK_N);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      newScheduleDescription();
    }
  }

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
      saveScheduleDescription(false);

      if (null == mScheduleDescriptionFile) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Schedule description not saved, cannot run scheduler");
        LOGGER.error(errorFormatter);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Cannot Run Scheduler",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }

      // description not null by now
      final SchedulerWorker worker = new SchedulerWorker(castNonNull(mScheduleDescriptionFile));

      // make sure the task doesn't start until the window is up
      progressDialog.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(final ComponentEvent e) {
          progressDialog.removeComponentListener(this);
          worker.execute();
        }
      });

      progressDialog.setLocationRelativeTo(SchedulerUI.this);
      progressDialog.setNote("Running Scheduler");
      progressDialog.setVisible(true);

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

    SchedulerWorker(final File descriptionFile) throws IOException, ParseException, InvalidParametersException {
      this.solver = new GreedySolver(descriptionFile, false);
    }

    @Override
    protected Integer doInBackground() {
      return solver.solve(progressDialog);
    }

    @Override
    protected void done() {
      progressDialog.setVisible(false);

      try {
        final int numSolutions = this.get();

        if (numSolutions < 1) {
          if (progressDialog.isCanceled()) {
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

  private final @NotOnlyInitialized ProgressDialog progressDialog;

  private final class OptimizerWorker extends SwingWorker<Void, Void> {
    private final TableOptimizer optimizer;

    OptimizerWorker(final TableOptimizer optimizer) {
      this.optimizer = optimizer;
    }

    @Override
    protected Void doInBackground() {
      // see if we can get a better solution
      optimizer.optimize(progressDialog);

      return null;
    }

    @Override
    protected void done() {
      progressDialog.setVisible(false);

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

  private final Action mRunSchedulerAction = new RunSchedulerAction();

  private final class RunSchedulerAction extends AbstractAction {
    RunSchedulerAction() {
      super("Run Scheduler");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/TipOfTheDay16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/TipOfTheDay24.gif"));
      putValue(SHORT_DESCRIPTION, "Run the scheduler on the current description");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_S);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      runScheduler();
    }
  }

  private final Action mOpenScheduleDescriptionAction = new OpenScheduleDescriptionAction();

  private final class OpenScheduleDescriptionAction extends AbstractAction {
    OpenScheduleDescriptionAction() {
      super("Open Schedule Description");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/Open16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/Open24.gif"));
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
        if (null != currentDirectory) {
          PREFS.put(DESCRIPTION_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());
        }

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
  }

  private void loadScheduleDescription(final File file) {
    final Properties properties = new Properties();
    try (Reader reader = new InputStreamReader(new FileInputStream(file), Utilities.DEFAULT_CHARSET)) {
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

  @RequiresNonNull({ "mDescriptionFilename", "mNewScheduleDescriptionAction", "mOpenScheduleDescriptionAction",
                     "mSaveScheduleDescriptionAction", "mRunSchedulerAction" })
  private JToolBar createDescriptionToolbar(@UnknownInitialization(JFrame.class) SchedulerUI this) {
    final JToolBar toolbar = new JToolBar("Description Toolbar");
    toolbar.setFloatable(false);

    toolbar.add(mDescriptionFilename);
    toolbar.addSeparator();
    toolbar.add(mNewScheduleDescriptionAction);
    toolbar.add(mOpenScheduleDescriptionAction);
    toolbar.add(mSaveScheduleDescriptionAction);
    toolbar.add(mSaveAsScheduleDescriptionAction);
    toolbar.add(mRunSchedulerAction);

    return toolbar;
  }

  @RequiresNonNull({ "mScheduleFilename", "mOpenScheduleAction", "mReloadFileAction", "mDisplayGeneralScheduleAction" })
  private JToolBar createScheduleToolbar(@UnderInitialization(JFrame.class) SchedulerUI this) {
    final JToolBar toolbar = new JToolBar("Schedule Toolbar");
    toolbar.setFloatable(false);

    toolbar.add(mScheduleFilename);
    toolbar.addSeparator();
    toolbar.add(mOpenScheduleAction);
    toolbar.add(mReloadFileAction);
    toolbar.add(mDisplayGeneralScheduleAction);

    return toolbar;
  }

  private final Action mReloadFileAction = new ReloadFileAction();

  private final class ReloadFileAction extends AbstractAction {
    ReloadFileAction() {
      super("Reload Schedule");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/Refresh16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/Refresh24.gif"));
      putValue(SHORT_DESCRIPTION, "Reload the file and check for violations");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      try {
        final File selectedFile = getScheduleFile();
        final String sheetName = getCurrentSheetName();

        if (null == columnInfo) {
          throw new IllegalStateException("Cannot reload a schedule without having specified the column information");
        }

        final TournamentSchedule newData = new TournamentSchedule(tournamentName,
                                                                  CellFileReader.createCellReader(selectedFile,
                                                                                                  sheetName),
                                                                  columnInfo);
        setScheduleData(newData, columnInfo);
      } catch (final IOException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error reloading file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reloading file",
                                      JOptionPane.ERROR_MESSAGE);
      } catch (final ParseException e) {
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
      }
    }
  }

  private final Action mPreferencesAction = new PreferencesAction();

  private final class PreferencesAction extends AbstractAction {
    PreferencesAction() {
      super("Preferences");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/Preferences16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/Preferences24.gif"));
      putValue(SHORT_DESCRIPTION, "Set scheduling preferences");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      JOptionPane.showMessageDialog(SchedulerUI.this, "Not implemented yet");
    }
  }

  private final Action mExitAction = new ExitAction();

  private final class ExitAction extends AbstractAction {
    ExitAction() {
      super("Exit");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/Stop16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/Stop24.gif"));
      putValue(SHORT_DESCRIPTION, "Exit the application");
      putValue(MNEMONIC_KEY, KeyEvent.VK_X);
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      SchedulerUI.this.setVisible(false);
    }
  }

  private final Action mDisplayGeneralScheduleAction = new DisplayGeneralScheduleAction();

  private final class DisplayGeneralScheduleAction extends AbstractAction {
    DisplayGeneralScheduleAction() {
      super("General Schedule");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/History16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/History24.gif"));
      putValue(SHORT_DESCRIPTION, "Display the general schedule");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final List<TournamentSchedule.GeneralSchedule> generalSchedule = getScheduleData().computeGeneralSchedule();

      final StringBuilder schedule = new StringBuilder();
      for (final var gs : generalSchedule) {
        if (null != gs.wave()) {
          schedule.append(String.format("Wave %s%n", gs.wave()));
        }

        schedule.append(String.format("Judging %s - %s%n", TournamentSchedule.humanFormatTime(gs.subjectiveStart()),
                                      TournamentSchedule.humanFormatTime(gs.subjectiveEnd())));
        schedule.append(String.format("Robot Game %s - %s%n", TournamentSchedule.humanFormatTime(gs.performanceStart()),
                                      TournamentSchedule.humanFormatTime(gs.performanceEnd())));
        schedule.append(String.format("%n"));
      }

      JOptionPane.showMessageDialog(SchedulerUI.this, schedule.toString(), "General Schedule",
                                    JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private final Action saveScheduleAction = new SaveScheduleAction();

  private final class SaveScheduleAction extends AbstractAction {
    SaveScheduleAction() {
      super("Save Schedule");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/Save16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/Save24.gif"));
      putValue(SHORT_DESCRIPTION, "Save changes made to the schedule");
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final JFileChooser fileChooser = new JFileChooser();
      final FileFilter filter = new BasicFileFilter("Schedule File (csv)", new String[] { "csv" });
      fileChooser.setFileFilter(filter);
      final File directory = getScheduleFile().getParentFile();
      if (null != directory) {
        fileChooser.setCurrentDirectory(directory);
      }

      final int returnVal = fileChooser.showSaveDialog(SchedulerUI.this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        if (null == selectedFile) {
          // user canceled
          return;
        }

        if (!selectedFile.getName().endsWith(".csv")) {
          selectedFile = new File(selectedFile.getAbsolutePath()
              + ".csv");
        }

        try (OutputStream os = new FileOutputStream(selectedFile)) {
          getScheduleData().outputScheduleAsCSV(os);
        } catch (IOException e) {
          final Formatter errorFormatter = new Formatter();
          errorFormatter.format("Unexpected error writing schedule: %s", e.getMessage());
          LOGGER.error(errorFormatter, e);
          JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }

      } else {
        // user canceled
        return;
      }
    }
  }

  private List<List<String>> promptForTableGroups() {
    final Collection<String> tableColors = mScheduleData.getTableColors();

    final TableGroupDialog dialog = new TableGroupDialog(this, tableColors);
    dialog.pack();

    // center window
    dialog.setLocationRelativeTo(null);

    dialog.setVisible(true);

    final List<List<String>> tableGroups = dialog.getTableGroups();

    dialog.dispose();

    return tableGroups;
  }

  /**
   * Run the table optimizer on the current schedule and open the resulting
   * file.
   */
  private void runTableOptimizer() {
    try {
      File scheduleDirectory = mScheduleFile.getAbsoluteFile().getParentFile();
      if (null == scheduleDirectory) {
        // use current working directory
        scheduleDirectory = new File(".");
      }

      final List<List<String>> tableGroups = promptForTableGroups();

      final TableOptimizer optimizer = new TableOptimizer(mSchedParams, mScheduleData, scheduleDirectory, tableGroups);

      final OptimizerWorker optimizerWorker = new OptimizerWorker(optimizer);

      // make sure the task doesn't start until the window is up
      progressDialog.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(final ComponentEvent e) {
          progressDialog.removeComponentListener(this);
          optimizerWorker.execute();
        }
      });

      progressDialog.setLocationRelativeTo(SchedulerUI.this);
      progressDialog.setNote("Running Table Optimizer");
      progressDialog.setVisible(true);
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
   * @return header row or -1 if canceled
   */
  private static int findHeaderRowIndex(final JFrame owner,
                                        final File scheduleFile,
                                        final @Nullable String sheetName) {
    final SelectHeaderRowDialog dialog = new SelectHeaderRowDialog(owner, scheduleFile, sheetName);
    dialog.setLocationRelativeTo(owner);
    dialog.setVisible(true);
    LOGGER.debug("findHeaderRow: Canceled? {} header row index {}", dialog.isCanceled(), dialog.getHeaderRowIndex());

    if (dialog.isCanceled()) {
      return -1;
    } else {
      return dialog.getHeaderRowIndex();
    }
  }

  private static int promptUserForInt(final JFrame owner,
                                      final String message,
                                      final int defaultValue) {
    while (true) {
      final @Nullable String str = JOptionPane.showInputDialog(owner, message, defaultValue);
      if (null == str) {
        JOptionPane.showMessageDialog(owner, "Please enter an integer", "Error", JOptionPane.ERROR_MESSAGE);
      } else {
        try {
          final int value = Integer.parseInt(str);
          return value;
        } catch (final NumberFormatException e) {
          JOptionPane.showMessageDialog(owner, "Please enter an integer", "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  private static ColumnInformation promptForColumns(final JFrame owner,
                                                    final File file,
                                                    final @Nullable String sheetName,
                                                    final int headerRowIndex,
                                                    final Collection<String> headerNames,
                                                    final ChallengeDescription description)
      throws InvalidFormatException, IOException {

    final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);
    reader.skipRows(headerRowIndex);
    final @Nullable String @Nullable [] headerRow = reader.readNext();
    if (null == headerRow) {
      throw new FLLRuntimeException("No data in the file");
    }

    final int numPerformanceRounds = promptUserForInt(owner, "Enter the number of scheduled performance rounds", 1);

    final ChooseScheduleHeadersDialog dialog = new ChooseScheduleHeadersDialog(owner, headerNames, numPerformanceRounds,
                                                                               description);
    dialog.setLocationRelativeTo(owner);
    dialog.setVisible(true);

    final ColumnInformation columnInfo = dialog.createColumnInformation(headerRowIndex, headerRow);

    dialog.dispose();

    return columnInfo;
  }

  /**
   * Load the specified schedule file and select the schedule tab.
   *
   * @param selectedFile
   * @param subjectiveStations if not null, use as the subjective stations,
   *          otherwise prompt the user for the subjective stations
   */
  private void loadScheduleFile(final File selectedFile,
                                final @Nullable List<SubjectiveStation> subjectiveStations) {
    try {
      final boolean csv = !ExcelCellReader.isExcelFile(selectedFile);
      final @Nullable String sheetName;
      if (csv) {
        sheetName = null;
      } else {
        sheetName = promptForSheetName(this, selectedFile);
        if (null == sheetName) {
          return;
        }
      }

      final int headerRowIndex = findHeaderRowIndex(this, selectedFile, sheetName);
      if (headerRowIndex < 0) {
        return;
      }

      final Collection<String> headerNames = StoreColumnNames.extractHeaderNames(selectedFile.toPath(), sheetName,
                                                                                 headerRowIndex);

      if (null == challengeDescription) {
        promptForChallengeDescription();
      }
      final ChallengeDescription description = challengeDescription;
      if (null == description) {
        return;
      }

      final ColumnInformation columnInfo = promptForColumns(this, selectedFile, sheetName, headerRowIndex, headerNames,
                                                            description);

      final List<SubjectiveStation> newSubjectiveStations;
      if (null == subjectiveStations) {
        newSubjectiveStations = specifySubjectivateStationDurations(SchedulerUI.this, columnInfo);
      } else {
        newSubjectiveStations = subjectiveStations;
      }

      mSchedParams.setSubjectiveStations(newSubjectiveStations);

      final String chosenTournamentName = JOptionPane.showInputDialog(SchedulerUI.this,
                                                                      "What is the name of the tournament to put on the score sheets?");
      if (null == chosenTournamentName) {
        tournamentName = "No_Name";
      } else {
        tournamentName = chosenTournamentName;
      }

      final TournamentSchedule schedule = new TournamentSchedule(tournamentName,
                                                                 CellFileReader.createCellReader(selectedFile,
                                                                                                 sheetName),
                                                                 columnInfo);
      mScheduleFile = selectedFile;
      mScheduleSheetName = sheetName;
      setScheduleData(schedule, columnInfo);

      setTitle(BASE_TITLE
          + " - "
          + mScheduleFile.getName()
          + ":"
          + mScheduleSheetName);

      mDisplayGeneralScheduleAction.setEnabled(true);
      mRunOptimizerAction.setEnabled(true);
      mReloadFileAction.setEnabled(true);
      mScheduleFilename.setText(mScheduleFile.getName());
      saveScheduleAction.setEnabled(true);

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
    }
  }

  private final Action mRunOptimizerAction = new RunOptimizerAction();

  private final class RunOptimizerAction extends AbstractAction {
    RunOptimizerAction() {
      super("Run Table Optimizer");
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      runTableOptimizer();
    }
  }

  private final Action mOpenScheduleAction = new OpenScheduleAction();

  private final class OpenScheduleAction extends AbstractAction {
    OpenScheduleAction() {
      super("Open Schedule");
      putValue(SMALL_ICON, Utilities.getIcon("toolbarButtonGraphics/general/Open16.gif"));
      putValue(LARGE_ICON_KEY, Utilities.getIcon("toolbarButtonGraphics/general/Open24.gif"));
      putValue(SHORT_DESCRIPTION, "Open a schedule file");
      putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "False positive in SpotBugs thinking that the second parameter to loaodScheduleFile cannot be null")
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
        if (null != currentDirectory) {
          PREFS.put(SCHEDULE_STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());
        }

        final File selectedFile = fileChooser.getSelectedFile();
        if (null != selectedFile
            && selectedFile.isFile()
            && selectedFile.canRead()) {

          pauseCheckSchedule();

          // use default sched params until the user changes them
          setSchedParams(new SchedParams());
          setScheduleData(new TournamentSchedule(), ColumnInformation.NULL);

          loadScheduleFile(selectedFile, null);

          resumeCheckSchedule();
        } else if (null != selectedFile) {
          JOptionPane.showMessageDialog(SchedulerUI.this,
                                        new Formatter().format("%s is not a file or is not readable",
                                                               selectedFile.getAbsolutePath()),
                                        "Error reading file", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  /**
   * If there is more than 1 sheet, prompt, otherwise just use the sheet.
   *
   * @param owner the owning frame for modal dialogs
   * @param selectedFile the file to read
   * @return the sheet name or null if the user canceled
   * @throws IOException if there is an error reading the file
   * @throws InvalidFormatException if there is an error decoding the file
   */
  private static @Nullable String promptForSheetName(final JFrame owner,
                                                     final File selectedFile)
      throws InvalidFormatException, IOException {
    final List<String> sheetNames = ExcelCellReader.getAllSheetNames(selectedFile);
    if (sheetNames.size() == 1) {
      return sheetNames.get(0);
    } else {
      final String[] options = sheetNames.toArray(new String[sheetNames.size()]);
      final int choosenOption = JOptionPane.showOptionDialog(owner, "Choose which sheet to work with", "Choose Sheet",
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

  /* package */
  TournamentSchedule getScheduleData() {
    return mScheduleData;
  }

  private SchedParams mSchedParams = new SchedParams();

  /**
   * @return the current schedule parameters
   */
  public SchedParams getSchedParams() {
    return mSchedParams;
  }

  private void setSchedParams(@UnknownInitialization(SchedulerUI.class) SchedulerUI this,
                              final SchedParams params) {
    mSchedParams = params;
    changeDuration.setValue(mSchedParams.getChangetimeMinutes());
    performanceChangeDuration.setValue(mSchedParams.getPerformanceChangetimeMinutes());
    subjectiveChangeDuration.setValue(mSchedParams.getSubjectiveChangetimeMinutes());
    performanceDuration.setValue(mSchedParams.getPerformanceMinutes());
  }

  @EnsuresNonNull("this.columnInfo")
  private void setScheduleData(final TournamentSchedule sd,
                               final ColumnInformation columnInfo) {
    mScheduleTable.clearSelection();

    mScheduleData = sd;
    mScheduleModel = new SchedulerTableModel(mScheduleData);
    mScheduleTable.setModel(mScheduleModel);
    this.columnInfo = columnInfo;

    checkSchedule();
  }

  private boolean pauseCheckSchedule = false;

  private void pauseCheckSchedule() {
    pauseCheckSchedule = true;
  }

  private void resumeCheckSchedule() {
    pauseCheckSchedule = false;
    checkSchedule();
  }

  /**
   * Verify the existing schedule and update the violations.
   */
  private void checkSchedule() {
    if (pauseCheckSchedule) {
      LOGGER.debug("Skipping schedule check during pause");
      return;
    }

    violationTable.clearSelection();

    // make sure sched params are updated based on the UI elements
    final SchedParams params = getSchedParams();

    params.setPerformanceMinutes((Integer) performanceDuration.getValue());
    params.setChangetimeMinutes((Integer) changeDuration.getValue());
    params.setPerformanceChangetimeMinutes((Integer) performanceChangeDuration.getValue());
    params.setSubjectiveChangetimeMinutes((Integer) subjectiveChangeDuration.getValue());

    final ScheduleChecker checker = new ScheduleChecker(params, getScheduleData());
    mViolationsModel = new ViolationTableModel(checker.verifySchedule());
    violationTable.setModel(mViolationsModel);
    mScheduleTable.repaint();
  }

  private final TableCellRenderer schedTableRenderer = new SchedTableRenderer();

  private final class SchedTableRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(final JTable table,
                                                   final @Nullable Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      setHorizontalAlignment(CENTER);

      final int tmRow = table.convertRowIndexToModel(row);
      final int tmCol = table.convertColumnIndexToModel(column);
      final TeamScheduleInfo schedInfo = mScheduleModel.getSchedInfo(tmRow);
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
      for (final ConstraintViolation violation : mViolationsModel.getViolations()) {
        if (violation.getTeam() == schedInfo.getTeamNumber()) {
          final Collection<SubjectiveTime> subjectiveTimes = violation.getSubjectiveTimes();
          if (tmCol <= SchedulerTableModel.JUDGE_COLUMN
              && subjectiveTimes.isEmpty()
              && null == violation.getPerformance()) {
            // there is an error for this team and the team information fields
            // should be highlighted
            violationTypes.add(violation.getType());
          } else if (null != violation.getPerformance()) {
            final LocalTime violationPerformanceTime = violation.getPerformance();

            final Pair<PerformanceTime, Long> performanceRoundResult = schedInfo.enumeratePerformances()
                                                                                .filter(p -> p.getLeft().getTime()
                                                                                              .equals(violationPerformanceTime))
                                                                                .findFirst().orElse(null);
            final int firstIdx;
            if (null != performanceRoundResult) {
              firstIdx = performanceRoundResult.getRight().intValue()
                  * SchedulerTableModel.NUM_COLUMNS_PER_ROUND;
            } else {
              throw new RuntimeException("Internal error, cannot find performance at time "
                  + violationPerformanceTime);
            }

            final int lastIdx = firstIdx
                + SchedulerTableModel.NUM_COLUMNS_PER_ROUND
                - 1;
            if (firstIdx <= tmCol
                && tmCol <= lastIdx) {
              violationTypes.add(violation.getType());
            }

            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Violation "
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
              if (tmCol == mScheduleModel.getColumnForSubjective(subj.getName())) {
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
        final String strValue = TournamentSchedule.humanFormatTime((LocalTime) value);
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

  private final TableCellRenderer violationTableRenderer = new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(final JTable table,
                                                   final @Nullable Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      setHorizontalAlignment(CENTER);

      final int tmRow = table.convertRowIndexToModel(row);

      final ConstraintViolation violation = mViolationsModel.getViolation(tmRow);

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

  private @Nullable File mScheduleDescriptionFile = null;

  // the default value should not be used as the actions are disabled to prevent
  // this
  private File mScheduleFile = new File("dummy.csv");

  /**
   * @return the currently loaded file
   */
  protected File getScheduleFile() {
    return mScheduleFile;
  }

  private @Nullable String mScheduleSheetName;

  /**
   * @return the name of the current sheet
   */
  protected @Nullable String getCurrentSheetName() {
    return mScheduleSheetName;
  }

  /**
   * Prompt the user for the duration of each judging station
   *
   * @param parentComponent the parent for the dialog
   * @param columnInfo the column information
   * @return the list of subjective information the user choose
   */
  private static List<SubjectiveStation> specifySubjectivateStationDurations(final Component parentComponent,
                                                                             final ColumnInformation columnInfo) {
    final Box optionPanel = Box.createVerticalBox();

    optionPanel.add(new JLabel("Specify the durations for each judging station"));

    final JPanel grid = new JPanel(new GridLayout(0, 2));
    optionPanel.add(grid);
    grid.add(new JLabel("Judging Station"));
    grid.add(new JLabel("Duration (minutes)"));

    final Map<String, JFormattedTextField> fields = new HashMap<>();
    for (final String judgingStation : columnInfo.getSubjectiveStationNames()) {
      final JLabel label = new JLabel(judgingStation);
      final JFormattedTextField duration = new JFormattedTextField(Integer.valueOf(SchedParams.DEFAULT_SUBJECTIVE_MINUTES));
      duration.setColumns(4);
      grid.add(label);
      grid.add(duration);
      fields.put(judgingStation, duration);
    }

    JOptionPane.showMessageDialog(parentComponent, optionPanel, "Choose Subjective Durations",
                                  JOptionPane.QUESTION_MESSAGE);

    final List<SubjectiveStation> subjectiveHeaders = fields.entrySet().stream() //
                                                            .map(e -> new SubjectiveStation(e.getKey(),
                                                                                            ((Number) e.getValue()
                                                                                                       .getValue()).intValue())) //
                                                            .collect(Collectors.toList());

    LOGGER.trace("Subjective durations: {}", subjectiveHeaders);

    return subjectiveHeaders;
  }

  private final JFormattedTextField changeDuration;

  private final JFormattedTextField performanceChangeDuration;

  private final JFormattedTextField subjectiveChangeDuration;

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

  /**
   * Prompt the user for a challenge description and set
   * {@code challengeDescription}. If {@code challengeDescription} is null after
   * calling this method the user canceled or there was an error (and the user was
   * notified of it).
   */
  private void promptForChallengeDescription() {
    challengeDescription = null;

    final ChooseChallengeDescriptor dialog = new ChooseChallengeDescriptor(this);
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
    final URL descriptorLocation = dialog.getSelectedDescription();
    if (null != descriptorLocation) {
      try {
        final Reader descriptorReader = new InputStreamReader(descriptorLocation.openStream(),
                                                              Utilities.DEFAULT_CHARSET);
        challengeDescription = ChallengeParser.parse(descriptorReader);
      } catch (final IOException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Loading the challenge description: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(this, errorFormatter, "Error", JOptionPane.ERROR_MESSAGE);
      }
    }

    dialog.dispose();

    setChallengeDescriptionTitle();
  }

  @RequiresNonNull({ "challengeDescriptionTitle" })
  private void setChallengeDescriptionTitle(@UnknownInitialization(JFrame.class) SchedulerUI this) {
    final String title;
    if (null == challengeDescription) {
      title = NO_DESCRIPTION_LOADED;
    } else {
      title = challengeDescription.getTitle();
    }
    challengeDescriptionTitle.setText(String.format("Challenge: %s", title));
  }

  private final Action loadChallengeDescriptionAction = new LoadChallengeDescriptionAction();

  private final class LoadChallengeDescriptionAction extends AbstractAction {
    LoadChallengeDescriptionAction() {
      super("Load Challenge Description");
      // putValue(SMALL_ICON,
      // Utilities.getIcon("toolbarButtonGraphics/general/Preferences16.gif"));
      // putValue(LARGE_ICON_KEY,
      // Utilities.getIcon("toolbarButtonGraphics/general/Preferences24.gif"));
      // putValue(SHORT_DESCRIPTION, "Set scheduling preferences");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      promptForChallengeDescription();
    }
  }
}
