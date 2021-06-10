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

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;

/**
 * Edit a list of {@link SubjectiveStation} objects.
 */
/* package */ class SubjectiveStationListEditor extends JComponent {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final SubjectiveStationModel tableModel;

  private final JTable table;

  SubjectiveStationListEditor() {
    super();
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder("Subjective Judging Stations"));

    tableModel = new SubjectiveStationModel();
    table = new JTable(this.tableModel);
    table.setDefaultEditor(String.class, new NameCellEditor());
    table.setDefaultEditor(Integer.class, new IntegerCellEditor(1, 1000));

    new ButtonColumn(table, new DeleteAction(tableModel), 2);

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

  private static final class DeleteAction extends AbstractAction {

    DeleteAction(final SubjectiveStationModel tableModel) {
      this.tableModel = tableModel;
    }

    private final SubjectiveStationModel tableModel;

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

  public void setStations(final List<SubjectiveStation> stations) {
    this.tableModel.setData(stations);
  }

  /**
   * @return unmodifiable list of the stations
   */
  public List<SubjectiveStation> getStations() {
    return this.tableModel.getData();
  }

}
