/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/*package*/class ViolationTableModel extends AbstractTableModel {

  private final List<ConstraintViolation> violations;

  private static final Comparator<ConstraintViolation> TEAM_NUMBER_COMPARATOR = new Comparator<ConstraintViolation>() {
    public int compare(final ConstraintViolation one, final ConstraintViolation two) {
      if(one.getTeam() < two.getTeam()) {
        return -1;
      } else if(one.getTeam() > two.getTeam()) {
        return 1;
      } else {
        return 0;
      }
    }
  };

  public ViolationTableModel(final List<ConstraintViolation> violations) {
    this.violations = new ArrayList<ConstraintViolation>(violations);
    Collections.sort(this.violations, TEAM_NUMBER_COMPARATOR);
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    return 1;
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return violations.size();
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final ConstraintViolation violation = violations.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return violation.getMessage();
    default:
      return null;
    }
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    switch (columnIndex) {
    case 0:
      return String.class;
    default:
      return Object.class;
    }
  }

  @Override
  public String getColumnName(final int columnIndex) {
    switch (columnIndex) {
    case 0:
      return "Message";
    default:
      return null;
    }
  }

  public ConstraintViolation getViolation(final int index) {
    return violations.get(index);
  }

  /**
   * 
   * @return Unmodifiable version of the constraint violations
   */
  public List<ConstraintViolation> getViolations() {
    return Collections.unmodifiableList(violations);
  }

}
