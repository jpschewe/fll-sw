/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import javax.swing.table.AbstractTableModel;

/**
 * Table model that allows rows to be added and deleted.
 */
public abstract class EditableTableModel extends AbstractTableModel {

  /**
   * Add a new row. Implementations need to fire appropriate table change events.
   */
  public abstract void addNewRow();

  /**
   * Delete the specified row. Implementations need to fire appropriate table
   * change events.
   * 
   * @param row row index to delete
   */
  public abstract void deleteRow(int row);

}
