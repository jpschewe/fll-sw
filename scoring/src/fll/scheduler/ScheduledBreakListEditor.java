/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Edit a list of {@link ScheduledBreak} objects.
 */
/* package */ class ScheduledBreakListEditor extends JComponent {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  private final ScheduledBreakModel tableModel;

  private final JTable table;

  public ScheduledBreakListEditor(final String title) {
    super();
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder(title));

    tableModel = new ScheduledBreakModel();
    table = new JTable(this.tableModel);
    table.setDefaultEditor(LocalTime.class, new ScheduleTimeCellEditor(false));
    table.setDefaultEditor(Integer.class, new IntegerCellEditor(1, 1000));

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

  public void setBreaks(final List<ScheduledBreak> breaks) {
    this.tableModel.setData(breaks);
  }

  /**
   * @return unmodifiable list of the breaks
   */
  public List<ScheduledBreak> getBreaks() {
    return this.tableModel.getData();
  }

}
