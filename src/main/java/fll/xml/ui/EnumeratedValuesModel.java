/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.util.ArrayList;
import java.util.List;

import fll.scheduler.EditableTableModel;
import fll.util.FP;
import fll.xml.EnumeratedValue;
import fll.xml.Goal;

/**
 * Table model for working with the {@link EnumeratedValue} objects in a
 * {@link Goal}. This also specifies the initial value.
 */
class EnumeratedValuesModel extends EditableTableModel {

  private final Goal goal;

  private final List<EnumeratedValue> data = new ArrayList<>();

  private final List<Boolean> initialValue = new ArrayList<>();

  /**
   * @param goal the goal to modify
   */
  EnumeratedValuesModel(final Goal goal) {
    this.goal = goal;
    data.addAll(goal.getSortedValues());

    final double goalInitialValue = goal.getInitialValue();
    final double fuzz = 1E-6;
    for (int i = 0; i < data.size(); ++i) {
      if (FP.equals(goalInitialValue, data.get(i).getScore(), fuzz)) {
        initialValue.add(Boolean.TRUE);
      } else {
        initialValue.add(Boolean.FALSE);
      }
    }
  }

  @Override
  public int getRowCount() {
    return data.size();
  }

  @Override
  public int getColumnCount() {
    return 5;
  }

  @Override
  public String getColumnName(final int columnIndex) {
    switch (columnIndex) {
    case 0:
      return "Title";
    case 1:
      return "Value";
    case 2:
      return "Score";
    case 3:
      return "Initial Value";
    case 4:
      return "Delete";
    default:
      return "";
    }
  }

  @Override
  public Object getValueAt(final int rowIndex,
                           final int columnIndex) {
    final EnumeratedValue value = data.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return value.getTitle();
    case 1:
      return value.getValue();
    case 2:
      return value.getScore();
    case 3:
      return initialValue.get(rowIndex);
    case 4:
      return "Delete";
    default:
      return "";
    }
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    switch (columnIndex) {
    case 0:
      return String.class;
    case 1:
      return String.class;
    case 2:
      return Double.class;
    case 3:
      return Boolean.class;
    case 4:
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

  @Override
  public void setValueAt(final Object aValue,
                         final int rowIndex,
                         final int columnIndex) {
    final EnumeratedValue value = data.get(rowIndex);
    switch (columnIndex) {
    case 0:
      value.setTitle((String) aValue);
      break;
    case 1:
      value.setValue((String) aValue);
      break;
    case 2:
      value.setScore(((Number) aValue).doubleValue());
      break;
    case 3: {
      final Boolean boolValue = (Boolean) aValue;
      if (!boolValue.equals(initialValue.get(rowIndex))) {
        initialValue.set(rowIndex, boolValue);

        // if setting the value, unset all of the other rows
        if (Boolean.TRUE.equals(boolValue)) {
          for (int i = 0; i < data.size(); ++i) {
            if (i != rowIndex) {
              if (Boolean.TRUE.equals(initialValue.get(i))) {
                initialValue.set(i, Boolean.FALSE);
                fireTableCellUpdated(i, 4);
              }
            }
          }
        }
      }
    }
      break;
    default:
      // nothing, just return so we don't fire a cell updated event
      return;
    }
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  @Override
  public void addNewRow() {
    final EnumeratedValue newValue = new EnumeratedValue("title"
        + data.size(), "value"
            + data.size(), 0);
    goal.addValue(newValue);
    data.add(newValue);
    initialValue.add(false);

    fireTableRowsInserted(data.size()
        - 1, data.size()
            - 1);
  }

  @Override
  public void deleteRow(final int row) {
    final EnumeratedValue deleted = data.remove(row);
    initialValue.remove(row);
    goal.removeValue(deleted);
    fireTableRowsDeleted(row, row);
  }

  /**
   * Make sure that all values are committed to the underlying goal.
   * If no initial value is set, it will be NaN.
   */
  public void commitChanges() {
    final double value = getInitialValue();
    goal.setInitialValue(value);
  }

  /**
   * What is the currently selected initial value?
   * 
   * @return the value or NaN if there are no entries in the table
   */
  private double getInitialValue() {
    for (int i = 0; i < initialValue.size(); ++i) {
      if (initialValue.get(i)) {
        return data.get(i).getScore();
      }
    }
    return Double.NaN;
  }

}
