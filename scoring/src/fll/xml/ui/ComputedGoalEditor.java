/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;

import fll.xml.ComputedGoal;
import fll.xml.SwitchStatement;

/**
 * Editor for {@link ComputedGoal} objects.
 */
public class ComputedGoalEditor extends AbstractGoalEditor {

  private final VariableListEditor variableListEditor;

  /**
   * @param goal the goal to edit
   */
  public ComputedGoalEditor(final ComputedGoal goal) {
    super(goal);

    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;

    variableListEditor = new VariableListEditor(goal);
    add(variableListEditor, gbc);

    final SwitchStatement switchStmt = goal.getSwitch();
    
    // FIXME need cases

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;

    final PolynomialEditor otherwiseEditor = new PolynomialEditor(switchStmt.getDefaultCase());
    final MovableExpandablePanel otherwisePanel = new MovableExpandablePanel("Otherwise goal value is",
                                                                             otherwiseEditor, false, false);
    add(otherwisePanel, gbc);
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    variableListEditor.commitChanges();
  }

}
