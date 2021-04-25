/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.ChooseOptionDialog;
import fll.xml.Variable;
import fll.xml.VariableRef;

/**
 * Editor for {@link VariableRef} objects.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
class VariableRefEditor extends Box {

  private final VariableRef variableRef;

  private final JButton editor;

  /**
   * @param ref the object to edit
   */
  VariableRefEditor(@Nonnull final VariableRef ref) {
    super(BoxLayout.LINE_AXIS);

    this.variableRef = ref;

    editor = new JButton(ref.getVariableName());
    add(editor);
    editor.setToolTipText("Click to change the referenced variable");
    ref.getVariable().addPropertyChangeListener(nameListener);
    editor.addActionListener(l -> {
      final Collection<Variable> variables = variableRef.getVariableScope().getAllVariables();

      final ChooseOptionDialog<Variable> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                           new LinkedList<>(variables),
                                                                           new VariableCellRenderer());
      dialog.setVisible(true);
      final Variable selected = dialog.getSelectedValue();
      if (null != selected) {
        ref.getVariable().removePropertyChangeListener(nameListener);

        ref.setVariableName(selected.getName());
        editor.setText(selected.getName());

        ref.getVariable().addPropertyChangeListener(nameListener);
      }
    });
  }

  private final NameChangeListener nameListener = new NameChangeListener();

  private class NameChangeListener implements PropertyChangeListener {

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if ("name".equals(evt.getPropertyName())) {
        final String newName = (String) evt.getNewValue();
        variableRef.setVariableName(newName);
        editor.setText(newName);
      }
    }
  }

}
