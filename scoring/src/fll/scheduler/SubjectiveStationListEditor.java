/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.poi.ss.formula.eval.NotImplementedException;

/**
 * Edit a list of {@link SubjectiveStation} objects.
 */
/* package */ class SubjectiveStationListEditor extends JComponent {

  private List<SubjectiveStationEditor> stationEditors = new LinkedList<>();

  public SubjectiveStationListEditor() {
    super();
    setLayout(new GridBagLayout());

    final JButton addButton = new JButton("+");
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        addNewRow();
      }
    });
    addRow(new JLabel("Name"), new JLabel("Duration"), addButton);

  }

  /**
   * Add another row of
   */
  private void addNewRow() {
    //FIXME need to arrange into nicer table, perhaps use JTable?
    final SubjectiveStationEditor editor = new SubjectiveStationEditor();
    final SubjectiveStation station = new SubjectiveStation("Subjective Station "
        + stationEditors.size(), SchedParams.DEFAULT_SUBJECTIVE_MINUTES);
    editor.setSubjectiveStation(station);

    final JButton deleteButton = new JButton("-");

    final JPanel row = new JPanel(new FlowLayout());
    row.add(editor);
    row.add(deleteButton);
    addRow(row);
    revalidate();

    deleteButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        remove(row);
        stationEditors.remove(editor);
        revalidate();
      }
    });

    stationEditors.add(editor);
  }

  private void addRow(final JComponent... components) {
    GridBagConstraints gbc;

    // for (final JComponent comp : components) {
    for (int i = 0; i < components.length
        - 1; ++i) {
      final JComponent comp = components[i];
      gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.WEST;
      gbc.weighty = 0;
      add(comp, gbc);
    }

    // end of line spacer
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    // add(new JPanel(), gbc);
    add(components[components.length
        - 1], gbc);
  }

  public void setStations(final List<SubjectiveStation> stations) {
    // FIXME
    throw new NotImplementedException("setStations is not yet implemented");
  }

  public List<SubjectiveStation> getStations() {
    // FIXME
    throw new NotImplementedException("getStations is not yet implemented");
  }
}
