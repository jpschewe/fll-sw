/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

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

  private final JFormattedTextField numPerformanceRounds;

  private final JCheckBox subjectiveFirst;

  private final JFormattedTextField perfAttemptOffsetMinutes;

  private final JFormattedTextField subjectiveAttemptOffsetMinutes;

  private final JFormattedTextField numTables;

  private final ScheduleDurationField maxTime;

  private final JudgingGroupListEditor judgingGroups;

  private final ScheduledBreakListEditor subjectiveBreaks;

  private final ScheduledBreakListEditor performanceBreaks;

  public SolverParamsEditor() {
    super(new GridBagLayout());

    GridBagConstraints gbc;

    startTimeEditor = new ScheduleTimeField();
    startTimeEditor.setInputVerifier(new ScheduleTimeField.TimeVerifier());
    addRow(new JLabel("Start Time:"), startTimeEditor);

    alternateTables = new JCheckBox("Alternate tables");
    addRow(alternateTables);

    performanceDuration = FormatterUtils.createIntegerField(1, 1000);
    performanceDuration.setToolTipText("The number of minutes between performance runs");
    addRow(new JLabel("Performance duration:"), performanceDuration);

    subjectiveStations = new SubjectiveStationListEditor();
    addRow(subjectiveStations);

    changeDuration = FormatterUtils.createIntegerField(0, 1000);
    changeDuration.setToolTipText("The number of minutes that a team has between any 2 activities");
    addRow(new JLabel("Change time duration:"), changeDuration);

    performanceChangeDuration = FormatterUtils.createIntegerField(0, 1000);
    performanceChangeDuration.setToolTipText("The number of minutes that a team has between any 2 performance runs");
    addRow(new JLabel("Performance change time duration:"), performanceChangeDuration);

    judgingGroups = new JudgingGroupListEditor();
    addRow(judgingGroups);

    numPerformanceRounds = FormatterUtils.createIntegerField(0, 10);
    addRow(new JLabel("Number of performance rounds:"), numPerformanceRounds);

    subjectiveFirst = new JCheckBox("Schedule subjective before performance");
    addRow(subjectiveFirst);

    perfAttemptOffsetMinutes = FormatterUtils.createIntegerField(1, 1000);
    perfAttemptOffsetMinutes.setToolTipText("How many minutes later to try to find a new performance slot");
    addRow(new JLabel("Number of minutes between attempts "), perfAttemptOffsetMinutes);

    subjectiveAttemptOffsetMinutes = FormatterUtils.createIntegerField(1, 1000);
    subjectiveAttemptOffsetMinutes.setToolTipText("How many minutes later to try to find a new subjective slot");
    addRow(new JLabel("Number of minutes between subjective attempts"), subjectiveAttemptOffsetMinutes);

    numTables = FormatterUtils.createIntegerField(1, 1000);
    addRow(new JLabel("Number of performance tables"), numTables);

    maxTime = new ScheduleDurationField();
    maxTime.setToolTipText("Maximum duration of the tournament hours:minutes");
    addRow(new JLabel("Maximum length of the tournament"), maxTime);

    subjectiveBreaks = new ScheduledBreakListEditor("Subjective Breaks");
    addRow(subjectiveBreaks);

    performanceBreaks = new ScheduledBreakListEditor("Performance Breaks");
    addRow(performanceBreaks);

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

    final Map<String, Integer> judgingGroupMap = new HashMap<>();
    for (int groupNum = 0; groupNum < params.getNumGroups(); ++groupNum) {
      final int groupCount = params.getNumTeamsInGroup(groupNum);
      final String groupName = String.format("group-%d", groupNum);
      judgingGroupMap.put(groupName, groupCount);
    }
    judgingGroups.setJudgingGroups(judgingGroupMap);

    numPerformanceRounds.setValue(params.getNumPerformanceRounds());
    subjectiveFirst.setSelected(params.getSubjectiveFirst());
    perfAttemptOffsetMinutes.setValue(params.getPerformanceAttemptOffsetMinutes());
    subjectiveAttemptOffsetMinutes.setValue(params.getSubjectiveAttemptOffsetMinutes());
    numTables.setValue(params.getNumTables());

    maxTime.setDuration(params.getMaxDuration());

    subjectiveBreaks.setBreaks(params.getSubjectiveBreaks());
    performanceBreaks.setBreaks(params.getPerformanceBreaks());

  }

  public SolverParams getParams() {
    // FIXME make sure everything is valid
    // FIXME copy valuesl from editors into params and then return, or should
    // the values set immediately?
    return this.params;
  }

}
