/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.util.List;

import javax.annotation.Nonnull;

import fll.xml.ComputedGoal;
import fll.xml.GoalScope;

/**
 * Editor for {@link ComputedGoal} objects.
 */
public class ComputedGoalEditor extends AbstractGoalEditor {

  private final SwitchStatementEditor switchStatementEditor;

  private final VariableListEditor variableListEditor;

  /**
   * @param goal the goal to edit
   */
  public ComputedGoalEditor(@Nonnull final ComputedGoal goal,
                            @Nonnull final GoalScope goalScope) {
    super(goal);

    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;

    variableListEditor = new VariableListEditor(goal, goalScope);
    add(variableListEditor, gbc);

    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;

    switchStatementEditor = new SwitchStatementEditor(goal.getSwitch(), goalScope, goal);
    add(switchStatementEditor, gbc);
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    variableListEditor.commitChanges();
    switchStatementEditor.commitChanges();
  }

  @Override
  protected void gatherValidityMessages(final List<String> messages) {
    super.gatherValidityMessages(messages);

    variableListEditor.gatherValidityMessages(messages);
    switchStatementEditor.gatherValidityMessages(messages);
  }

}
