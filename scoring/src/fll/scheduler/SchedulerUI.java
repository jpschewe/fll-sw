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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.mtu.eggplant.util.BasicFileFilter;
import net.mtu.eggplant.util.gui.BasicWindowMonitor;
import net.mtu.eggplant.util.gui.GraphicsUtils;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.itextpdf.text.DocumentException;

import fll.util.ExcelCellReader;

/**
 * UI for the scheduler.
 */
public class SchedulerUI extends JFrame {

  public static void main(final String[] args) {
    // Use cross platform look and feel so that things look right all of the time 
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

      frame.pack();
      frame.setSize(frame.getPreferredSize());
      frame.addWindowListener(new BasicWindowMonitor());
      GraphicsUtils.centerWindow(frame);

      frame.setVisible(true);
    } catch (final Exception e) {
      LOGGER.fatal("Unexpected error", e);
      JOptionPane.showMessageDialog(null, "Unexpected error: "
          + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }

  public SchedulerUI() {
    super("FLL Scheduler");
    setJMenuBar(createMenubar());

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add(createToolbar(), BorderLayout.PAGE_START);
    scheduleTable = new JTable();
    scheduleTable.setAutoCreateRowSorter(true);
    scheduleTable.setDefaultRenderer(Date.class, schedTableRenderer);
    scheduleTable.setDefaultRenderer(String.class, schedTableRenderer);
    scheduleTable.setDefaultRenderer(Integer.class, schedTableRenderer);
    scheduleTable.setDefaultRenderer(Object.class, schedTableRenderer);

    final JScrollPane dataScroller = new JScrollPane(scheduleTable);

    violationTable = new JTable();
    final JScrollPane violationScroller = new JScrollPane(violationTable);
    violationTable.getSelectionModel().addListSelectionListener(violationSelectionListener);

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dataScroller, violationScroller);
    cpane.add(splitPane, BorderLayout.CENTER);

  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "There is no state needed to be kept here")
  private final transient ListSelectionListener violationSelectionListener = new ListSelectionListener() {
    public void valueChanged(final ListSelectionEvent e) {
      final int selectedRow = getViolationTable().getSelectedRow();
      if (selectedRow == -1) {
        return;
      }

      final ConstraintViolation selected = getViolationsModel().getViolation(selectedRow);
      if (ConstraintViolation.NO_TEAM != selected.getTeam()) {
        final int teamIndex = getScheduleModel().getIndexOfTeam(selected.getTeam());
        getScheduleTable().changeSelection(teamIndex, 1, false, false);        
      }
    }
  };

  private JToolBar createToolbar() {
    final JToolBar toolbar = new JToolBar("SchedulerUI Main Toolbar");

    toolbar.add(openAction);
    toolbar.add(reloadFileAction);
    toolbar.add(writeSchedulesAction);
    toolbar.add(displayGeneralScheduleAction);
    toolbar.add(preferencesAction);

    return toolbar;
  }

  private JMenuBar createMenubar() {
    final JMenuBar menubar = new JMenuBar();

    menubar.add(createFileMenu());

    return menubar;
  }

  private JMenu createFileMenu() {
    final JMenu menu = new JMenu("File");
    menu.setMnemonic('f');

    menu.add(openAction);
    menu.add(reloadFileAction);
    menu.add(EXIT_ACTION);

    return menu;
  }

