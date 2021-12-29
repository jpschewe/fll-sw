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

  private final List<JComboBox<String>> practiceRounds;

  private final List<JComboBox<String>> practiceRoundTables;

  private final List<JComboBox<String>> performanceRounds;

  private final List<JComboBox<String>> performanceRoundTables;

  private final Map<SubjectiveScoreCategory, JComboBox<String>> subjectiveCategories = new HashMap<>();

  /**
   * @param owner the owner for the dialog
   * @param headerNames the header names to choose from
   * @param numPracticeRounds number of practice rounds
   * @param numRegularMatchRounds number of regular match play rounds
   * @param description the challenge description to get the subjective categories
   *          from
   */
  ChooseScheduleHeadersDialog(final JFrame owner,
                              final Collection<String> headerNames,
                              final int numPracticeRounds,
                              final int numRegularMatchRounds,
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

    panel.add(new JLabel("Team Name"));
    teamName = new JComboBox<>(notRequiredHeaderNames);
    panel.add(teamName);

    panel.add(new JLabel("Organization"));
    organization = new JComboBox<>(notRequiredHeaderNames);
    panel.add(organization);

    panel.add(new JLabel("Award Group"));
    awardGroup = new JComboBox<>(notRequiredHeaderNames);
    panel.add(awardGroup);

    panel.add(new JLabel("Judging Group"));
    judgingGroup = new JComboBox<>(notRequiredHeaderNames);
    panel.add(judgingGroup);

    practiceRounds = new ArrayList<>(numPracticeRounds);
    practiceRoundTables = new ArrayList<>(numPracticeRounds);
    for (int i = 0; i < numPracticeRounds; ++i) {
      panel.add(new JLabel(String.format("Practice %d", (i
          + 1))));
      final JComboBox<String> time = new JComboBox<>(requiredHeaderNames);
      panel.add(time);
      practiceRounds.add(time);

      panel.add(new JLabel(String.format("Practice %d table", (i
          + 1))));
      final JComboBox<String> table = new JComboBox<>(requiredHeaderNames);
      panel.add(table);
      practiceRoundTables.add(table);
    }

    performanceRounds = new ArrayList<>(numRegularMatchRounds);
    performanceRoundTables = new ArrayList<>(numRegularMatchRounds);
    for (int i = 0; i < numRegularMatchRounds; ++i) {
      panel.add(new JLabel(String.format("Performance %d", (i
          + 1))));
      final JComboBox<String> time = new JComboBox<>(requiredHeaderNames);
      panel.add(time);
      performanceRounds.add(time);

      panel.add(new JLabel(String.format("Performance %d table", (i
          + 1))));
      final JComboBox<String> table = new JComboBox<>(requiredHeaderNames);
      panel.add(table);
      performanceRoundTables.add(table);
    }

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      panel.add(new JLabel(category.getTitle()));

      final JComboBox<String> columnSelect = new JComboBox<>(requiredHeaderNames);
      panel.add(columnSelect);
      subjectiveCategories.put(category, columnSelect);
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
    final Collection<CategoryColumnMapping> subjectiveColumnMappings = subjectiveCategories.entrySet().stream() //
                                                                                           .map(e -> new CategoryColumnMapping(e.getKey()
                                                                                                                                .getName(),
                                                                                                                               e.getValue()
                                                                                                                                .getItemAt(e.getValue()
                                                                                                                                            .getSelectedIndex()))) //
                                                                                           .collect(Collectors.toList());

    final String[] perfColumn = performanceRounds.stream().map(c -> c.getItemAt(c.getSelectedIndex()))
                                                 .collect(Collectors.toList()).toArray(new String[0]);
    final String[] perfTableColumn = performanceRoundTables.stream().map(c -> c.getItemAt(c.getSelectedIndex()))
                                                           .collect(Collectors.toList()).toArray(new String[0]);

    final String[] practiceColumn = practiceRounds.stream().map(c -> c.getItemAt(c.getSelectedIndex()))
                                                  .collect(Collectors.toList()).toArray(new String[0]);
    final String[] practiceTableColumn = practiceRoundTables.stream().map(c -> c.getItemAt(c.getSelectedIndex()))
                                                            .collect(Collectors.toList()).toArray(new String[0]);

    return new ColumnInformation(headerRowIndex, headerRow, teamNumber.getItemAt(teamNumber.getSelectedIndex()),
                                 organization.getItemAt(organization.getSelectedIndex()),
                                 teamName.getItemAt(teamName.getSelectedIndex()),
                                 awardGroup.getItemAt(awardGroup.getSelectedIndex()),
                                 judgingGroup.getItemAt(judgingGroup.getSelectedIndex()), subjectiveColumnMappings,
                                 perfColumn, perfTableColumn, practiceColumn, practiceTableColumn);
  }

}
