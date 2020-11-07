/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.ChooseOptionDialog;
import fll.xml.AbstractGoal;
import fll.xml.GoalRef;
import fll.xml.GoalScoreType;

/**
 * Editor for {@link GoalRef} objects.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
/* package */ class GoalRefEditor extends JPanel {

  private final GoalRef goalRef;

  private final JButton editor;

  /**
   * @param ref the object to edit
   */
  /* package */ GoalRefEditor(@Nonnull final GoalRef ref) {
    this.goalRef = ref;

    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

    editor = new JButton(ref.getGoal().getTitle());
    add(editor);
    ref.getGoal().addPropertyChangeListener(nameListener);
    editor.setToolTipText("Click to change the referenced goal");
    editor.addActionListener(l -> {
      final Collection<AbstractGoal> goals = goalRef.getGoalScope().getAllGoals();

      final ChooseOptionDialog<AbstractGoal> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                               new LinkedList<>(goals),
                                                                               new AbstractGoalCellRenderer());
      dialog.setVisible(true);
      final AbstractGoal selected = dialog.getSelectedValue();
      if (null != selected) {
        ref.getGoal().removePropertyChangeListener(nameListener);

        ref.setGoalName(selected.getName());
        editor.setText(selected.getTitle());

        ref.getGoal().addPropertyChangeListener(nameListener);
      }
    });

    final JComboBox<GoalScoreType> scoreType = new JComboBox<>(GoalScoreType.values());
    add(scoreType);
    scoreType.addActionListener(l -> {
      final GoalScoreType selected = scoreType.getItemAt(scoreType.getSelectedIndex());
      goalRef.setScoreType(selected);
    });
    scoreType.setSelectedItem(goalRef.getScoreType());
  }

  private final NameChangeListener nameListener = new NameChangeListener();

  private class NameChangeListener implements PropertyChangeListener {

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if ("name".equals(evt.getPropertyName())) {
        final String newName = (String) evt.getNewValue();
        goalRef.setGoalName(newName);
      } else if ("title".equals(evt.getPropertyName())) {
        final String newTitle = (String) evt.getNewValue();
        editor.setText(newTitle);
      }
    }
  }

}
