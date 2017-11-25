/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.text.ParseException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import org.apache.log4j.Logger;

import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.xml.Goal;
import fll.xml.ScoreType;
import fll.xml.WinnerType;

/**
 * Editor for {@link Goal} objects.
 */
public class GoalEditor extends AbstractGoalEditor {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JFormattedTextField mMultiplierEditor;

  private final JCheckBox mRequiredEditor;

  private final JComboBox<ScoreType> mScoreTypeEditor;

  /**
   * @param goal the goal to edit
   */
  public GoalEditor(final Goal goal) {
    super(goal);

    GridBagConstraints gbc;

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Multiplier: "), gbc);

    mMultiplierEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mMultiplierEditor, gbc);
    mMultiplierEditor.setValue(goal.getMultiplier());

    mMultiplierEditor.addPropertyChangeListener("value", e -> {
      final Number value = (Number) mMultiplierEditor.getValue();
      if (null != value) {
        final double newValue = value.doubleValue();
        getGoal().setMultiplier(newValue);
      }
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Required for awards: "), gbc);

    mRequiredEditor = new JCheckBox();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mRequiredEditor, gbc);
    mRequiredEditor.setSelected(getGoal().isRequired());
    mRequiredEditor.addActionListener(e -> {
      getGoal().setRequired(mRequiredEditor.isSelected());
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Score type: "), gbc);

    mScoreTypeEditor = new JComboBox<>(ScoreType.values());
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mScoreTypeEditor, gbc);

    mScoreTypeEditor.addActionListener(e -> {
      final ScoreType value = mScoreTypeEditor.getItemAt(mScoreTypeEditor.getSelectedIndex());
      getGoal().setScoreType(value);
    });
  }

  @Override
  public void commitChanges() {
    super.commitChanges();

    try {
      mMultiplierEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to multiplier, assuming bad value and ignoring", e);
    }

  }

  @Override
  public Goal getGoal() {
    return (Goal) super.getGoal();
  }

}
