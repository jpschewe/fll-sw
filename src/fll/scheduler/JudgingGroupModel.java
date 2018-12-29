/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

/**
 * Model for displaying and editing the list of judging groups.
 */
public class JudgingGroupModel extends AbstractTableModel {

  private final List<String> judges = new ArrayList<>();

  private final List<Integer> counts = new ArrayList<>();

  public JudgingGroupModel() {
  }

  @Override
  public int getRowCount() {
    return judges.size();
  }

  @Override
  public int getColumnCount() {
    return 3;
  }

  @Override
  public String getColumnName(final int column) {
    switch (column) {
    case 0:
      return "Name";
    case 1:
      return "# of teams";
    case 2:
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
      final String name = judges.get(rowIndex);
      return name;
    case 1:
      final Integer count = counts.get(rowIndex);
      return count;
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
    switch (columnIndex) {
    case 0: {
      final String newName = (String) aValue;
      judges.remove(rowIndex);
      judges.add(rowIndex, newName);
      break;
    }
    case 1: {
      final Integer newCount = (Integer) aValue;
      counts.remove(rowIndex);
      counts.add(rowIndex, newCount);
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
      return String.class;
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
  public void setData(final Map<String, Integer> data) {
    this.judges.clear();
    this.counts.clear();
    for (final Map.Entry<String, Integer> entry : data.entrySet()) {
      this.judges.add(entry.getKey());
      this.counts.add(entry.getValue());
    }
    fireTableDataChanged();
  }

  /**
   * Get the current data.
   * 
   * @return new map of judge to number of teams
   */
  public Map<String, Integer> getData() {
    final Map<String, Integer> map = new HashMap<>();
    for (int i = 0; i < judges.size(); ++i) {
      final String judge = judges.get(i);
      final int numTeams = counts.get(i);
      map.put(judge, numTeams);
    }
    return map;
  }

  /**
   * Add a new row to the table. The name will be auto generated. The count will be 1.
   */
  public void addNewRow() {
    judges.add(String.format("group-%d", judges.size()));
    counts.add(1);
    fireTableRowsInserted(judges.size()
        - 1, judges.size()
            - 1);
  }

  /**
   * Delete the specified row.
   * 
   * @param row
   */
  public void deleteRow(final int row) {
    judges.remove(row);
    counts.remove(row);
    fireTableRowsDeleted(row, row);
  }

}
