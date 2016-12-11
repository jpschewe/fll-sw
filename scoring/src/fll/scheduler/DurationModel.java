/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Model for displaying and editing the list of integer durations.
 */
public class DurationModel extends AbstractTableModel {

  private final List<Integer> durations = new ArrayList<>();

  public DurationModel() {
  }

  @Override
  public int getRowCount() {
    return durations.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public String getColumnName(final int column) {
    switch (column) {
    case 0:
      return "Duration";
    case 1:
      return "Delete";
    default:
      return null;
    }
  }

  @Override
  public Object getValueAt(final int rowIndex,
                           final int columnIndex) {
    switch (columnIndex) {
    case 0:
      final Integer duration = durations.get(rowIndex);
      return duration;
    case 1:
      return "Delete";
    default:
      return null;
    }
  }

  @Override
  public void setValueAt(final Object aValue,
                         final int rowIndex,
                         final int columnIndex) {
    switch (columnIndex) {
    case 0: {
      final Integer newDuration = (Integer) aValue;
      durations.remove(rowIndex);
      durations.add(rowIndex, newDuration);
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
      return Integer.class;
    case 1:
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
  public void setData(final List<Integer> data) {
    this.durations.clear();
    this.durations.addAll(data);
    fireTableDataChanged();
  }

  /**
   * Get the current data.
   * 
   * @return read-only list of the durations
   */
  public List<Integer> getData() {
    return Collections.unmodifiableList(this.durations);
  }

  /**
   * Add a new row to the table. The duration will be 1.
   */
  public void addNewRow() {
    durations.add(1);
    fireTableRowsInserted(durations.size()
        - 1, durations.size()
            - 1);
  }

  /**
   * Delete the specified row.
   * 
   * @param row
   */
  public void deleteRow(final int row) {
    durations.remove(row);
    fireTableRowsDeleted(row, row);
  }

}
