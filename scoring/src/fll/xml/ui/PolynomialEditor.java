/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.xml.BasicPolynomial;
import fll.xml.ComplexPolynomial;
import fll.xml.FloatingPointType;
import fll.xml.GoalScope;
import fll.xml.Term;
import fll.xml.VariableScope;

/**
 * Editor for {@link BasicPolynomial} and {@link ComplexPolynomial}.
 * Add changes are immediately committed to the polynomial.
 */
/* package */ class PolynomialEditor extends JPanel {

  private final BasicPolynomial poly;

  private final JComponent termsContainer;

  private final JComboBox<FloatingPointType> floatingPointType;

  private final GoalScope goalScope;

  private final VariableScope variableScope;

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

}
