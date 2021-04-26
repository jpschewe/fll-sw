/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.util.Collection;

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
   * @param goalScope used for looking up goals
   */
  public ComputedGoalEditor(final ComputedGoal goal,
                            final GoalScope goalScope) {
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
  protected void gatherValidityMessages(final Collection<String> messages) {
    super.gatherValidityMessages(messages);

    variableListEditor.gatherValidityMessages(messages);
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    boolean valid = super.checkValidity(messagesToDisplay);

    boolean localValid = switchStatementEditor.checkValidity(messagesToDisplay);
    valid &= localValid;

    return valid;
  }
}
