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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Associate headers in the schedule file with the information needed to load the schedule.
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

  private final Map<SubjectiveScoreCategory, JFormattedTextField> subjectiveCategoryDurations = new HashMap<>();

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

      final Box box = Box.createHorizontalBox();
      panel.add(box);
      final JComboBox<String> columnSelect = new JComboBox<>(requiredHeaderNames);
      box.add(columnSelect);
      subjectiveCategories.put(category, columnSelect);

      final JFormattedTextField duration = new JFormattedTextField(Integer.valueOf(SchedParams.DEFAULT_SUBJECTIVE_MINUTES));
      duration.setColumns(4);
      box.add(duration);
      subjectiveCategoryDurations.put(category, duration);
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
   * @param headerRow the header row from the schedule file
   * @return the column information for the schedule
   */
  public ColumnInformation createColumnInformation(final @Nullable String[] headerRow) {
    // FIXME
    return null;
  }

}
