/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import fll.scheduler.TeamScheduleInfo.SubjectiveTime;

/**
 * Table model for scheduler information used in {@link SchedulerUI}.
 */
/* package */class SchedulerTableModel extends AbstractTableModel {

  private final List<TeamScheduleInfo> scheduleData;

  private final TournamentSchedule schedule;

  private final List<String> subjectiveColumns;

  private static final Comparator<TeamScheduleInfo> TEAM_NUMBER_COMPARATOR = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {
      if (one.getTeamNumber() < two.getTeamNumber()) {
        return -1;
      } else if (one.getTeamNumber() > two.getTeamNumber()) {
        return 1;
      } else {
        return 0;
      }
    }
  };

  public SchedulerTableModel(final TournamentSchedule schedule) {
    this.schedule = schedule;
    this.scheduleData = new ArrayList<TeamScheduleInfo>(schedule.getSchedule());
    this.subjectiveColumns = new ArrayList<String>(schedule.getSubjectiveStations());
    Collections.sort(scheduleData, TEAM_NUMBER_COMPARATOR);
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    return (JUDGE_COLUMN + 1)
        + subjectiveColumns.size() + (schedule.getNumberOfRounds() * NUM_COLUMNS_PER_ROUND);
  }

  public static final int TEAM_NUMBER_COLUMN = 0;

  public static final int JUDGE_COLUMN = TEAM_NUMBER_COLUMN + 1;

  /**
   * Number of columns per performance found.
   */
  public static final int NUM_COLUMNS_PER_ROUND = 3;

  public int getColumnForSubjective(final String name) {
    return subjectiveColumns.indexOf(name);
  }

  public int getFirstPerformanceColumn() {
    return JUDGE_COLUMN
        + 1 + subjectiveColumns.size();
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return scheduleData.size();
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(final int rowIndex,
                           final int columnIndex) {
    final TeamScheduleInfo schedInfo = scheduleData.get(rowIndex);
    if (columnIndex == TEAM_NUMBER_COLUMN) {
      return schedInfo.getTeamNumber();
    } else if (columnIndex == JUDGE_COLUMN) {
      return schedInfo.getJudgingStation();
    } else if (columnIndex < getFirstPerformanceColumn()) {
      // columns are named by the subjective categories
      final SubjectiveTime subj = schedInfo.getSubjectiveTimeByName(getColumnName(columnIndex));
      if (null == subj) {
        return null;
      } else {
        return subj.getTime();
      }
    } else {
      final int perfColIdx = columnIndex
          - getFirstPerformanceColumn();
      final int round = perfColIdx
          / NUM_COLUMNS_PER_ROUND;
      switch (perfColIdx
          % NUM_COLUMNS_PER_ROUND) {
      case 0:
      case 3:
        return schedInfo.getPerf(round);
      case 1:
        return schedInfo.getPerfTableColor(round);
      case 2:
        return schedInfo.getPerfTableSide(round);
      default:
        return null;
      }
    }
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    if (columnIndex == TEAM_NUMBER_COLUMN) {
      return Integer.class;
    } else if (columnIndex == JUDGE_COLUMN) {
      return String.class;
    } else if (columnIndex < getFirstPerformanceColumn()) {
      return Date.class;
    } else {
      final int perfColIdx = columnIndex
          - getFirstPerformanceColumn();
      switch (perfColIdx
          % NUM_COLUMNS_PER_ROUND) {
      case 0:
        return Date.class;
      case 1:
        return String.class;
      case 2:
        return Integer.class;
      default:
        return Object.class;
      }
    }
  }

  @Override
  public String getColumnName(final int columnIndex) {
    if (columnIndex == TEAM_NUMBER_COLUMN) {
      return "Team #";
    } else if (columnIndex == JUDGE_COLUMN) {
      return "Judge";
    } else if (columnIndex < getFirstPerformanceColumn()) {
      return subjectiveColumns.get(columnIndex
          - JUDGE_COLUMN - 1);
    } else {
      final int perfColIdx = columnIndex
          - getFirstPerformanceColumn();
      final int round = perfColIdx
          / NUM_COLUMNS_PER_ROUND;
      switch (perfColIdx
          % NUM_COLUMNS_PER_ROUND) {
      case 0:
        return "Perf #"
            + (round + 1);
      case 1:
        return "Table";
      case 2:
        return "Side";
      default:
        return null;
      }
    }
  }

  /**
   * Find the index of the specified team in this model.
   * 
   * @param teamNumber
   * @return -1 if none found
   */
  public int getIndexOfTeam(final int teamNumber) {
    for (int idx = 0; idx < scheduleData.size(); ++idx) {
      if (scheduleData.get(idx).getTeamNumber() == teamNumber) {
        return idx;
      }
    }
    return -1;
  }

  /**
   * Get team information for row index.
   */
  public TeamScheduleInfo getSchedInfo(final int index) {
    return scheduleData.get(index);
  }

}
