/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.db.CategoryColumnMapping;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Associate headers in the schedule file with the information needed to load
 * the schedule.
 */
class ChooseScheduleHeadersDialog extends JDialog {

  private final JComboBox<String> teamNumber;

  private final JComboBox<String> teamName;

  private final JComboBox<String> organization;

  private final JComboBox<String> awardGroup;

  private final JComboBox<String> judgingGroup;

  private final JComboBox<String> wave;

  private final List<JComboBox<String>> performanceRounds;

  private final List<JComboBox<String>> performanceRoundTables;

  private final Map<SubjectiveScoreCategory, JComboBox<String>> subjectiveCategories = new HashMap<>();

  private final Map<SubjectiveScoreCategory, JComboBox<String>> subjectiveCategories2 = new HashMap<>();

  /**
   * @param owner the owner for the dialog
   * @param headerNames the header names to choose from
   * @param numPerformanceRounds number of scheduled performance rounds
   * @param description the challenge description to get the subjective categories
   *          from
   */
  ChooseScheduleHeadersDialog(final JFrame owner,
                              final Collection<String> headerNames,
                              final int numPerformanceRounds,
                              final ChallengeDescription description) {
    super(owner, "Choose schedule columns", true);

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    final JLabel instructions = new JLabel("Select the columns in the schedule data file that match the needed information.");
    cpane.add(instructions, BorderLayout.NORTH);

    final JPanel panel = new JPanel(new GridLayout(0, 2));
    cpane.add(panel, BorderLayout.CENTER);

    final Vector<String> requiredHeaderNames = new Vector<>(headerNames);
    final Vector<String> notRequiredHeaderNames = new Vector<>();
    notRequiredHeaderNames.add("");
    notRequiredHeaderNames.addAll(headerNames);

    panel.add(new JLabel("Team Number"));
    teamNumber = new JComboBox<>(requiredHeaderNames);
    panel.add(teamNumber);
    teamNumber.setSelectedItem(TournamentSchedule.TEAM_NUMBER_HEADER);

    panel.add(new JLabel("Team Name"));
    teamName = new JComboBox<>(notRequiredHeaderNames);
    panel.add(teamName);
    teamName.setSelectedItem(TournamentSchedule.TEAM_NAME_HEADER);

    panel.add(new JLabel("Organization"));
    organization = new JComboBox<>(notRequiredHeaderNames);
    panel.add(organization);
    organization.setSelectedItem(TournamentSchedule.ORGANIZATION_HEADER);

    panel.add(new JLabel("Award Group"));
    awardGroup = new JComboBox<>(notRequiredHeaderNames);
    panel.add(awardGroup);
    awardGroup.setSelectedItem(TournamentSchedule.AWARD_GROUP_HEADER);

    panel.add(new JLabel("Judging Group"));
    judgingGroup = new JComboBox<>(notRequiredHeaderNames);
    panel.add(judgingGroup);
    judgingGroup.setSelectedItem(TournamentSchedule.JUDGE_GROUP_HEADER);

    panel.add(new JLabel("Wave"));
    wave = new JComboBox<>(notRequiredHeaderNames);
    panel.add(wave);
    wave.setSelectedItem(TournamentSchedule.WAVE_HEADER);

    performanceRounds = new ArrayList<>(numPerformanceRounds);
    performanceRoundTables = new ArrayList<>(numPerformanceRounds);
    for (int i = 0; i < numPerformanceRounds; ++i) {
      final int roundNumber = i
          + 1;
      panel.add(new JLabel(String.format("Performance %d", roundNumber)));
      final JComboBox<String> time = new JComboBox<>(requiredHeaderNames);
      panel.add(time);
      performanceRounds.add(time);
      time.setSelectedItem(String.format(TournamentSchedule.PERF_HEADER_FORMAT, roundNumber));

      panel.add(new JLabel(String.format("Performance %d table", roundNumber)));
      final JComboBox<String> table = new JComboBox<>(requiredHeaderNames);
      panel.add(table);
      performanceRoundTables.add(table);
      table.setSelectedItem(String.format(TournamentSchedule.TABLE_HEADER_FORMAT, roundNumber));
    }

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      panel.add(new JLabel(category.getTitle()));

      final JPanel selectPanel = new JPanel(new GridLayout(0, 2));
      panel.add(selectPanel);

      final JComboBox<String> columnSelect = new JComboBox<>(requiredHeaderNames);
      selectPanel.add(columnSelect);
      subjectiveCategories.put(category, columnSelect);

      final JComboBox<String> columnSelect2 = new JComboBox<>(notRequiredHeaderNames);
      selectPanel.add(columnSelect2);
      subjectiveCategories2.put(category, columnSelect2);
    }

    final Box buttonBox = Box.createHorizontalBox();
    cpane.add(buttonBox, BorderLayout.SOUTH);

    buttonBox.add(Box.createHorizontalGlue());

    final JButton okButton = new JButton("OK");
    buttonBox.add(okButton);

    okButton.addActionListener(e -> {
      ChooseScheduleHeadersDialog.this.setVisible(false);
    });

    pack();
  }

  /**
   * @param headerRowIndex index into the data file to get the header row
   * @param headerRow the header row from the schedule file
   * @return the column information for the schedule
   */
  public ColumnInformation createColumnInformation(final int headerRowIndex,
                                                   final @Nullable String[] headerRow) {
    final Collection<CategoryColumnMapping> subjectiveColumnMappings = new LinkedList<>();

    for (final Map.Entry<SubjectiveScoreCategory, JComboBox<String>> entry : subjectiveCategories.entrySet()) {
      final SubjectiveScoreCategory category = entry.getKey();
      final JComboBox<String> select = entry.getValue();
      final String scheduleColumn = select.getItemAt(select.getSelectedIndex());
      final CategoryColumnMapping mapping = new CategoryColumnMapping(category.getName(), scheduleColumn);
      subjectiveColumnMappings.add(mapping);
    }

    for (final Map.Entry<SubjectiveScoreCategory, JComboBox<String>> entry2 : subjectiveCategories2.entrySet()) {
      final SubjectiveScoreCategory category = entry2.getKey();
      final JComboBox<String> select2 = entry2.getValue();
      final String scheduleColumn2 = select2.getItemAt(select2.getSelectedIndex());
      if (!"".equals(scheduleColumn2)) {
        final CategoryColumnMapping mapping2 = new CategoryColumnMapping(category.getName(), scheduleColumn2);
        subjectiveColumnMappings.add(mapping2);
      }
    }

    final String[] perfColumn = performanceRounds.stream().map(c -> c.getItemAt(c.getSelectedIndex()))
                                                 .collect(Collectors.toList()).toArray(new String[0]);
    final String[] perfTableColumn = performanceRoundTables.stream().map(c -> c.getItemAt(c.getSelectedIndex()))
                                                           .collect(Collectors.toList()).toArray(new String[0]);

    return new ColumnInformation(headerRowIndex, headerRow, teamNumber.getItemAt(teamNumber.getSelectedIndex()),
                                 organization.getItemAt(organization.getSelectedIndex()),
                                 teamName.getItemAt(teamName.getSelectedIndex()),
                                 awardGroup.getItemAt(awardGroup.getSelectedIndex()),
                                 judgingGroup.getItemAt(judgingGroup.getSelectedIndex()),
                                 wave.getItemAt(wave.getSelectedIndex()), subjectiveColumnMappings, perfColumn,
                                 perfTableColumn);
  }

}
