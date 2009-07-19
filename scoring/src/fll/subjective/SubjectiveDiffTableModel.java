/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * 
 */
public class SubjectiveDiffTableModel extends AbstractTableModel {

  private final List<SubjectiveScoreDifference> diffs;

  public SubjectiveDiffTableModel(final Collection<SubjectiveScoreDifference> diffs) {
    this.diffs = new ArrayList<SubjectiveScoreDifference>(diffs);
  }

  @Override
  public String getColumnName(final int column) {
    switch (column) {
    case 0:
      return "Category";
    case 1:
      return "Subcategory";
    case 2:
      return "TeamNumber";
    case 3:
      return "Judge";
    case 4:
      return "Value";
    case 5:
      return "Compare Value";
    default:
      return null;
    }
  }

  @Override
  public Class<?> getColumnClass(final int column) {
    switch (column) {
    case 0:
      return String.class;
    case 1:
      return String.class;
    case 2:
      return Integer.class;
    case 3:
      return String.class;
    case 4:
      return Object.class;
    case 5:
      return Object.class;
    default:
      return null;
    }
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    return 6;
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return diffs.size();
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final SubjectiveScoreDifference diff = diffs.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return diff.getCategory();
    case 1:
      return diff.getSubcategory();
    case 2:
      return diff.getTeamNumber();
    case 3:
      return diff.getJudge();
    case 4:
      return diff.getMasterValue();
    case 5:
      return diff.getCompareValue();
    default:
      return null;
    }
  }

}
