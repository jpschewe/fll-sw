/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.apache.log4j.Logger;

import fll.Utilities;
import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.xml.AbstractGoal;
import fll.xml.ComputedGoal;
import fll.xml.Goal;
import fll.xml.ScoreCategory;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;

/**
 * Editor for {@link ScoreCategory} objects.
 */
public abstract class ScoreCategoryEditor extends JPanel {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JFormattedTextField mWeight;

  private final List<AbstractGoalEditor> mGoalEditors = new LinkedList<>();

  private final Box mGoalEditorContainer;

  private ScoreCategory mCategory = null;

  private final MoveEventListener mGoalMoveListener;

  public ScoreCategoryEditor() {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    final Box weightContainer = Box.createHorizontalBox();
    add(weightContainer);

    weightContainer.add(new JLabel("Weight: "));

    mWeight = FormatterUtils.createDoubleField();
    weightContainer.add(mWeight);
    weightContainer.add(Box.createHorizontalGlue());

    mWeight.addPropertyChangeListener("value", e -> {
      if (null != mCategory) {
        final Number value = (Number) mWeight.getValue();
        if (null != value) {
          final double newWeight = value.doubleValue();
          mCategory.setWeight(newWeight);
        }
      }
    });

    final Box buttonBox = Box.createHorizontalBox();
    add(buttonBox);

    final JButton addGoal = new JButton("Add Goal");
    buttonBox.add(addGoal);
    addGoal.addActionListener(l -> addNewGoal());

    final JButton addComputedGoal = new JButton("Add Computed Goal");
    buttonBox.add(addComputedGoal);
    addComputedGoal.addActionListener(l -> addNewComputedGoal());

    buttonBox.add(Box.createHorizontalGlue());

    mGoalEditorContainer = Box.createVerticalBox();
    add(mGoalEditorContainer);

    mGoalMoveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        final int oldIndex = Utilities.getIndexOfComponent(mGoalEditorContainer, e.getComponent());
        if (oldIndex < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of move event in goal container");
            return;
          }
        }

        final int newIndex;
        if (e.getDirection() == MoveDirection.DOWN) {
          newIndex = oldIndex
              + 1;
        } else {
          newIndex = oldIndex
              - 1;
        }

        if (newIndex < 0
            || newIndex >= mGoalEditorContainer.getComponentCount()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Can't move component outside the container oldIndex: "
                + oldIndex
                + " newIndex: "
                + newIndex);
          }
          return;
        }

        // update editor list
        final AbstractGoalEditor editor = mGoalEditors.remove(oldIndex);
        mGoalEditors.add(newIndex, editor);

        // update the UI
        mGoalEditorContainer.add(e.getComponent(), newIndex);
        mGoalEditorContainer.validate();

        // update the order in the challenge description
        final AbstractGoal goal = mCategory.removeGoal(oldIndex);
        mCategory.addGoal(newIndex, goal);
      }
    };

  }

  /**
   * @param category the category to edit, may be null to clear the object
   */
  public void setCategory(final ScoreCategory category) {
    mCategory = category;

    mWeight.setValue(null);
    mGoalEditorContainer.removeAll();
    mGoalEditors.clear();

    if (null != mCategory) {
      mWeight.setValue(mCategory.getWeight());

      mCategory.getGoals().forEach(this::addGoal);
    }
  }

  /**
   * @return the category being edited, may be null if
   *         {@link #setCategory(ScoreCategory)} hasn't been called
   */
  public ScoreCategory getCategory() {
    return mCategory;
  }

  private void addNewGoal() {
    final String name = String.format("goal_%d", mCategory.getGoals().size());
    final String title = String.format("Goal %d", mCategory.getGoals().size());
    final Goal newGoal = new Goal(name);
    newGoal.setTitle(title);
    mCategory.addGoal(newGoal);
    addGoal(newGoal);
  }

  private void addNewComputedGoal() {
    final String name = String.format("goal_%d", mCategory.getGoals().size());
    final String title = String.format("Goal %d", mCategory.getGoals().size());
    final ComputedGoal newGoal = new ComputedGoal(name);
    newGoal.setTitle(title);
    mCategory.addGoal(newGoal);
    addGoal(newGoal);
  }

  private void addGoal(final AbstractGoal goal) {

    final AbstractGoalEditor editor;
    if (goal instanceof Goal) {
      editor = new GoalEditor((Goal) goal);
    } else if (goal instanceof ComputedGoal) {
      editor = new ComputedGoalEditor((ComputedGoal) goal);
    } else {
      throw new RuntimeException("Unexpected goal class: "
          + goal.getClass());
    }
    editor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    final MovableExpandablePanel panel = new MovableExpandablePanel(goal.getTitle(), editor, true);
    editor.addPropertyChangeListener("title", e -> {
      final String newTitle = (String) e.getNewValue();
      panel.setTitle(newTitle);
    });
    panel.addMoveEventListener(mGoalMoveListener);

    mGoalEditors.add(editor);

    mGoalEditorContainer.add(panel);
    mGoalEditorContainer.validate();
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      mWeight.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes, assuming bad value and ignoring", e);
    }

    mGoalEditors.forEach(e -> e.commitChanges());
  }

}
