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
import fll.xml.Term;

/**
 * Editor for {@link BasicPolynomial} and {@link ComplexPolynomial}.
 */
/* package */ class PolynomialEditor extends JPanel {

  private final BasicPolynomial poly;

  private final boolean allowVariables;

  private final JComponent termsContainer;

  private final JComboBox<FloatingPointType> floatingPointType;

  /**
   * @param poly the polynomial to edit
   */
  public PolynomialEditor(@Nonnull final BasicPolynomial poly,
                          final boolean allowVariables) {
    super();
    this.poly = poly;
    this.allowVariables = allowVariables;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    final Box buttonBar = Box.createHorizontalBox();
    this.add(buttonBar);

    final JButton addTerm = new JButton("Add Term");
    buttonBar.add(addTerm);
    addTerm.addActionListener(e -> {
      addTerm(new Term());
      PolynomialEditor.this.validate();
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

  }

  private void addTerm(final Term term) {
    final Box termContainer = Box.createVerticalBox();
    termsContainer.add(termContainer);

    final Box buttonBar = Box.createHorizontalBox();
    termContainer.add(buttonBar);
    buttonBar.add(new JLabel("+ "));

    final JButton delete = new JButton("Delete Term");
    buttonBar.add(delete);
    // FIXME need action listener

    buttonBar.add(Box.createHorizontalGlue());

    final Box termBox = Box.createHorizontalBox();
    termContainer.add(termBox);
    
    termBox.add(Box.createHorizontalStrut(20));
    
    final TermEditor editor = new TermEditor(term, false);
    termBox.add(editor);
  }

}
