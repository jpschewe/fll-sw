/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.border.EtchedBorder;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import fll.Utilities;
import fll.xml.AbstractGoal;
import fll.xml.ComputedGoal;
import fll.xml.Goal;
import fll.xml.GoalGroup;
import fll.xml.GoalScope;
import fll.xml.ui.MovableExpandablePanel.DeleteEvent;
import fll.xml.ui.MovableExpandablePanel.DeleteEventListener;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;

/**
 * Editor for {@link GoalGroup} objects.
 */
/* package */ class GoalGroupEditor extends GoalElementEditor {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final List<AbstractGoalEditor> goalEditors = new LinkedList<>();

  private final Box goalEditorContainer;

  private final GoalGroup goalGroup;

  private final GoalScope goalScope;

  private final MoveDeleteListener moveDeleteListener;

  /**
   * @param goalGroup the object to edit
   * @param goalScope used to lookup goals for {@link ComputedGoalEditor}
   */
  /* package */ GoalGroupEditor(final GoalGroup goalGroup,
                                final GoalScope goalScope) {
    super(goalGroup);

    this.goalGroup = goalGroup;
    this.goalScope = goalScope;

    final Box container = Box.createVerticalBox();
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    add(container, gbc);

    final Box buttonBox = Box.createHorizontalBox();
    container.add(buttonBox);

    final JButton addGoal = new JButton("Add Goal");
    buttonBox.add(addGoal);

    final JButton addComputedGoal = new JButton("Add Computed Goal");
    buttonBox.add(addComputedGoal);

    new AddListener(addGoal, addComputedGoal);

    buttonBox.add(Box.createHorizontalGlue());

    goalEditorContainer = Box.createVerticalBox();
    container.add(goalEditorContainer);

    moveDeleteListener = new MoveDeleteListener();

    this.goalGroup.getGoals().forEach(this::addGoal);
  }

  private void addNewGoal() {
    final String name = String.format("goal_%d", goalGroup.getGoals().size());
    final String title = String.format("Goal %d", goalGroup.getGoals().size());
    final Goal newGoal = new Goal(name);
    newGoal.setTitle(title);
    goalGroup.addGoal(newGoal);
    addGoal(newGoal);
  }

  private void addNewComputedGoal() {
    final String name = String.format("goal_%d", goalGroup.getGoals().size());
    final String title = String.format("Goal %d", goalGroup.getGoals().size());
    final ComputedGoal newGoal = new ComputedGoal(name, goalScope);
    newGoal.setTitle(title);
    goalGroup.addGoal(newGoal);
    addGoal(newGoal);
  }

  @RequiresNonNull({ "moveDeleteListener", "goalEditorContainer", "goalScope" })
  private void addGoal(@UnknownInitialization(GoalElementEditor.class) GoalGroupEditor this,
                       final AbstractGoal goal) {

    final AbstractGoalEditor editor;
    if (goal instanceof Goal) {
      editor = new GoalEditor((Goal) goal);
    } else if (goal instanceof ComputedGoal) {
      editor = new ComputedGoalEditor((ComputedGoal) goal, goalScope);
    } else {
      throw new RuntimeException("Unexpected goal class: "
          + goal.getClass());
    }
    editor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    final MovableExpandablePanel panel = new MovableExpandablePanel(goal.getTitle(), editor, true, true);
    editor.addPropertyChangeListener("title", e -> {
      final String newTitle = (String) e.getNewValue();
      panel.setTitle(newTitle);
    });
    panel.addMoveEventListener(moveDeleteListener);
    panel.addDeleteEventListener(moveDeleteListener);

    goalEditors.add(editor);

    GuiUtils.addToContainer(goalEditorContainer, panel);
  }

  /**
   * Force any pending edits to complete.
   */
  @Override
  public void commitChanges() {
    super.commitChanges();
    goalEditors.forEach(e -> e.commitChanges());
  }

  @Override
  protected void gatherValidityMessages(final Collection<String> messages) {
    super.gatherValidityMessages(messages);

    final Set<String> goalNames = new HashSet<>();
    for (final AbstractGoalEditor editor : goalEditors) {
      final String name = editor.getGoal().getName();

      final boolean editorValid = editor.checkValidity(messages);
      if (!editorValid) {
        messages.add(String.format("Goal \"%s\" has invalid elements", name));
      }

      final boolean newName = goalNames.add(name);
      if (!newName) {
        messages.add(String.format("Goal names must be unique in a category, the name \"%s\" is used more than once",
                                   name));
      }
    }
  }

  private final class MoveDeleteListener implements MoveEventListener, DeleteEventListener {
    private MoveDeleteListener() {
    }

    @Override
    public void requestedMove(final MoveEvent e) {
      final int oldIndex = Utilities.getIndexOfComponent(goalEditorContainer, e.getComponent());
      if (oldIndex < 0) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unable to find source of move event in goal container");
        }
        return;
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
          || newIndex >= goalEditorContainer.getComponentCount()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Can't move component outside the container oldIndex: "
              + oldIndex
              + " newIndex: "
              + newIndex);
        }
        return;
      }

      // update editor list
      final AbstractGoalEditor editor = goalEditors.remove(oldIndex);
      /* goalGroupEditor. */goalEditors.add(newIndex, editor);

      // update the UI
      goalEditorContainer.add(e.getComponent(), newIndex);
      goalEditorContainer.validate();

      // update the order in the challenge description
      final AbstractGoal goal = goalGroup.removeGoal(oldIndex);
      goalGroup.addGoal(newIndex, goal);

    }

    @Override
    public void requestDelete(final DeleteEvent e) {
      final int confirm = JOptionPane.showConfirmDialog(GoalGroupEditor.this,
                                                        "Are you sure that you want to delete the goal?",
                                                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
      if (confirm != JOptionPane.YES_OPTION) {
        return;
      }

      final int index = Utilities.getIndexOfComponent(goalEditorContainer, e.getComponent());
      if (index < 0) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unable to find source of delete event in goal container");
        }
        return;
      }

      // update editor list
      goalEditors.remove(index);

      // update the challenge description
      goalGroup.removeGoal(index);

      // update the UI
      GuiUtils.removeFromContainer(goalEditorContainer, index);
    }

  } // MoveDeleteListener

  private final class AddListener implements ActionListener {
    private final JButton addGoal;

    private final JButton addComputedGoal;

    private AddListener(final JButton addGoal,
                        final JButton addComputedGoal) {
      this.addGoal = addGoal;
      this.addComputedGoal = addComputedGoal;
      this.addGoal.addActionListener(this);
      this.addComputedGoal.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final Object source = ae.getSource();
      if (addGoal.equals(source)) {
        addNewGoal();
      } else if (addComputedGoal.equals(source)) {
        addNewComputedGoal();
      } else {
        LOGGER.warn("AddListener: Unknown source found, ignoring: {}", source);
      }
    }

  }

}
