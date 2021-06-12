/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;

/**
 * Edit a list of judging groups.
 */
/* package */ class JudgingGroupListEditor extends JComponent {

  private final JudgingGroupModel tableModel;

  private final JTable table;

  JudgingGroupListEditor() {
    super();
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder("Judging Groups"));

    tableModel = new JudgingGroupModel();
    table = new JTable(this.tableModel);
    table.setDefaultEditor(String.class, new NameCellEditor());
    table.setDefaultEditor(Integer.class, new IntegerCellEditor(1, 100));

    new ButtonColumn(table, new EditableTableDeleteAction(tableModel), 2);

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

  public void setJudgingGroups(final Map<String, Integer> groups) {
    this.tableModel.setData(groups);
  }

  public Map<String, Integer> getJudgingGroups() {
    return this.tableModel.getData();
  }

}
