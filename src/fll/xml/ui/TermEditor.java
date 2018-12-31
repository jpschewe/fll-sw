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
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import fll.util.ChooseOptionDialog;
import fll.util.FormatterUtils;
import fll.xml.AbstractGoal;
import fll.xml.GoalRef;
import fll.xml.GoalScope;
import fll.xml.GoalScoreType;
import fll.xml.Term;
import fll.xml.Variable;
import fll.xml.VariableRef;
import fll.xml.VariableScope;

/**
 * Editor for {@link Term} objects.
 */
/* package */ class TermEditor extends JPanel {

  private final Term term;

  private final GoalScope goalScope;

  private final VariableScope variableScope;

  private final JFormattedTextField coefficient;

  private final JComponent refContainer;

  /**
   * @param term the term to edit
   * @param goalScope where to find goals
   * @param variableScope where to find variables, null if variables are not
   *          allowed
   */
  public TermEditor(@Nonnull final Term term,
                    @Nonnull final GoalScope goalScope,
                    final VariableScope variableScope) {
    this.term = term;
    this.goalScope = goalScope;
    this.variableScope = variableScope;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    final Box buttonBar = Box.createHorizontalBox();
    this.add(buttonBar);

    final JButton addGoal = new JButton("Add Goal");
    buttonBar.add(addGoal);
    addGoal.addActionListener(l -> addNewGoalRef());
    addGoal.setToolTipText("Add a reference to a goal");

    if (null != variableScope) {
      final JButton addVariable = new JButton("Add Variable");
      buttonBar.add(addVariable);
      addVariable.addActionListener(l -> addNewVariableRef());
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
      addGoalRef(goalRef);
    });

    if (null != variableScope) {
      term.getVariables().forEach(varRef -> {
        addVariableRef(varRef);
      });
    } else {
      if (!term.getVariables().isEmpty()) {
        throw new IllegalArgumentException("Passed a term with variables, but allow variables is false");
      }
    }

  }

  private void addNewGoalRef() {
    final Collection<AbstractGoal> goals = goalScope.getAllGoals();
    final ChooseOptionDialog<AbstractGoal> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                             new LinkedList<>(goals),
                                                                             new AbstractGoalCellRenderer());
    dialog.setVisible(true);
    final AbstractGoal selected = dialog.getSelectedValue();
    if (null != selected) {
      final GoalRef ref = new GoalRef(selected.getName(), goalScope, GoalScoreType.COMPUTED);
      this.term.addGoal(ref);
      addGoalRef(ref);
    }
  }

  private void addGoalRef(final GoalRef ref) {
    final Box row = Box.createHorizontalBox();

    row.add(new JLabel("X "));

    final GoalRefEditor editor = new GoalRefEditor(ref);
    row.add(editor);

    final JButton delete = new JButton("Delete Goal");
    delete.addActionListener(l -> {
      term.removeGoal(ref);
      GuiUtils.removeFromContainer(refContainer, row);
    });
    row.add(delete);

    row.add(Box.createHorizontalGlue());

    GuiUtils.addToContainer(refContainer, row);
  }

  private void addNewVariableRef() {
    final Collection<Variable> variables = variableScope.getAllVariables();
    final ChooseOptionDialog<Variable> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                         new LinkedList<>(variables),
                                                                         new VariableCellRenderer());
    dialog.setVisible(true);
    final Variable selected = dialog.getSelectedValue();
    if (null != selected) {
      final VariableRef ref = new VariableRef(selected.getName(), variableScope);
      this.term.addVariable(ref);
      addVariableRef(ref);
    }
  }

  private void addVariableRef(final VariableRef ref) {
    final Box row = Box.createHorizontalBox();

    row.add(new JLabel("X "));

    final VariableRefEditor editor = new VariableRefEditor(ref);
    row.add(editor);

    final JButton delete = new JButton("Delete Variable");
    delete.addActionListener(l -> {
      term.removeVariable(ref);
      GuiUtils.removeFromContainer(refContainer, row);
    });
    row.add(delete);

    row.add(Box.createHorizontalGlue());
    GuiUtils.addToContainer(refContainer, row);
  }

}
