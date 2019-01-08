/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Model for displaying and editing a list of performance rounds.
 */
/*package*/ class PerformanceRoundsModel extends AbstractTableModel {
  private final ArrayList<LocalTime> times = new ArrayList<>();

  public PerformanceRoundsModel() {
  }

  @Override
  public int getRowCount() {
    return times.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public String getColumnName(final int column) {
    switch (column) {
    case 0:
      return "Round";
    case 1:
      return "Earliest Start";
    default:
      return null;
    }
  }

  @Override
  public Object getValueAt(final int rowIndex,
                           final int columnIndex) {
    switch (columnIndex) {
    case 0:
      return rowIndex + 1;
    case 1:
      return times.get(rowIndex);
    default:
      return null;
    }
  }

  @Override
  public void setValueAt(final Object aValue,
                         final int rowIndex,
                         final int columnIndex) {
    switch (columnIndex) {
    case 1: {
      final LocalTime newTime = (LocalTime) aValue;
      times.set(rowIndex, newTime);
      break;
    }
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
      return Integer.class;
    case 1:
      return LocalTime.class;
    default:
      return super.getColumnClass(columnIndex);
    }
  }

  @Override
  public boolean isCellEditable(final int rowIndex,
                                final int columnIndex) {
    // the first column is not editable
    return columnIndex >= 1;
  }

  /**
   * Specify the performance limits for the model.
   * 
   * @param data the index specifies the round, null means no limit
   */
  public void setData(final List<LocalTime> data) {
    this.times.clear();
    this.times.addAll(data);
    fireTableDataChanged();
  }

  /**
   * Get the current data.
   * 
   * @return an unmodifiable list of the limits, null means the round can start
   *         as early as it wants
   */
  public List<LocalTime> getData() {
    return Collections.unmodifiableList(times);
  }

  /**
   * Add a new row to the table. The round number is automatically incremented.
   */
  public void addRound() {
    times.add(null);
    fireTableRowsInserted(times.size()
        - 1, times.size()
            - 1);
  }

  /**
   * Remove the last round.
   */
  public void removeLastRound() {
    final int rowToRemove = times.size()
        - 1;
    times.remove(rowToRemove);
    fireTableRowsDeleted(rowToRemove, rowToRemove);
  }

}
