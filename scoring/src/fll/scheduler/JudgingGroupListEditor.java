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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Edit a list of judging groups.
 */
/* package */ class JudgingGroupListEditor extends JComponent {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JudgingGroupModel tableModel;

  private final JTable table;

  public JudgingGroupListEditor() {
    super();
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder("Judging Groups"));

    tableModel = new JudgingGroupModel();
    table = new JTable(this.tableModel);
    table.setDefaultEditor(String.class, new NameCellEditor());
    table.setDefaultEditor(Integer.class, new IntegerCellEditor(1, 100));

    new ButtonColumn(table, deleteAction, 2);

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

  public void setJudgingGroups(final Map<String, Integer> groups) {
    this.tableModel.setData(groups);
  }

  public Map<String, Integer> getJudgingGroups() {
    return this.tableModel.getData();
  }

}
