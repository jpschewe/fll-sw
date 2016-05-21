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
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.JTextField;

import fll.util.FormatterUtils;

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
    table.setDefaultEditor(String.class, new NameCellEditor());
    table.setDefaultEditor(Integer.class, new DurationCellEditor());

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

  private static final class NameCellEditor extends DefaultCellEditor {

    public NameCellEditor() {
      super(new JTextField()); // perhaps could use a formatted text field with
                               // a verifier for null
    }

    @Override
    public boolean stopCellEditing() {
      final String value = getCellEditorValue().toString();
      if (value.isEmpty()) {
        return false;
      } else {
        return true;
      }
    }
  }

  private static final class DurationCellEditor extends DefaultCellEditor {
    public DurationCellEditor() {
      super(FormatterUtils.createIntegerField(1, 1000));
    }

    @Override
    public Object getCellEditorValue() {
      final Object v = getComponent().getValue();
      return v;
    }

    @Override
    public boolean stopCellEditing() {
      if (!getComponent().getInputVerifier().shouldYieldFocus(getComponent())) {
        return false;
      } else {
        return super.stopCellEditing();
      }
    }

    @Override
    public JFormattedTextField getComponent() {
      return (JFormattedTextField) super.getComponent();
    }

  }

}
