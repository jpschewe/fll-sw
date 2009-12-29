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

/**
 * Table model for scheduler information used in {@link SchedulerUI}.
 */
/*package*/ class SchedulerTableModel extends AbstractTableModel {

  private final List<TeamScheduleInfo> scheduleData;
  
  private static final Comparator<TeamScheduleInfo> TEAM_NUMBER_COMPARATOR = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
      if(one.getTeamNumber() < two.getTeamNumber()) {
        return -1;
      } else if(one.getTeamNumber() > two.getTeamNumber()) {
        return 1;
      } else {
        return 0;
      }
    }
  };
  
  public SchedulerTableModel(final List<TeamScheduleInfo> schedule) {
    scheduleData = new ArrayList<TeamScheduleInfo>(schedule);
    Collections.sort(scheduleData, TEAM_NUMBER_COMPARATOR);
  }
  
  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    return 3 + ParseSchedule.NUMBER_OF_ROUNDS * NUM_COLUMNS_PER_ROUND;
  }

  public static final int TEAM_NUMBER_COLUMN = 0;
  public static final int PRESENTATION_COLUMN = 1;
  public static final int TECHNICAL_COLUMN = 2;
  public static final int FIRST_PERFORMANCE_COLUMN = 3;
  public static final int NUM_COLUMNS_PER_ROUND = 3;
  
  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return scheduleData.size();
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final TeamScheduleInfo schedInfo = scheduleData.get(rowIndex);
    switch(columnIndex) {
    case TEAM_NUMBER_COLUMN:
      return schedInfo.getTeamNumber();
    case PRESENTATION_COLUMN:
      return schedInfo.getPresentation();
    case TECHNICAL_COLUMN:
      return schedInfo.getTechnical();
    default:
      final int round = (columnIndex-FIRST_PERFORMANCE_COLUMN) / NUM_COLUMNS_PER_ROUND;
      switch((columnIndex-FIRST_PERFORMANCE_COLUMN) % NUM_COLUMNS_PER_ROUND) {
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
    switch(columnIndex) {
    case TEAM_NUMBER_COLUMN:
      return Integer.class;
    case PRESENTATION_COLUMN:
      return Date.class;
    case TECHNICAL_COLUMN:
      return Date.class;
    default:
      switch(columnIndex % NUM_COLUMNS_PER_ROUND) {
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
    switch(columnIndex) {
    case TEAM_NUMBER_COLUMN:
      return "Team #";
    case PRESENTATION_COLUMN:
      return "Presentation";
    case TECHNICAL_COLUMN:
      return "Technical";
    default:
      final int round = (columnIndex-FIRST_PERFORMANCE_COLUMN) / NUM_COLUMNS_PER_ROUND;
      switch(columnIndex % NUM_COLUMNS_PER_ROUND) {
      case 0:
        return "Perf #" + (round + 1);
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
   * @param teamNumber
   * @return -1 if none found
   */
  public int getIndexOfTeam(final int teamNumber) {
    for(int idx=0; idx<scheduleData.size(); ++idx) {
      if(scheduleData.get(idx).getTeamNumber() == teamNumber) {
        return idx;
      }
    }
    return -1;     
  }
  
  public TeamScheduleInfo getSchedInfo(final int index) {
    return scheduleData.get(index);
  }
                              
}
