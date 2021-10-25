/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JPanel;

import fll.xml.ConditionStatement;
import fll.xml.GoalScope;
import fll.xml.InequalityComparison;
import fll.xml.VariableScope;

/**
 * Edit {@link ConditionStatement}.
 */
/* package */ class ConditionStatementEditor extends JPanel implements Validatable {

  private final PolynomialEditor leftEditor;

  private final PolynomialEditor rightEditor;

  ConditionStatementEditor(final ConditionStatement stmt,
                           final GoalScope goalScope,
                           final VariableScope variableScope) {
    super(new BorderLayout());

    final Box container = Box.createVerticalBox();
    add(container, BorderLayout.CENTER);

    leftEditor = new PolynomialEditor(stmt.getLeft(), goalScope, variableScope);
    container.add(leftEditor);

    final InequalityEditor comparisonEditor = new InequalityEditor(new InequalityComparison[] { InequalityComparison.GREATER_THAN,
                                                                                                InequalityComparison.GREATER_THAN_OR_EQUAL,
                                                                                                InequalityComparison.LESS_THAN,
                                                                                                InequalityComparison.LESS_THAN_OR_EQUAL,
                                                                                                InequalityComparison.EQUAL_TO,
                                                                                                InequalityComparison.NOT_EQUAL_TO });
    container.add(comparisonEditor);
    comparisonEditor.addActionListener(e -> {
      stmt.setComparison(comparisonEditor.getItemAt(comparisonEditor.getSelectedIndex()));
    });
    comparisonEditor.setSelectedItem(stmt.getComparison());

    rightEditor = new PolynomialEditor(stmt.getRight(), goalScope, variableScope);
    container.add(rightEditor);
  }

  @Override
  public boolean checkValidity(Collection<String> messagesToDisplay) {
    boolean valid = true;

    final boolean leftValid = leftEditor.checkValidity(messagesToDisplay);
    valid &= leftValid;

    final boolean rightValid = rightEditor.checkValidity(messagesToDisplay);
    valid &= rightValid;

    return valid;
  }

}
