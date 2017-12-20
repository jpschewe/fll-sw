/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.util.FormatterUtils;
import fll.xml.AbstractGoal;
import fll.xml.GoalScoreType;
import fll.xml.Term;
import fll.xml.Variable;

/**
 * Editor for {@link Term} objects.
 */
/* package */ class TermEditor extends JPanel {

  private final Term term;

  private final JFormattedTextField coefficient;
  
  private final JComponent refContainer;

  public TermEditor(final Term term,
                    final boolean allowVariables) {
    this.term = term;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    final Box buttonBar = Box.createHorizontalBox();
    this.add(buttonBar);

    final JButton addGoal = new JButton("Add Goal");    
    buttonBar.add(addGoal);
    // FIXME listener
    addGoal.setToolTipText("Add a reference to a goal");

    if (allowVariables) {
      final JButton addVariable = new JButton("Add Variable");
      buttonBar.add(addVariable);
      // FIXME listener
      
      addVariable.setToolTipText("Add a reference to a variable");
    }

    buttonBar.add(Box.createHorizontalGlue());

    final Box coefficientBox = Box.createHorizontalBox();
    this.add(coefficientBox);

    coefficient = FormatterUtils.createDoubleField();
    coefficientBox.add(coefficient);

    coefficient.addPropertyChangeListener("value", e -> {
      final Number value = (Number) coefficient.getValue();
      if (null != value) {
        final double newValue = value.doubleValue();
        term.setCoefficient(newValue);
      }
    });

    coefficient.setValue(term.getCoefficient());
    coefficientBox.add(Box.createHorizontalGlue());
    
    refContainer = Box.createVerticalBox();
    this.add(refContainer);

    term.getGoals().forEach(goalRef -> {
      addGoal(goalRef.getGoal());
    });

    if (allowVariables) {
      term.getVariables().forEach(varRef -> {
        addVariable(varRef.getVariable());
      });
    } else {
      if (!term.getVariables().isEmpty()) {
        throw new IllegalArgumentException("Passed a term with variables, but allow variables is false");
      }
    }

  }

  private void addGoal(final AbstractGoal goal) {
    final Box row = Box.createHorizontalBox();
    refContainer.add(row);
    
    row.add(new JLabel("X "));
    
    final JLabel name = new JLabel(goal.getTitle());
    //FIXME add listener
    row.add(name);
    
    final JComboBox<GoalScoreType> scoreType = new JComboBox<>(GoalScoreType.values());
    row.add(scoreType);
    //FIXME add listener and a place to store the value
    
    final JButton delete = new JButton("Delete Goal");
    //FIXME add action listener
    row.add(delete);

    row.add(Box.createHorizontalGlue());
  }

  private void addVariable(final Variable variable) {
    final Box row = Box.createHorizontalBox();
    refContainer.add(row);
    
    row.add(new JLabel("X "));
    
    final JLabel name = new JLabel(variable.getName());
    //FIXME add listener
    row.add(name);
       
    final JButton delete = new JButton("Delete Variable");
    //FIXME add action listener
    row.add(delete);
    
    row.add(Box.createHorizontalGlue());

  }

}
