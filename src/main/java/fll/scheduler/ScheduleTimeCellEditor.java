/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.Component;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import javax.swing.AbstractCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Table cell editor for {@link LocalTime} schedule objects.
 */
/* package */ final class ScheduleTimeCellEditor extends AbstractCellEditor implements TableCellEditor {

  private final ScheduleTimeField delegate;

  private final transient ScheduleTimeField.TimeVerifier verifier;

  /**
   * Create a cell editor for schedule times.
   * 
   * @param allowNull if true, allow null values
   */
  /* package */ ScheduleTimeCellEditor(final boolean allowNull) {
    super();
    delegate = new ScheduleTimeField();
    verifier = new ScheduleTimeField.TimeVerifier(allowNull);
  }

  @Override
  public @Nullable Object getCellEditorValue() {
    final LocalTime value = delegate.getTime();
    return value;
  }

  @Override
  public boolean stopCellEditing() {
    boolean stop;

    // attempt to get the value, JPS 6/3/2016 - not sure if this is needed
    // this.getCellEditorValue();
    stop = verifier.verify(delegate);

    if (stop) {
      return super.stopCellEditing();
    } else {
      // dialog
      JOptionPane.showMessageDialog(delegate, "You must enter a valid time", "Error", JOptionPane.WARNING_MESSAGE);
      return false;
    }
  }

  @Override
  public Component getTableCellEditorComponent(final JTable table,
                                               final @Nullable Object value,
                                               final boolean isSelected,
                                               final int row,
                                               final int column) {
    LocalTime time;
    if (null == value) {
      time = LocalTime.now();
    } else if (value instanceof String) {
      final String s = (String) value;
      if (s.isEmpty()) {
        time = LocalTime.now();
      } else {
        try {
          time = LocalTime.parse(s);
        } catch (final DateTimeParseException e) {
          time = LocalTime.now();
        }
      }
    } else if (value instanceof LocalTime) {
      time = (LocalTime) value;
    } else {
      time = LocalTime.now();
    }
    delegate.setTime(time);
    return delegate;
  }

}