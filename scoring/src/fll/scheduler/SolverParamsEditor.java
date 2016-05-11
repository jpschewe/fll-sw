/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.MaskFormatter;

import fll.util.FLLInternalException;

/**
 * Editor for {@link SolverParams}.
 */
public class SolverParamsEditor extends JPanel {

  private final ScheduleTimeField startTimeEditor;

  private final JCheckBox alternateTables;

  private final JFormattedTextField performanceDuration;

  private final JFormattedTextField changeDuration;

  private final JFormattedTextField performanceChangeDuration;

  private final JFormattedTextField numPerformanceRounds;

  private final JCheckBox subjectiveFirst;

  private final JFormattedTextField perfAttemptOffsetMinutes;

  private final JFormattedTextField subjectiveAttemptOffsetMinutes;

  private final JFormattedTextField numTables;

  private final JFormattedTextField maxTime;

  public SolverParamsEditor() {
    super(new GridBagLayout());

    try {
      final MaskFormatter integerFormat = new MaskFormatter("#");

      GridBagConstraints gbc;

      startTimeEditor = new ScheduleTimeField();
      addRow(new JLabel("Start Time:"), startTimeEditor);
      
      alternateTables = new JCheckBox("Alternate tables");
      addRow(alternateTables);

      performanceDuration = new JFormattedTextField(integerFormat);
      performanceDuration.setToolTipText("The number of minutes between performance runs");
      addRow(new JLabel("Performance duration:"), performanceDuration);

      // FIXME mSubjectiveStations = new
      // ArrayList<SubjectiveStation>(subjectiveParams);

      changeDuration = new JFormattedTextField(integerFormat);
      changeDuration.setToolTipText("The number of minutes that a team has between any 2 activities");
      addRow(new JLabel("Change time duration:"), changeDuration);

      performanceChangeDuration = new JFormattedTextField(integerFormat);
      performanceChangeDuration.setToolTipText("The number of minutes that a team has between any 2 performance runs");
      addRow(new JLabel("Performance change time duration:"), performanceChangeDuration);

      // FIXME put in group counts

      numPerformanceRounds = new JFormattedTextField(integerFormat);
      addRow(new JLabel("Number of performance rounds:"), numPerformanceRounds);

      subjectiveFirst = new JCheckBox("Schedule subjective before performance");
      addRow(subjectiveFirst);

      perfAttemptOffsetMinutes = new JFormattedTextField(integerFormat);
      perfAttemptOffsetMinutes.setToolTipText("How many minutes later to try to find a new performance slot");
      addRow(new JLabel("Number of minutes between attempts "), perfAttemptOffsetMinutes);

      subjectiveAttemptOffsetMinutes = new JFormattedTextField(integerFormat);
      subjectiveAttemptOffsetMinutes.setToolTipText("How many minutes later to try to find a new subjective slot");
      addRow(new JLabel("Number of minutes between subjective attempts"), subjectiveAttemptOffsetMinutes);

      numTables = new JFormattedTextField(integerFormat);
      addRow(new JLabel("Number of performance tables"), numTables);

      // FIXME need format for this
      maxTime = new JFormattedTextField(/* new HoursMinutesFormat() */);
      addRow(new JLabel("Maximum length of the tournament"), maxTime);

      // end of form spacer
      gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridheight = GridBagConstraints.REMAINDER;
      gbc.weighty = 1.0;
      add(new JPanel(), gbc);
    } catch (final ParseException pe) {
      throw new FLLInternalException("Error building up SolverParamsEditor", pe);
    }
  }

  /**
   * Add a row of components and then add a spacer to the end of the row.
   * 
   * @param components the components to add
   */
  private void addRow(final JComponent... components) {
    GridBagConstraints gbc;

    for (final JComponent comp : components) {
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
    add(new JPanel(), gbc);
  }

  private SolverParams params;

  public void setParams(final SolverParams params) {
    this.params = params;

    startTimeEditor.setTime(params.getStartTime());

    alternateTables.setSelected(this.params.getAlternateTables());
    
  }

  public SolverParams getParams() {
    // FIXME make sure everything is valid
    // FIXME copy valuesl from editors into params and then return, or should
    // the values set immediately?
    return this.params;
  }

}
