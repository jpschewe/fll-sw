/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Delete action for editing a table model.
 */
public final class EditableTableDeleteAction extends AbstractAction {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param tableModel the table model to modify
   */
  public EditableTableDeleteAction(final EditableTableModel tableModel) {
    this.tableModel = tableModel;
  }

  private final EditableTableModel tableModel;

  /**
   * Calls {@link EditableTableModel#deleteRow(int)} with the integer from the
   * event.
   * 
   * @see ActionEvent#getActionCommand()
   */
  @Override
  public void actionPerformed(final ActionEvent ae) {
    final String cmd = ae.getActionCommand();
    try {
      final int row = Integer.parseInt(cmd);
      tableModel.deleteRow(row);
    } catch (final NumberFormatException nfe) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Can't parse row number from action command: "
            + cmd, nfe);
      }
    }
  }
}