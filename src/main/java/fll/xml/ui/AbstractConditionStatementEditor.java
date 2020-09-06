/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.checkerframework.checker.nullness.qual.NonNull;

import fll.xml.AbstractConditionStatement;
import fll.xml.ConditionStatement;
import fll.xml.EnumConditionStatement;
import fll.xml.GoalScope;
import fll.xml.StringConstant;
import fll.xml.VariableScope;

/**
 * Editor for {@link AbstractConditionStatement}.
 */
public class AbstractConditionStatementEditor extends JPanel implements Validatable {

  private static final String ENUM_PANEL = "enum";

  private static final String STANDARD_PANEL = "standard";

  private AbstractConditionStatement stmt;

  private final EnumConditionStatementEditor enumStmtEditor;

  private final JCheckBox enumCondition;

  private final ConditionStatementEditor standardStmtEditor;

  /**
   * This is a {@link ConditionStatement} or an {@link EnumConditionStatement}
   * depending on what the user has chosen.
   * 
   * @return the statement
   */
  public AbstractConditionStatement getStatement() {
    return stmt;
  }

  public AbstractConditionStatementEditor(@NonNull final AbstractConditionStatement stmt,
                                          @NonNull final GoalScope goalScope,
                                          final VariableScope variableScope) {
    super(new BorderLayout());
    this.stmt = stmt;
    final ConditionStatement condStmt;
    final EnumConditionStatement enumStmt;
    if (stmt instanceof ConditionStatement) {
      condStmt = (ConditionStatement) stmt;
      enumStmt = new EnumConditionStatement(new StringConstant("missing value"), new StringConstant("missing value"));
    } else if (stmt instanceof EnumConditionStatement) {
      condStmt = new ConditionStatement();
      enumStmt = (EnumConditionStatement) stmt;
    } else {
      throw new IllegalArgumentException("Unknown condition statement class: "
          + stmt.getClass());
    }

    enumCondition = new JCheckBox("Enumeration Comparison");
    enumCondition.setToolTipText("Select this when you want to compare specific values of an enumerated goal");
    add(enumCondition, BorderLayout.NORTH);

    final CardLayout conditionLayout = new CardLayout();
    final JPanel conditionPanel = new JPanel(conditionLayout);
    add(conditionPanel, BorderLayout.CENTER);

    standardStmtEditor = new ConditionStatementEditor(condStmt, goalScope, variableScope);
    conditionPanel.add(standardStmtEditor, STANDARD_PANEL);

    final Box enumPanel = Box.createVerticalBox();
    conditionPanel.add(enumPanel, ENUM_PANEL);

    enumStmtEditor = new EnumConditionStatementEditor(enumStmt, goalScope);
    enumPanel.add(enumStmtEditor);
    enumPanel.add(Box.createVerticalGlue());

    enumCondition.addActionListener(e -> {
      if (enumCondition.isSelected()) {
        conditionLayout.show(conditionPanel, ENUM_PANEL);
        this.stmt = enumStmt;
      } else {
        conditionLayout.show(conditionPanel, STANDARD_PANEL);
        this.stmt = condStmt;
      }
    });

    if (stmt instanceof EnumConditionStatement) {
      enumCondition.doClick();
    }
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    enumStmtEditor.commitChanges();
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    boolean valid = true;

    final boolean conditionValid;
    if (enumCondition.isSelected()) {
      conditionValid = enumStmtEditor.checkValidity(messagesToDisplay);
      valid &= conditionValid;
    } else {
      conditionValid = standardStmtEditor.checkValidity(messagesToDisplay);
      valid &= conditionValid;
    }

    return valid;
  }

}
