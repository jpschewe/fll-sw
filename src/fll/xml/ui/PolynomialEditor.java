/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.util.Collection;
import java.util.LinkedList;

import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.xml.AbstractGoal;
import fll.xml.BasicPolynomial;
import fll.xml.ComplexPolynomial;
import fll.xml.FloatingPointType;
import fll.xml.GoalRef;
import fll.xml.GoalScope;
import fll.xml.GoalScoreType;
import fll.xml.ScopeException;
import fll.xml.Term;
import fll.xml.VariableRef;
import fll.xml.VariableScope;

/**
 * Editor for {@link BasicPolynomial} and {@link ComplexPolynomial}.
 * Add changes are immediately committed to the polynomial.
 */
/* package */ class PolynomialEditor extends JPanel implements Validatable {

  private final BasicPolynomial poly;

  private final JComponent termsContainer;

  private final JComboBox<FloatingPointType> floatingPointType;

  private final GoalScope goalScope;

  private final VariableScope variableScope;

  private final ValidityPanel polyValid;

  /**
   * @param poly the polynomial to edit
   * @param goalScope where to find goals for the polynomial
   * @param variableScope where to find variables for the polynomial, may be null
   */
  public PolynomialEditor(@Nonnull final BasicPolynomial poly,
                          @Nonnull final GoalScope goalScope,
                          final VariableScope variableScope) {
    super();
    this.poly = poly;
    this.goalScope = goalScope;
    this.variableScope = variableScope;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    polyValid = new ValidityPanel();
    this.add(polyValid);

    final Box buttonBar = Box.createHorizontalBox();
    this.add(buttonBar);

    final JButton addTerm = new JButton("Add Term");
    buttonBar.add(addTerm);
    addTerm.addActionListener(e -> {
      addNewTerm();
    });

    buttonBar.add(Box.createHorizontalGlue());

    termsContainer = Box.createVerticalBox();
    this.add(termsContainer);
    poly.getTerms().forEach(term -> {
      addTerm(term);
    });

    // floating point type
    floatingPointType = new JComboBox<>(FloatingPointType.values());
    floatingPointType.setSelectedItem(FloatingPointType.TRUNCATE);
    this.add(floatingPointType);
    floatingPointType.setToolTipText("How to handle floating point values");
  }

  private void addNewTerm() {
    final Term term = new Term();
    poly.addTerm(term);
    addTerm(term);
  }

  private void addTerm(final Term term) {
    final Box termContainer = Box.createVerticalBox();

    final Box buttonBar = Box.createHorizontalBox();
    termContainer.add(buttonBar);
    buttonBar.add(new JLabel("+ "));

    final JButton delete = new JButton("Delete Term");
    buttonBar.add(delete);
    delete.addActionListener(l -> {
      poly.removeTerm(term);
      GuiUtils.removeFromContainer(termsContainer, termContainer);
    });

    buttonBar.add(Box.createHorizontalGlue());

    final Box termBox = Box.createHorizontalBox();
    termContainer.add(termBox);

    termBox.add(Box.createHorizontalStrut(20));

    final TermEditor editor = new TermEditor(term, goalScope, variableScope);
    termBox.add(editor);

    GuiUtils.addToContainer(termsContainer, termContainer);
  }

  /**
   * Called by {@link #checkValidity()}. If the list is empty after the call, then
   * the goal is valid, otherwise the goal is invalid and the messages will be
   * displayed to the user.
   * Subclasses should override this to add extra checks. Make sure to call the
   * parent class method.
   * 
   * @param messages put invalid messages in the list.
   */
  protected void gatherValidityMessages(final Collection<String> messages) {
    for (final Term t : poly.getTerms()) {
      for (final GoalRef gr : t.getGoals()) {
        try {
          final AbstractGoal g = goalScope.getGoal(gr.getGoalName());
          if (g.isEnumerated()
              && !GoalScoreType.COMPUTED.equals(gr.getScoreType())) {
            messages.add(String.format("Goal %s is enumerated and therefore must use a computed score type",
                                       gr.getGoalName()));
          }
        } catch (final ScopeException e) {
          messages.add(String.format("Goal %s is not known. It may have been deleted.", gr.getGoalName()));
        }
      } // foreach goal reference

      if (null != variableScope) {
        for (final VariableRef vr : t.getVariables()) {
          try {
            variableScope.getVariable(vr.getVariableName());
          } catch (final ScopeException e) {
            messages.add(String.format("Variable %s is not known. It may have been deleted.", vr.getVariableName()));
          }
        }
      } else {
        if (!t.getVariables().isEmpty()) {
          messages.add("Variables are not allowed in this polynomial");
        }
      }

    } // foreach term
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    final Collection<String> messages = new LinkedList<>();
    gatherValidityMessages(messages);

    if (!messages.isEmpty()) {
      polyValid.setInvalid(String.join("<br/>", messages));
      return false;
    } else {
      polyValid.setValid();
      return true;
    }
  }

}
