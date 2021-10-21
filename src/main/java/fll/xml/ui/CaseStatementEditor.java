/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.xml.CaseStatement;
import fll.xml.GoalScope;
import fll.xml.VariableScope;

/**
 * Editor for {@link CaseStatement}.
 */
public final class CaseStatementEditor extends JPanel implements Validatable {

  private final AbstractConditionStatementEditor conditionEditor;

  private final CaseStatement stmt;

  private final CaseStatementResultEditor resultEditor;

  /**
   * @param stmt the object to edit
   * @param goalScope used to find goals
   * @param variableScope used to find variables
   * @param ifThenFont the font to use for the if/else labels
   */
  public CaseStatementEditor(final CaseStatement stmt,
                             final GoalScope goalScope,
                             final VariableScope variableScope,
                             final Font ifThenFont) {
    super(new GridBagLayout());
    this.stmt = stmt;

    GridBagConstraints gbc;

    final JLabel ifLabel = new JLabel("If");
    gbc = new GridBagConstraints();
    add(ifLabel, gbc);

    ifLabel.setFont(ifThenFont);

    conditionEditor = new AbstractConditionStatementEditor(stmt.getCondition(), goalScope, variableScope);
    conditionEditor.setBorder(BorderFactory.createLineBorder(SwitchStatementEditor.IF_COLOR));
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    add(conditionEditor, gbc);

    final JLabel thenLabel = new JLabel("Then value is");
    thenLabel.setFont(ifThenFont);
    gbc = new GridBagConstraints();
    add(thenLabel, gbc);

    resultEditor = new CaseStatementResultEditor(stmt.getResult(), goalScope, variableScope);
    resultEditor.setBorder(BorderFactory.createLineBorder(SwitchStatementEditor.THEN_COLOR));
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    add(resultEditor, gbc);
  }

  /**
   * Save all changes.
   */
  public void commitChanges() {
    conditionEditor.commitChanges();
    resultEditor.commitChanges();

    stmt.setCondition(conditionEditor.getStatement());
    stmt.setResult(resultEditor.getResult());
  }

  @Override
  public boolean checkValidity(final Collection<String> messages) {
    boolean valid = true;

    final boolean conditionValid = conditionEditor.checkValidity(messages);
    valid &= conditionValid;

    final boolean resultValid = resultEditor.checkValidity(messages);
    valid &= resultValid;

    return valid;
  }

}
