/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.util.Collection;
import java.util.LinkedList;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import fll.util.ChooseOptionDialog;
import fll.xml.Variable;
import fll.xml.VariableRef;

/**
 * Editor for {@link VariableRef} objects.
 */
class VariableRefEditor extends JPanel {

  private final VariableRef variableRef;

  /**
   * @param ref the object to edit
   */
  public VariableRefEditor(@Nonnull final VariableRef ref) {
    this.variableRef = ref;

    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

    final JButton goal = new JButton(ref.getVariableName());
    add(goal);
    goal.setToolTipText("Click to change the referenced variable");
    goal.addActionListener(l -> {
      final Collection<Variable> variables = variableRef.getVariableScope().getAllVariables();

      final ChooseOptionDialog<Variable> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                           new LinkedList<>(variables),
                                                                           new VariableCellRenderer());
      dialog.setVisible(true);
      final Variable selected = dialog.getSelectedValue();
      if (null != selected) {
        ref.setVariableName(selected.getName());
        goal.setText(selected.getName());
      }
    });
  }
}