  private final Action reloadFileAction = new AbstractAction("Reload File") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Refresh16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Refresh24.gif"));
      putValue(SHORT_DESCRIPTION, "Reload the file");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    public void actionPerformed(final ActionEvent ae) {
      try {
        final ParseSchedule newData = new ParseSchedule(getScheduleData().getFile(), getScheduleData().getSheetName());
        setScheduleData(newData);
      } catch (final IOException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error reloading file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reloading file", JOptionPane.ERROR_MESSAGE);
      } catch (ParseException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error reloading file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reloading file", JOptionPane.ERROR_MESSAGE);
      } catch (final InvalidFormatException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error reloading file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reloading file", JOptionPane.ERROR_MESSAGE);
      } catch (final ScheduleParseException e) {
        final Formatter errorFormatter = new Formatter();
        errorFormatter.format("Error parsing file: %s", e.getMessage());
        LOGGER.error(errorFormatter, e);
        JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error parsing file", JOptionPane.ERROR_MESSAGE);
      }
    }
  };

  private final Action preferencesAction = new AbstractAction("Preferences") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Preferences16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Preferences24.gif"));
      putValue(SHORT_DESCRIPTION, "Set scheduling preferences");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    public void actionPerformed(final ActionEvent ae) {
      JOptionPane.showMessageDialog(SchedulerUI.this, "Not implemented yet");
    }
  };

  private static final Action EXIT_ACTION = new AbstractAction("Exit") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop24.gif"));
      putValue(SHORT_DESCRIPTION, "Exit the application");
      putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_EXIT", justification = "This is the exit method for the application")
    public void actionPerformed(final ActionEvent ae) {
      System.exit(0);
    }
  };

  private final Action displayGeneralScheduleAction = new AbstractAction("General Schedule") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/History16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/History24.gif"));
      putValue(SHORT_DESCRIPTION, "Display the general schedule");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    public void actionPerformed(final ActionEvent ae) {
      final String schedule = getScheduleData().computeGeneralSchedule();
      JOptionPane.showMessageDialog(SchedulerUI.this, schedule, "General Schedule", JOptionPane.INFORMATION_MESSAGE);
    }
  };

  private final Action writeSchedulesAction = new AbstractAction("Write Detailed Schedules") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Export16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Export24.gif"));
      putValue(SHORT_DESCRIPTION, "Write out the detailed schedules as PDFs");
      // putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    public void actionPerformed(final ActionEvent ae) {
      try {
        getScheduleData().outputDetailedSchedules();
        JOptionPane.showMessageDialog(SchedulerUI.this, "Detailed schedules written to same directory as the schedule", "Information",
                                      JOptionPane.INFORMATION_MESSAGE);
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
      }
    }
  };

  private final Action openAction = new AbstractAction("Open") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open24.gif"));
      putValue(SHORT_DESCRIPTION, "Open a schedule file");
      putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    public void actionPerformed(final ActionEvent ae) {
      final String startingDirectory = PREFS.get(STARTING_DIRECTORY_PREF, null);

      final JFileChooser fileChooser = new JFileChooser();
      final FileFilter filter = new BasicFileFilter("Excel Spreadsheet", new String[] { "xls", "xslx" });
      fileChooser.setFileFilter(filter);
      if (null != startingDirectory) {
        fileChooser.setCurrentDirectory(new File(startingDirectory));
      }

      final int returnVal = fileChooser.showOpenDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File currentDirectory = fileChooser.getCurrentDirectory();
        PREFS.put(STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());

        final File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile.isFile()
            && selectedFile.canRead()) {
          try {
            final String sheetName = promptForSheetName(selectedFile);
            if (null == sheetName) {
              return;
            }
            final ParseSchedule schedule = new ParseSchedule(selectedFile, sheetName);
            setScheduleData(schedule);
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
          }

        } else {
          JOptionPane.showMessageDialog(SchedulerUI.this, new Formatter().format("%s is not a file or is not readable", selectedFile.getAbsolutePath()),
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
  private String promptForSheetName(final File selectedFile) throws InvalidFormatException, IOException {
    final List<String> sheetNames = ExcelCellReader.getAllSheetNames(selectedFile);
    if (sheetNames.size() == 1) {
      return sheetNames.get(0);
    } else {
      final String[] options = sheetNames.toArray(new String[sheetNames.size()]);
      final int choosenOption = JOptionPane.showOptionDialog(null, "Choose which sheet to work with", "Choose Sheet", JOptionPane.DEFAULT_OPTION,
                                                             JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
      if (JOptionPane.CLOSED_OPTION == choosenOption) {
        return null;
      }
      return options[choosenOption];
    }
  }

  private static final Preferences PREFS = Preferences.userNodeForPackage(ParseSchedule.class);

  private static final String STARTING_DIRECTORY_PREF = "startingDirectory";

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SE_BAD_FIELD", justification = "This calss isn't going to be serialized")
  private ParseSchedule scheduleData;

  /* package */ParseSchedule getScheduleData() {
    return scheduleData;
  }

  private SchedulerTableModel scheduleModel;

  SchedulerTableModel getScheduleModel() {
    return scheduleModel;
  }

  private ViolationTableModel violationsModel;

  /* package */ViolationTableModel getViolationsModel() {
    return violationsModel;
  }

  private void setScheduleData(final ParseSchedule sd) {
    scheduleTable.clearSelection();

    scheduleData = sd;
    scheduleModel = new SchedulerTableModel(scheduleData.getSchedule());
    scheduleTable.setModel(scheduleModel);

    checkSchedule();
  }

  /**
   * Verify the existing schedule and update the violations.
   */
  private void checkSchedule() {
    violationTable.clearSelection();

    violationsModel = new ViolationTableModel(scheduleData.verifySchedule());
    violationTable.setModel(violationsModel);
  }

  private final JTable scheduleTable;

  JTable getScheduleTable() {
    return scheduleTable;
  }

  private final JTable violationTable;

  JTable getViolationTable() {
    return violationTable;
  }

  private static final Logger LOGGER = Logger.getLogger(SchedulerUI.class);

  private TableCellRenderer schedTableRenderer = new DefaultTableCellRenderer() {
    private final Color errorColor = Color.RED;

    @Override
    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      setHorizontalAlignment(CENTER);

      final TeamScheduleInfo schedInfo = getScheduleModel().getSchedInfo(row);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Checking for violations against team: "
            + schedInfo.getTeamNumber() //
            + " column: " + column //
        );
      }

      boolean error = false;
      for (final ConstraintViolation violation : getViolationsModel().getViolations()) {
        if (violation.getTeam() == schedInfo.getTeamNumber()) {
          if ((SchedulerTableModel.TEAM_NUMBER_COLUMN == column || SchedulerTableModel.JUDGE_COLUMN == column)
              && null == violation.getPresentation() && null == violation.getTechnical() && null == violation.getPerformance()) {
            error = true;
          } else if (SchedulerTableModel.PRESENTATION_COLUMN == column
              && null != violation.getPresentation()) {
            error = true;
          } else if (SchedulerTableModel.TECHNICAL_COLUMN == column
              && null != violation.getTechnical()) {
            error = true;
          } else if (null != violation.getPerformance()) {
            // need to check round which round
            int round = 0;
            while (!violation.getPerformance().equals(schedInfo.getPerf(round))
                && round < ParseSchedule.NUMBER_OF_ROUNDS) {
              ++round;
              if (round >= ParseSchedule.NUMBER_OF_ROUNDS) {
                throw new RuntimeException("Internal error, walkd off the end of the round list");
              }
            }
            final int firstIdx = SchedulerTableModel.FIRST_PERFORMANCE_COLUMN
                + (round * SchedulerTableModel.NUM_COLUMNS_PER_ROUND);
            final int lastIdx = firstIdx
                + SchedulerTableModel.NUM_COLUMNS_PER_ROUND - 1;
            if (firstIdx <= column
                && column <= lastIdx) {
              error = true;
            }

            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Violation is in performance round: "
                  + round //
                  + " team: " + schedInfo.getTeamNumber() // 
                  + " firstIdx: " + firstIdx // 
                  + " lastIdx: " + lastIdx //
                  + " column: " + column //
                  + " error: " + error //
              );
            }
          }
        }
      }

      // set the background based on the error state
      setForeground(null);
      setBackground(null);
      if (error) {
        if (isSelected) {
          setForeground(errorColor);
        } else {
          setBackground(errorColor);
        }
      }

      // TODO would be nice to set foreground color when selected

      if (value instanceof Date) {
        final String strValue = ParseSchedule.OUTPUT_DATE_FORMAT.get().format((Date) value);
        return super.getTableCellRendererComponent(table, strValue, isSelected, hasFocus, row, column);
      } else {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
    }
  };
}
