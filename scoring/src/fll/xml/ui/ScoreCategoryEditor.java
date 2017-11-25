/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.apache.log4j.Logger;

import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.xml.AbstractGoal;
import fll.xml.ScoreCategory;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;

/**
 * Editor for {@link ScoreCategory} objects.
 */
public class ScoreCategoryEditor extends JPanel {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JFormattedTextField mWeight;

  private final List<AbstractGoalEditor> mGoalEditors = new LinkedList<>();

  private final Box mGoalEditorContainer;

  private ScoreCategory mCategory = null;

  private final MoveEventListener mGoalMoveListener;

  public ScoreCategoryEditor() {
    setLayout(new GridBagLayout());

    GridBagConstraints gbc;

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Weight: "), gbc);

    mWeight = FormatterUtils.createDoubleField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mWeight, gbc);

    mWeight.addPropertyChangeListener("value", e -> {
      if (null != mCategory) {
        final Number value = (Number) mWeight.getValue();
        if (null != value) {
          final double newWeight = value.doubleValue();
          mCategory.setWeight(newWeight);
        }
      }
    });

    mGoalEditorContainer = Box.createVerticalBox();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    add(mGoalEditorContainer, gbc);

    mGoalMoveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        // find index of e.component in subjective
        int oldIndex = -1;
        for (int i = 0; oldIndex < 0
            && i < mGoalEditorContainer.getComponentCount(); ++i) {
          final Component c = mGoalEditorContainer.getComponent(i);
          if (c == e.getComponent()) {
            oldIndex = i;
          }
        }
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

      for (final AbstractGoal goal : mCategory.getGoals()) {

        final AbstractGoalEditor editor = new AbstractGoalEditor(goal);
        editor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        final MovableExpandablePanel panel = new MovableExpandablePanel(goal.getTitle(), editor, true);
        editor.addPropertyChangeListener("title", e -> {
          final String newTitle = (String) e.getNewValue();
          panel.setTitle(newTitle);
        });
        panel.addMoveEventListener(mGoalMoveListener);

        mGoalEditorContainer.add(panel);
        mGoalEditors.add(editor);
      }
    }
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
