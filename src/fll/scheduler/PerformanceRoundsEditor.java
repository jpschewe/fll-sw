/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;

/**
 * Edit a list of performance start round limits.
 */
/* package */ class PerformanceRoundsEditor extends JComponent {

  private final PerformanceRoundsModel tableModel;

  private final JTable table;

  public PerformanceRoundsEditor() {
    super();
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder("Performance Rounds"));
    this.setToolTipText("Define the number of rounds and the earliest start time for each round.");

    tableModel = new PerformanceRoundsModel();
    table = new JTable(this.tableModel);
    table.setDefaultEditor(LocalTime.class, new ScheduleTimeCellEditor(true));
    table.setDefaultEditor(Integer.class, new IntegerCellEditor(1, 1000));

    final JPanel buttonPanel = new JPanel(new FlowLayout());
    final JButton addButton = new JButton("Add Round");
    buttonPanel.add(addButton);
    addButton.addActionListener(e -> {
      tableModel.addRound();
    });

    final JButton removeButton = new JButton("Remove Last Round");
    buttonPanel.add(removeButton);
    removeButton.addActionListener(e -> {
      tableModel.removeLastRound();
    });

    add(buttonPanel, BorderLayout.SOUTH);
    add(table, BorderLayout.CENTER);
    add(table.getTableHeader(), BorderLayout.NORTH);
  }

  /**
   * The rounds for the tournament.
   * 
   * @param v a list of earliest start times for each round, the indeex
   *          is the round, each value may be null meaning no limit
   */
  public void setRounds(final List<LocalTime> v) {
    this.tableModel.setData(v);
  }

  /**
   * @return unmodifiable map of the limits.
   */
  public List<LocalTime> getLimits() {
    return this.tableModel.getData();
  }

}
