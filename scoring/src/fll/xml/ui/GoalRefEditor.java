/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.util.Collection;
import java.util.LinkedList;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import fll.util.ChooseOptionDialog;
import fll.xml.AbstractGoal;
import fll.xml.GoalRef;
import fll.xml.GoalScoreType;

/**
 * Editor for {@link GoalRef} objects.
 */
class GoalRefEditor extends JPanel {

  private final GoalRef goalRef;

  /**
   * @param ref the object to edit
   */
  public GoalRefEditor(@Nonnull final GoalRef ref) {
    this.goalRef = ref;

    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

    final JButton goal = new JButton(ref.getGoal().getTitle());
    add(goal);
    goal.setToolTipText("Click to change the referenced goal");
    goal.addActionListener(l -> {
      final Collection<AbstractGoal> goals = goalRef.getGoalScope().getAllGoals();

      final ChooseOptionDialog<AbstractGoal> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                               new LinkedList<>(goals),
                                                                               new AbstractGoalCellRenderer());
      dialog.setVisible(true);
      final AbstractGoal selected = dialog.getSelectedValue();
      if (null != selected) {
        ref.setGoalName(selected.getName());
        goal.setText(selected.getTitle());
      }
    });

    final JComboBox<GoalScoreType> scoreType = new JComboBox<>(GoalScoreType.values());
    add(scoreType);
    scoreType.addActionListener(l -> {
      final GoalScoreType selected = scoreType.getItemAt(scoreType.getSelectedIndex());
      goalRef.setScoreType(selected);
    });

  }
}
