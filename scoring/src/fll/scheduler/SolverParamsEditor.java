/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import fll.util.FormatterUtils;

/**
 * Editor for {@link SolverParams}.
 */
public class SolverParamsEditor extends JPanel {

  private final ScheduleTimeField startTimeEditor;

  private final JCheckBox alternateTables;

  private final JFormattedTextField performanceDuration;

  private final SubjectiveStationListEditor subjectiveStations;

  private final JFormattedTextField changeDuration;

  private final JFormattedTextField performanceChangeDuration;

  private final JCheckBox subjectiveFirst;

  private final DurationListEditor perfAttemptOffsetMinutes;

  private final JFormattedTextField subjectiveAttemptOffsetMinutes;

  private final JFormattedTextField numTables;

  private final ScheduleDurationField maxTime;

  private final JudgingGroupListEditor judgingGroups;

  private final PerformanceRoundsEditor performanceRounds;

  private final ScheduledBreakListEditor subjectiveBreaks;

  private final ScheduledBreakListEditor performanceBreaks;

  public SolverParamsEditor() {
    super(new GridBagLayout());

    GridBagConstraints gbc;

    startTimeEditor = new ScheduleTimeField();
    startTimeEditor.setInputVerifier(new ScheduleTimeField.TimeVerifier(false));
    addRow(new JLabel("Start Time:"), startTimeEditor);

    maxTime = new ScheduleDurationField();
    maxTime.setToolTipText("Maximum duration of the tournament hours:minutes");
    addRow(new JLabel("Maximum length of the tournament"), maxTime);

    changeDuration = FormatterUtils.createIntegerField(0, 1000);
    changeDuration.setToolTipText("The number of minutes that a team has between any 2 activities");
    addRow(new JLabel("Change time duration:"), changeDuration);

    performanceChangeDuration = FormatterUtils.createIntegerField(0, 1000);
    performanceChangeDuration.setToolTipText("The number of minutes that a team has between any 2 performance runs");
    addRow(new JLabel("Performance change time duration:"), performanceChangeDuration);

    addRow(new JSeparator());
    // ---------------

    judgingGroups = new JudgingGroupListEditor();
    addRow(judgingGroups);

    subjectiveStations = new SubjectiveStationListEditor();
    addRow(subjectiveStations);

    subjectiveBreaks = new ScheduledBreakListEditor("Subjective Breaks");
    addRow(subjectiveBreaks);

    subjectiveAttemptOffsetMinutes = FormatterUtils.createIntegerField(1, 1000);
    subjectiveAttemptOffsetMinutes.setToolTipText("How many minutes later to try to find a new subjective time slot when no team can be scheduled at a time.");
    addRow(new JLabel("Number of minutes between subjective attempts"), subjectiveAttemptOffsetMinutes);

    addRow(new JSeparator());
    // ----------------

    numTables = FormatterUtils.createIntegerField(1, 1000);
    addRow(new JLabel("Number of performance tables"), numTables);

    alternateTables = new JCheckBox("Alternate tables");
    addRow(alternateTables);

    performanceDuration = FormatterUtils.createIntegerField(1, 1000);
    performanceDuration.setToolTipText("The amount of time that the team is expected to be at the table");
    addRow(new JLabel("Performance duration:"), performanceDuration);

    performanceRounds = new PerformanceRoundsEditor();
    addRow(performanceRounds);

    performanceBreaks = new ScheduledBreakListEditor("Performance Breaks");
    addRow(performanceBreaks);

    perfAttemptOffsetMinutes = new DurationListEditor();
    perfAttemptOffsetMinutes.setBorder(BorderFactory.createTitledBorder("Number of minutes between performance attempts"));
    perfAttemptOffsetMinutes.setToolTipText("If a performance round cannot be scheduled at a time, how many minutes later should the next time to try be. This is a list specifying the pattern. In most cases this list should only contain one element. However some tournaments may want to specify a pattern such as 7 and then 8 so that there are 2 timeslots available every 15 minutes.");
    addRow(perfAttemptOffsetMinutes);

    addRow(new JSeparator());
    // -------------

    subjectiveFirst = new JCheckBox("Schedule subjective before performance");
    addRow(subjectiveFirst);

    // end of form spacer
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weighty = 1.0;
    add(new JPanel(), gbc);
  }

  /**
   * Add a row of components and then add a spacer to the end of the row.
   * 
   * @param components the components to add
   */
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

  private SolverParams params;

  public void setParams(final SolverParams params) {
    this.params = params;

    startTimeEditor.setTime(params.getStartTime());
    alternateTables.setSelected(this.params.getAlternateTables());
    performanceDuration.setValue(params.getPerformanceMinutes());
    changeDuration.setValue(params.getChangetimeMinutes());
    performanceChangeDuration.setValue(params.getPerformanceChangetimeMinutes());

    subjectiveStations.setStations(params.getSubjectiveStations());

    judgingGroups.setJudgingGroups(params.getJudgingGroups());

    performanceRounds.setRounds(params.getPerformanceRoundEarliestStartTimes());

    subjectiveFirst.setSelected(params.getSubjectiveFirst());
    perfAttemptOffsetMinutes.setDurations(params.getPerformanceAttemptOffsetMinutes());
    subjectiveAttemptOffsetMinutes.setValue(params.getSubjectiveAttemptOffsetMinutes());
    numTables.setValue(params.getNumTables());

    maxTime.setDuration(params.getMaxDuration());

    subjectiveBreaks.setBreaks(params.getSubjectiveBreaks());
    performanceBreaks.setBreaks(params.getPerformanceBreaks());

  }

  /**
   * The values from the editors are pushed into the parameters object and that
   * object is returned.
   * The caller should call {@link SchedParams#isValid()} called on it and
   * display the errors to the user.
   * 
   * @return a non-null parameters object
   */
  public SolverParams getParams() {

    params.setStartTime(startTimeEditor.getTime());
    params.setAlternateTables(alternateTables.isSelected());
    params.setPerformanceMinutes((Integer) performanceDuration.getValue());
    params.setChangetimeMinutes((Integer) changeDuration.getValue());
    params.setPerformanceChangetimeMinutes((Integer) performanceChangeDuration.getValue());

    params.setSubjectiveStations(subjectiveStations.getStations());

    final Map<String, Integer> judgingGroupMap = judgingGroups.getJudgingGroups();
    params.setJudgingGroups(judgingGroupMap);

    params.setPerformanceRoundEarliestStartTimes(performanceRounds.getLimits());

    params.setSubjectiveFirst(subjectiveFirst.isSelected());
    params.setPerformanceAttemptOffsetMinutes(perfAttemptOffsetMinutes.getDurations());
    params.setSubjectiveAttemptOffsetMinutes((Integer) subjectiveAttemptOffsetMinutes.getValue());
    params.setNumTables((Integer) numTables.getValue());

    params.setMaxDuration(maxTime.getDuration());

    params.setSubjectiveBreaks(subjectiveBreaks.getBreaks());
    params.setPerformanceBreaks(performanceBreaks.getBreaks());

    return this.params;
  }

}
