/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * Map subjective headers to subjective categories. Also map file suffixes.
 * This dialog is modal.
 */
/* package */ class MapSubjectiveHeaders extends JDialog {

  private final ChallengeDescription description;

  private final TournamentSchedule schedule;

  private final Map<ScoreCategory, JComboBox<String>> comboBoxes = new HashMap<>();

  private final Map<ScoreCategory, JTextField> filenameSuffixes = new HashMap<>();

  private boolean canceled = true;

  /**
   * @return Was cancel pressed on the dialog?
   */
  public boolean isCanceled() {
    return canceled;
  }

  public MapSubjectiveHeaders(final Frame owner,
                              final ChallengeDescription description,
                              final TournamentSchedule schedule) {
    super(owner, true);
    this.description = description;
    this.schedule = schedule;

    initComponents();
  }

  public MapSubjectiveHeaders(final Dialog owner,
                              final ChallengeDescription description,
                              final TournamentSchedule schedule) {
    super(owner, true);
    this.description = description;
    this.schedule = schedule;

    initComponents();
  }

  private void initComponents() {
    getContentPane().setLayout(new BorderLayout());

    final JTextArea instructions = new JTextArea("Match the column names from the schedule data file with the subjective categories that they contain the schedule for. If you want a suffix on the filename, then fill in that column.");
    instructions.setEditable(false);
    instructions.setWrapStyleWord(true);
    instructions.setLineWrap(true);
    getContentPane().add(instructions, BorderLayout.NORTH);

    final JPanel grid = new JPanel(new GridLayout(0, 3));
    getContentPane().add(grid, BorderLayout.CENTER);

    grid.add(new JLabel("Subjective Category"));
    grid.add(new JLabel("Data file column name"));
    grid.add(new JLabel("Filename suffix for category"));

    final String[] scheduleColumns = schedule.getSubjectiveStations().toArray(new String[0]);

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      grid.add(new JLabel(category.getTitle()));

      final JComboBox<String> comboBox = new JComboBox<>(scheduleColumns);
      grid.add(comboBox);
      comboBoxes.put(category, comboBox);

      final String defaultText = DEFAULT_CATEGORY_SUFFIXES.get(category.getName());
      final JTextField filenameSuffix = new JTextField(defaultText);
      grid.add(filenameSuffix);
      filenameSuffixes.put(category, filenameSuffix);
    }

    final Box buttonBox = Box.createHorizontalBox();
    getContentPane().add(buttonBox, BorderLayout.SOUTH);

    final JButton ok = new JButton("OK");
    buttonBox.add(ok);
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        setVisible(false);
        canceled = false;
      }
    });

    final JButton cancel = new JButton("Cancel");
    buttonBox.add(cancel);
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        setVisible(false);
        canceled = true;
      }
    });

    // setMinimumSize(getPreferredSize());
    pack();
  }

  /**
   * Find schedule column for category.
   * 
   * @param category what to find
   * @return null if not found
   */
  public String getSubjectiveHeaderForCategory(final ScoreCategory category) {
    final JComboBox<String> combo = comboBoxes.get(category);
    if (null != combo) {
      return (String) combo.getSelectedItem();
    } else {
      return null;
    }
  }

  /**
   * Get filename suffix for category.
   * 
   * @param category what to find
   * @return null if not found or no suffix
   */
  public String getFilenameSuffixForCategory(final ScoreCategory category) {
    final JTextField widget = filenameSuffixes.get(category);
    if (null != widget) {
      final String text = widget.getText();
      if (null == text
          || "".equals(text.trim())) {
        return null;
      } else {
        return text.trim();
      }
    } else {
      return null;
    }
  }

  /**
   * Map category names to file suffixes.
   */
  private static final Map<String, String> DEFAULT_CATEGORY_SUFFIXES;
  static {
    final Map<String, String> defaultCategorySuffixes = new HashMap<>();
    defaultCategorySuffixes.put("project", "LtBeigePaper");
    defaultCategorySuffixes.put("core_values", "LtPinkPaper");
    defaultCategorySuffixes.put("robot_design", "LtBluePaper");
    defaultCategorySuffixes.put("robot_programming", "LtPurplePaper");
    DEFAULT_CATEGORY_SUFFIXES = Collections.unmodifiableMap(defaultCategorySuffixes);
  }

}
