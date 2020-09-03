/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.xml.CaseStatement;
import fll.xml.ComplexPolynomial;
import fll.xml.GoalScope;
import fll.xml.SwitchStatement;
import fll.xml.VariableScope;

/**
 * Editor for {@link CaseStatement}.
 */
public final class CaseStatementEditor extends JPanel implements Validatable {

  private final AbstractConditionStatementEditor conditionEditor;

  private final CaseStatement stmt;

  private static final String RESULT_POLY_PANEL = "resultPoly";

  private static final String RESULT_SWITCH_PANEL = "resultSwitch";

  final JCheckBox resultType;

  final PolynomialEditor resultPolyEditor;

  final SwitchStatementEditor resultSwitchEditor;

  public CaseStatementEditor(@Nonnull final CaseStatement stmt,
                             @Nonnull final GoalScope goalScope,
                             final VariableScope variableScope) {
    super(new BorderLayout());
    this.stmt = stmt;

    final Box container = Box.createVerticalBox();
    add(container, BorderLayout.CENTER);

    final Box ifBox = Box.createHorizontalBox();
    container.add(ifBox);
    final JLabel ifLabel = new JLabel("If");

    final Font ifThenFont = new Font(ifLabel.getFont().getFontName(), Font.BOLD, 18);

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

    final Box resultTypeBox = Box.createHorizontalBox();
    container.add(resultTypeBox);
    resultType = new JCheckBox("If/Then");
    resultType.setToolTipText("Check this if the result is a series of if/then statements");
    resultTypeBox.add(resultType);
    resultTypeBox.add(Box.createHorizontalGlue());

    final boolean showResultPoly = null == stmt.getResultSwitch();
    final ComplexPolynomial stmtResultPoly = stmt.getResultPoly();
    final ComplexPolynomial resultPoly = null == stmtResultPoly ? new ComplexPolynomial() : stmtResultPoly;
    final SwitchStatement stmtSwitch = stmt.getResultSwitch();
    final SwitchStatement resultSwitch = null == stmtSwitch ? new SwitchStatement() : stmtSwitch;

    final CardLayout resultLayout = new CardLayout();
    final JPanel resultPanel = new JPanel(resultLayout);
    container.add(resultPanel);

    resultPolyEditor = new PolynomialEditor(resultPoly, goalScope, variableScope);
    resultPanel.add(resultPolyEditor, RESULT_POLY_PANEL);

    resultSwitchEditor = new SwitchStatementEditor(resultSwitch, goalScope, variableScope);
    resultPanel.add(resultSwitchEditor, RESULT_SWITCH_PANEL);

    resultType.addActionListener(e -> {
      if (resultType.isSelected()) {
        resultLayout.show(resultPanel, RESULT_SWITCH_PANEL);
        stmt.setResultPoly(null);
        stmt.setResultSwitch(resultSwitch);
      } else {
        resultLayout.show(resultPanel, RESULT_POLY_PANEL);
        stmt.setResultPoly(resultPoly);
        stmt.setResultSwitch(null);
      }
    });

    if (!showResultPoly) {
      resultType.doClick();
    }

  }

  /**
   * Save all changes.
   */
  public void commitChanges() {
    stmt.setCondition(conditionEditor.getStatement());
  }

  @Override
  public boolean checkValidity(final Collection<String> messages) {
    boolean valid = true;

    final boolean conditionValid = conditionEditor.checkValidity(messages);
    valid &= conditionValid;

    if (resultType.isSelected()) {
      final boolean resultSwitchValid = resultSwitchEditor.checkValidity(messages);
      valid &= resultSwitchValid;
    } else {
      final boolean resultValid = resultPolyEditor.checkValidity(messages);
      valid &= resultValid;
    }

    return valid;
  }

}
