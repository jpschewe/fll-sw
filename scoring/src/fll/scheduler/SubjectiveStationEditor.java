/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

import fll.util.FormatterUtils;

/**
 * Editor for {@link SubjectiveStation} objects.
 */
/* package */ class SubjectiveStationEditor extends JComponent {

  private final JTextField nameEditor;

  private final JFormattedTextField durationEditor;

  public SubjectiveStationEditor() {
    super();
    setLayout(new FlowLayout());

    nameEditor = new JTextField();
    add(nameEditor);

    durationEditor = FormatterUtils.createIntegerField(1, 1000);
    durationEditor.setToolTipText("The duration of the judging for this subjective category in minutes");
    add(durationEditor);

  }

  /**
   * Specify the station to edit.
   */
  public void setSubjectiveStation(final SubjectiveStation station) {
    nameEditor.setText(station.getName());
    durationEditor.setValue(station.getDurationMinutes());
  }

  /**
   * Get a {@link SubjectiveStation} object based on the current values of the
   * widgets.
   */
  public SubjectiveStation getSubjectiveStation() {
    final String name = nameEditor.getText();
    final int duration = ((Integer) durationEditor.getValue()).intValue();
    return new SubjectiveStation(name, duration);
  }

}
