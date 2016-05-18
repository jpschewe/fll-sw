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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;

/**
 * Edit a list of {@link SubjectiveStation} objects.
 */
/* package */ class SubjectiveStationListEditor extends JComponent {

  private final SubjectiveStationModel tableModel;
  private final JTable table;

  public SubjectiveStationListEditor() {
    super();
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder("Subjective Stations"));

    tableModel = new SubjectiveStationModel();
    table = new JTable(this.tableModel);

    //FIXME make sure names are not empty
    //FIXME make sure durations are greater than 0
    
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
