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
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Edit a list of {@link SubjectiveStation} objects.
 */
/* package */ class SubjectiveStationListEditor extends JComponent {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final SubjectiveStationModel tableModel;

  private final JTable table;

  public SubjectiveStationListEditor() {
    super();
    setLayout(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder("Subjective Stations"));

    tableModel = new SubjectiveStationModel();
    table = new JTable(this.tableModel);
    table.setDefaultEditor(String.class, new NameCellEditor());
    table.setDefaultEditor(Integer.class, new IntegerCellEditor(1, 1000));

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

  private static final class IntegerCellEditor extends DefaultCellEditor {

    private final int minValue;

    private final int maxValue;

    /**
     * Create a cell editor for integers.
     * 
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     */
    public IntegerCellEditor(final int min,
                             final int max) {
      super(new JTextField());
      minValue = min;
      maxValue = max;
    }

    @Override
    public JTextField getComponent() {
      return (JTextField) super.getComponent();
    }

    @Override
    public Object getCellEditorValue() {
      final String str = super.getCellEditorValue().toString();
      final Integer value = Integer.parseInt(str);
      return value;
    }

    @Override
    public boolean stopCellEditing() {
      final String str = getComponent().getText();
      boolean stop;
      try {
        final Integer value = Integer.parseInt(str);
        if (minValue <= value
            && value <= maxValue) {
          stop = true;
        } else {
          stop = false;
        }
      } catch (final NumberFormatException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Could not parse as integer", e);
        }
        stop = false;
      }

      if (stop) {
        return super.stopCellEditing();
      } else {
        // dialog
        JOptionPane.showMessageDialog(null,
                                      String.format("You must enter an integer between %d and %d", minValue, maxValue),
                                      "Error", JOptionPane.WARNING_MESSAGE);
        return false;
      }
    }

  }

}
