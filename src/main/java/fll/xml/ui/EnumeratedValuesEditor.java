/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;

import fll.scheduler.ButtonColumn;
import fll.scheduler.EditableTableDeleteAction;
import fll.util.DatabaseNameCellEditor;
import fll.xml.Goal;

/**
 * Panel for editing the {@link Goal#getValues()} of a {@link Goal}.
 */
class EnumeratedValuesEditor extends JPanel {

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

    new ButtonColumn(table, new EditableTableDeleteAction(tableModel), 4);

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

  /**
   * Make sure that everything in the widget is written to the underlying
   * {@link Goal}.
   */
  public void commitChanges() {
    tableModel.commitChanges();
  }

}
