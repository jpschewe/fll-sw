/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Collection;

import javax.swing.Box;
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
   */
  public CaseStatementEditor(final CaseStatement stmt,
                             final GoalScope goalScope,
                             final VariableScope variableScope) {
    super(new BorderLayout());
    this.stmt = stmt;

    final Box container = Box.createVerticalBox();
    add(container, BorderLayout.CENTER);

    final Box ifBox = Box.createHorizontalBox();
    container.add(ifBox);
    final JLabel ifLabel = new JLabel("If");

    final Font labelFont = ifLabel.getFont();
    final String fontName;
    if (null == labelFont) {
      fontName = "Serif";
    } else {
      fontName = labelFont.getFontName();
    }
    final Font ifThenFont = new Font(fontName, Font.BOLD, 18);

    ifLabel.setFont(ifThenFont);
    ifBox.add(ifLabel);

    ifBox.add(Box.createHorizontalGlue());

    conditionEditor = new AbstractConditionStatementEditor(stmt.getCondition(), goalScope, variableScope);
    container.add(conditionEditor);

    final Box thenBox = Box.createHorizontalBox();
    container.add(thenBox);
    final JLabel thenLabel = new JLabel("Then goal value is");
    thenLabel.setFont(ifThenFont);
    thenBox.add(thenLabel);
    thenBox.add(Box.createHorizontalGlue());

    resultEditor = new CaseStatementResultEditor(stmt.getResult(), goalScope, variableScope);
    container.add(resultEditor);

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
