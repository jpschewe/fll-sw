/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Model for displaying and editing a list of scheduled breaks.
 */
public class ScheduledBreakModel extends AbstractTableModel {

  private final ArrayList<ScheduledBreak> breaks = new ArrayList<>();

  public ScheduledBreakModel() {
  }

  @Override
  public int getRowCount() {
    return breaks.size();
  }

  @Override
  public int getColumnCount() {
    return 3;
  }

  @Override
  public String getColumnName(final int column) {
    switch (column) {
    case 0:
      return "Time";
    case 1:
      return "Duration";
    case 2:
      return "Delete";
    default:
      return null;
    }
  }

  @Override
  public Object getValueAt(final int rowIndex,
                           final int columnIndex) {
    final ScheduledBreak aBreak = breaks.get(rowIndex);
    switch (columnIndex) {
    case 0:
      final LocalTime start = aBreak.getStart();
      return start;
    case 1:
      final Duration duration = aBreak.getDuration();
      return duration.toMinutes();
    case 2:
      return "Delete";
    default:
      return null;
    }
  }

  @Override
  public void setValueAt(final Object aValue,
                         final int rowIndex,
                         final int columnIndex) {
    final ScheduledBreak oldBreak = breaks.get(rowIndex);
    switch (columnIndex) {
    case 0: {
      final LocalTime newStart = (LocalTime) aValue;
      final ScheduledBreak newBreak = new ScheduledBreak(newStart, oldBreak.getDuration());
      breaks.remove(rowIndex);
      breaks.add(rowIndex, newBreak);
      break;
    }
    case 1: {
      final Integer newDurationMinutes = (Integer) aValue;
      final ScheduledBreak newBreak = new ScheduledBreak(oldBreak.getStart(), Duration.ofMinutes(newDurationMinutes));
      breaks.remove(rowIndex);
      breaks.add(rowIndex, newBreak);
    }
      break;
    default:
      // nothing, just return so we don't fire a cell updated event
      return;
    }
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    switch (columnIndex) {
    case 0:
      return LocalTime.class;
    case 1:
      return Integer.class;
    case 2:
      return String.class;
    default:
      return super.getColumnClass(columnIndex);
    }
  }

  @Override
  public boolean isCellEditable(final int rowIndex,
                                final int columnIndex) {
    // all data is editable
    return true;
  }

  /**
   * Specify the judging groups for the model.
   */
  public void setData(final List<ScheduledBreak> data) {
    this.breaks.clear();
    breaks.addAll(data);
    fireTableDataChanged();
  }

  /**
   * Get the current data.
   * 
   * @return an unmodifiable list of the breaks
   */
  public List<ScheduledBreak> getData() {
    return Collections.unmodifiableList(breaks);
  }

  /**
   * Add a new row to the table. The name will be auto generated. The time will
   * be 12:00 and the duration
   * will be 30 minutes.
   */
  public void addNewRow() {
    final ScheduledBreak newBreak = new ScheduledBreak(LocalTime.of(12, 0), Duration.ofMinutes(30));
    breaks.add(newBreak);
    fireTableRowsInserted(breaks.size()
        - 1, breaks.size()
            - 1);
  }

  /**
   * Delete the specified row.
   * 
   * @param row
   */
  public void deleteRow(final int row) {
    breaks.remove(row);
    fireTableRowsDeleted(row, row);
  }

}
