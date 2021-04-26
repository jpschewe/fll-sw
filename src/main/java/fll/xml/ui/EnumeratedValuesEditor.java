/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;

import fll.scheduler.ButtonColumn;
import fll.util.DatabaseNameCellEditor;

import fll.xml.Goal;

/**
 * Panel for editing the {@link Goal#getValues()} of a {@link Goal}.
 */
class EnumeratedValuesEditor extends JPanel {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JTable table;

  private final EnumeratedValuesModel tableModel;

  /**
   * @param goal the goal to modify
   */
  EnumeratedValuesEditor(final Goal goal) {
    super(new BorderLayout());

    this.tableModel = new EnumeratedValuesModel(goal);
    table = new JTable(this.tableModel);

    table.getColumnModel().getColumn(1).setCellEditor(new DatabaseNameCellEditor());

    new ButtonColumn(table, deleteAction, 4);

    final JButton addButton = new JButton("Add Row");
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        tableModel.addNewRow();
      }
    });

    add(table, BorderLayout.CENTER);
    add(table.getTableHeader(), BorderLayout.NORTH);

    final JComponent buttonBox = Box.createHorizontalBox();
    add(buttonBox, BorderLayout.SOUTH);
    buttonBox.add(addButton);
    buttonBox.add(Box.createHorizontalGlue());

  }

  private final Action deleteAction = new AbstractAction() {
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
  };

  /**
   * Make sure that everything in the widget is written to the underlying
   * {@link Goal}.
   */
  public void commitChanges() {
    tableModel.commitChanges();
  }

}
