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
 * Model for displaying and editing a list of {@link SubjectiveStation} objects.
 */
public class SubjectiveStationModel extends AbstractTableModel {

  private final List<SubjectiveStation> stations = new ArrayList<>();

  public SubjectiveStationModel() {
  }

  @Override
  public int getRowCount() {
    return stations.size();
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
    final SubjectiveStation station = stations.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return station.getName();
    case 1:
      return station.getDurationMinutes();
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
    final SubjectiveStation oldStation = stations.get(rowIndex);
    switch (columnIndex) {
    case 0: {
      final String newName = (String) aValue;
      final SubjectiveStation newStation = new SubjectiveStation(newName, oldStation.getDurationMinutes());
      stations.remove(rowIndex);
      stations.add(rowIndex, newStation);
      break;
    }
    case 1: {
      final Integer newDuration = (Integer) aValue;
      final SubjectiveStation newStation = new SubjectiveStation(oldStation.getName(), newDuration);
      stations.remove(rowIndex);
      stations.add(rowIndex, newStation);
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
   * Specify the stations for the model.
   */
  public void setData(final List<SubjectiveStation> stations) {
    this.stations.clear();
    this.stations.addAll(stations);
    fireTableDataChanged();
  }

  /**
   * Get the current data.
   * 
   * @return readonly list of the stations
   */
  public List<SubjectiveStation> getData() {
    return Collections.unmodifiableList(this.stations);
  }

  /**
   * Add a new row to the table. The name will be auto generated. The duration
   * will be {@link SchedParams#DEFAULT_SUBJECTIVE_MINUTES}.
   */
  public void addNewRow() {
    final SubjectiveStation newStation = new SubjectiveStation(GreedySolver.getSubjectiveColumnName(stations.size()),
                                                               SchedParams.DEFAULT_SUBJECTIVE_MINUTES);
    stations.add(newStation);
    fireTableRowsInserted(stations.size()
        - 1, stations.size()
            - 1);
  }
  
  /**
   * Delete the specified row.
   * 
   * @param row
   */
  public void deleteRow(final int row) {
    stations.remove(row);
    fireTableRowsDeleted(row, row);
  }

}
