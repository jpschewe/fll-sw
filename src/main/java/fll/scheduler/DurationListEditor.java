/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;

/**
 * Edit a list of integer durations.
 */
/* package */ class DurationListEditor extends JComponent {

  private final DurationModel tableModel;

  private final JTable table;

  DurationListEditor() {
    super();
    setLayout(new BorderLayout());

    tableModel = new DurationModel();
    table = new JTable(this.tableModel);
    table.setDefaultEditor(Integer.class, new IntegerCellEditor(1, 100));

    new ButtonColumn(table, new EditableTableDeleteAction(tableModel), 1);

    final JButton addButton = new JButton("Add Row");
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        tableModel.addNewRow();
      }
    });

    add(addButton, BorderLayout.SOUTH);
    add(table, BorderLayout.CENTER);
    add(table.getTableHeader(), BorderLayout.NORTH);
  }

  public void setDurations(final List<Integer> durations) {
    this.tableModel.setData(durations);
  }

  public List<Integer> getDurations() {
    return this.tableModel.getData();
  }

}
