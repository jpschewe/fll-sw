/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;

import javax.annotation.Nonnull;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.xml.GoalScope;
import fll.xml.Variable;

/**
 * 
 */
public class VariableEditor extends JPanel {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JFormattedTextField mNameEditor;

  private final Variable variable;

  /**
   * @return the variable being edited
   */
  public Variable getVariable() {
    return variable;
  }

  public VariableEditor(@Nonnull final Variable variable,
                        @Nonnull final GoalScope goalScope) {
    super(new GridBagLayout());

    this.variable = variable;

    GridBagConstraints gbc;

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Name: "), gbc);

    mNameEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mNameEditor, gbc);
    mNameEditor.setValue(variable.getName());

    mNameEditor.addPropertyChangeListener("value", e -> {
      final String oldName = variable.getName();
      final String newName = mNameEditor.getText();
      variable.setName(newName);
      fireNameChange(oldName, newName);
    });

    mNameEditor.setColumns(80);
    mNameEditor.setMaximumSize(mNameEditor.getPreferredSize());

    final PolynomialEditor polyEditor = new PolynomialEditor(variable, goalScope, null);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    add(polyEditor, gbc);

  }

  protected void fireNameChange(final String oldName,
                                final String newName) {
    firePropertyChange("name", oldName, newName);
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      mNameEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to name, assuming bad value and ignoring", e);
    }
  }

}
