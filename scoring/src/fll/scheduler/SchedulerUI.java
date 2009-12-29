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
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.mtu.eggplant.util.BasicFileFilter;
import net.mtu.eggplant.util.gui.BasicWindowMonitor;
import net.mtu.eggplant.util.gui.GraphicsUtils;

import org.apache.log4j.Logger;

/**
 * UI for the scheduler.
 */
public class SchedulerUI extends JFrame {

  public static void main(final String[] args) {
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
    scheduledTable = new JTable();
    scheduledTable.setDefaultRenderer(Date.class, schedTableRenderer);

    final JScrollPane dataScroller = new JScrollPane(scheduledTable);
    cpane.add(dataScroller, BorderLayout.CENTER);

    violationTable = new JTable();
    final JScrollPane violationScroller = new JScrollPane(violationTable);
    cpane.add(violationScroller, BorderLayout.PAGE_END);
    violationTable.getSelectionModel().addListSelectionListener(violationSelectionListener);
  }

  private final ListSelectionListener violationSelectionListener = new ListSelectionListener() {
    public void valueChanged(final ListSelectionEvent e) {
      // single selection only uses the last index
      final ConstraintViolation selected = violationsModel.getViolation(e.getLastIndex());
      if (ConstraintViolation.NO_TEAM != selected.getTeam()) {
        final int teamIndex = scheduleModel.getIndexOfTeam(selected.getTeam());
        scheduledTable.getSelectionModel().setSelectionInterval(teamIndex, teamIndex);
      }
    }
  };

  private JToolBar createToolbar() {
    final JToolBar toolbar = new JToolBar("SchedulerUI Main Toolbar");

    toolbar.add(_openAction);
    toolbar.add(_exitAction);

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

    menu.add(_openAction);
    menu.add(_exitAction);

    return menu;
  }

  private final Action _exitAction = new AbstractAction("Exit") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Stop24.gif"));
      putValue(SHORT_DESCRIPTION, "Exit the application");
      putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    }

    public void actionPerformed(final ActionEvent ae) {
      System.exit(0);
    }
  };

  private final Action _openAction = new AbstractAction("Open") {
    {
      putValue(SMALL_ICON, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open16.gif"));
      putValue(LARGE_ICON_KEY, GraphicsUtils.getIcon("toolbarButtonGraphics/general/Open24.gif"));
      putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    public void actionPerformed(final ActionEvent ae) {
      final String startingDirectory = PREFS.get(STARTING_DIRECTORY_PREF, null);

      final JFileChooser fileChooser = new JFileChooser();
      final FileFilter filter = new BasicFileFilter("csv or directory", "csv");
      fileChooser.setFileFilter(filter);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fileChooser.setMultiSelectionEnabled(false);
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
          final ParseSchedule schedule = new ParseSchedule(selectedFile);
          try {
            schedule.parseFile();
          } catch (final ParseException e) {
            final Formatter errorFormatter = new Formatter();
            errorFormatter.format("Error reading file %s: %s", selectedFile.getAbsolutePath(), e.getMessage());
            LOGGER.error(errorFormatter, e);
            JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reading file", JOptionPane.ERROR_MESSAGE);
          } catch (final IOException e) {
            final Formatter errorFormatter = new Formatter();
            errorFormatter.format("Error reading file %s: %s", selectedFile.getAbsolutePath(), e.getMessage());
            LOGGER.error(errorFormatter, e);
            JOptionPane.showMessageDialog(SchedulerUI.this, errorFormatter, "Error reading file", JOptionPane.ERROR_MESSAGE);
          }

          setScheduleData(schedule);

        } else {
          JOptionPane.showMessageDialog(SchedulerUI.this, new Formatter().format("%s is not a file or is not readable", selectedFile.getAbsolutePath()),
                                        "Error reading file", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  };

  private static final Preferences PREFS = Preferences.userNodeForPackage(ParseSchedule.class);

  private static final String STARTING_DIRECTORY_PREF = "startingDirectory";

  private ParseSchedule scheduleData;

  private SchedulerTableModel scheduleModel;

  private ViolationTableModel violationsModel;

  private void setScheduleData(final ParseSchedule sd) {
    scheduleData = sd;
    scheduleModel = new SchedulerTableModel(scheduleData.getSchedule());
    scheduledTable.setModel(scheduleModel);

    violationsModel = new ViolationTableModel(scheduleData.verifySchedule());
    violationTable.setModel(violationsModel);

    pack();
  }

  private final JTable scheduledTable;

  private final JTable violationTable;

  private static final Logger LOGGER = Logger.getLogger(SchedulerUI.class);

  private TableCellRenderer schedTableRenderer = new DefaultTableCellRenderer() {
    private final Color ERROR_COLOR = Color.RED;

    @Override
    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      setHorizontalAlignment(CENTER);
      
      final TeamScheduleInfo schedInfo = scheduleModel.getSchedInfo(row);

      // set the background based on the error state
      setBackground(null);
      for (final ConstraintViolation violation : violationsModel.getViolations()) {
        if (violation.getTeam() == schedInfo.teamNumber) {
          if (SchedulerTableModel.TEAM_NUMBER_COLUMN == column
              && null == violation.getPresentation() && null == violation.getTechnical() && null == violation.getPerformance()) {
            setBackground(ERROR_COLOR);
          } else if (SchedulerTableModel.PRESENTATION_COLUMN == column
              && null != violation.getPresentation()) {
            setBackground(ERROR_COLOR);
          } else if (SchedulerTableModel.TECHNICAL_COLUMN == column
              && null != violation.getTechnical()) {
            setBackground(ERROR_COLOR);
          } else if (null != violation.getPerformance()) {
            // need to check round
            for (int idx = 0; idx < schedInfo.perf.length; ++idx) {
              final int firstIdx = SchedulerTableModel.FIRST_PERFORMANCE_COLUMN + (idx * SchedulerTableModel.NUM_COLUMNS_PER_ROUND);
              final int lastIdx = firstIdx + SchedulerTableModel.NUM_COLUMNS_PER_ROUND - 1;
              if (violation.getPerformance().equals(schedInfo.perf[idx])) {
                if(firstIdx <= column && column <= lastIdx) {
                  setBackground(ERROR_COLOR);
                }
              }
            }
          }
        }
      }

      if (value instanceof Date) {
        final String strValue = ParseSchedule.OUTPUT_DATE_FORMAT.get().format((Date) value);
        return super.getTableCellRendererComponent(table, strValue, isSelected, hasFocus, row, column);
      } else {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
    }
  };
}
