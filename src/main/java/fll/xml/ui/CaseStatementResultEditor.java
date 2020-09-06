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

import fll.util.FLLInternalException;
import fll.xml.CaseStatementResult;
import fll.xml.ComplexPolynomial;
import fll.xml.GoalScope;
import fll.xml.SwitchStatement;
import fll.xml.VariableScope;

/**
 * Editor for {@link CaseStatementResult}.
 */
public final class CaseStatementResultEditor extends JPanel implements Validatable {

  private static final String RESULT_POLY_PANEL = "resultPoly";

  private static final String RESULT_SWITCH_PANEL = "resultSwitch";

  private final JCheckBox resultType;

  private final PolynomialEditor resultPolyEditor;

  private final SwitchStatementEditor resultSwitchEditor;

  private final SwitchStatement resultSwitch;

  private final ComplexPolynomial resultPoly;

  /**
   * @param result the object to edit
   * @param goalScope where to find goals
   * @param variableScope where to find variables
   */
  public CaseStatementResultEditor(final CaseStatementResult result,
                                   final GoalScope goalScope,
                                   final VariableScope variableScope) {
    super(new BorderLayout());

    final Box container = Box.createVerticalBox();
    add(container, BorderLayout.CENTER);

    final Box resultTypeBox = Box.createHorizontalBox();
    container.add(resultTypeBox);
    resultType = new JCheckBox("If/Then");
    resultType.setToolTipText("Check this if the result is a series of if/then statements");
    resultTypeBox.add(resultType);
    resultTypeBox.add(Box.createHorizontalGlue());

    final boolean showResultPoly;

    if (result instanceof ComplexPolynomial) {
      resultPoly = (ComplexPolynomial) result;
      resultSwitch = new SwitchStatement();
      showResultPoly = true;
    } else if (result instanceof SwitchStatement) {
      resultPoly = new ComplexPolynomial();
      resultSwitch = (SwitchStatement) result;
      showResultPoly = false;
    } else {
      throw new FLLInternalException("Unexpected type for condition statement result: "
          + result);
    }

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
      } else {
        resultLayout.show(resultPanel, RESULT_POLY_PANEL);
      }
    });

    if (!showResultPoly) {
      resultType.doClick();
    }
  }

  /**
   * @return the case statement result
   */
  public CaseStatementResult getResult() {
    if (resultType.isSelected()) {
      return resultSwitch;
    } else {
      return resultPoly;
    }
  }

  /**
   * Save all changes.
   */
  public void commitChanges() {
    resultSwitchEditor.commitChanges();    
  }

  @Override
  public boolean checkValidity(final Collection<String> messages) {
    boolean valid = true;

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
