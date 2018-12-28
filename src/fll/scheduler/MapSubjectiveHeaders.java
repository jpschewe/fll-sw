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
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * Map subjective headers to subjective categories.
 * This dialog is modal.
 */
public class MapSubjectiveHeaders extends JDialog {

  private final ChallengeDescription description;

  private final TournamentSchedule schedule;

  private final Map<ScoreCategory, JComboBox<String>> comboBoxes = new HashMap<>();

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

    final JTextArea instructions = new JTextArea("Match the column names from the schedule data file with the subjective categories that they contain the schedule for. Also specify the number of minutes between judging sessions for each category.");
    instructions.setEditable(false);
    instructions.setWrapStyleWord(true);
    instructions.setLineWrap(true);
    getContentPane().add(instructions, BorderLayout.NORTH);

    final JPanel grid = new JPanel(new GridLayout(0, 2));
    getContentPane().add(grid, BorderLayout.CENTER);

    grid.add(new JLabel("Subjective Category"));
    grid.add(new JLabel("Data file column name"));

    final String[] scheduleColumns = schedule.getSubjectiveStations().toArray(new String[0]);

    for (final ScoreCategory category : description.getSubjectiveCategories()) {
      grid.add(new JLabel(category.getTitle()));

      final JComboBox<String> comboBox = new JComboBox<>(scheduleColumns);
      grid.add(comboBox);
      comboBoxes.put(category, comboBox);
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

    setMinimumSize(getPreferredSize());
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

}
