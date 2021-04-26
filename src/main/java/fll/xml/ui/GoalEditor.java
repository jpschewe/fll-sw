/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.util.FormatterUtils;
import fll.xml.EnumeratedValue;
import fll.xml.Goal;
import fll.xml.ScoreType;

/**
 * Editor for {@link Goal} objects.
 */
public class GoalEditor extends AbstractGoalEditor {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JFormattedTextField mMultiplierEditor;

  private final JCheckBox mRequiredEditor;

  private final JComboBox<ScoreType> mScoreTypeEditor;

  private static final String COUNT_PANEL_KEY = "count";

  private static final String ENUMERATED_PANEL_KEY = "enumerated";

  private final JCheckBox mEnumerated;

  private final JFormattedTextField mMinEditor;

  private final JFormattedTextField mMaxEditor;

  private final JFormattedTextField mInitialValueEditor;

  private final EnumeratedValuesEditor enumEditor;

  private final RubricEditor rubricEditor;

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

    mMultiplierEditor = FormatterUtils.createDoubleField();
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
        goal.setMultiplier(newValue);
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
    mRequiredEditor.setSelected(goal.isRequired());
    mRequiredEditor.addActionListener(e -> {
      goal.setRequired(mRequiredEditor.isSelected());
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
      goal.setScoreType(value);
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Enumerated: "), gbc);

    mEnumerated = new JCheckBox();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mEnumerated, gbc);

    final CardLayout cardLayout = new CardLayout();
    final JPanel cardPanel = new JPanel(cardLayout);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    add(cardPanel, gbc);

    final JPanel countPanel = new JPanel(new GridBagLayout());
    cardPanel.add(countPanel, COUNT_PANEL_KEY);

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    countPanel.add(new JLabel("Minimum Value: "), gbc);

    mMinEditor = FormatterUtils.createDoubleField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    countPanel.add(mMinEditor, gbc);
    mMinEditor.setValue(goal.getMin());

    mMinEditor.addPropertyChangeListener("value", e -> {
      final Number value = (Number) mMinEditor.getValue();
      if (null != value) {
        final double newValue = value.doubleValue();
        goal.setMin(newValue);
      }
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    countPanel.add(new JLabel("Maximum Value: "), gbc);

    mMaxEditor = FormatterUtils.createDoubleField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    countPanel.add(mMaxEditor, gbc);
    mMaxEditor.setValue(goal.getMax());

    mMaxEditor.addPropertyChangeListener("value", e -> {
      final Number value = (Number) mMaxEditor.getValue();
      if (null != value) {
        final double newValue = value.doubleValue();
        goal.setMax(newValue);
      }
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    countPanel.add(new JLabel("Initial Value: "), gbc);

    mInitialValueEditor = FormatterUtils.createDoubleField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    countPanel.add(mInitialValueEditor, gbc);
    mInitialValueEditor.setValue(goal.getInitialValue());

    mInitialValueEditor.addPropertyChangeListener("value", e -> {
      final Number value = (Number) mInitialValueEditor.getValue();
      if (null != value) {
        final double newValue = value.doubleValue();
        goal.setInitialValue(newValue);
      }
    });

    enumEditor = new EnumeratedValuesEditor(goal);
    cardPanel.add(enumEditor, ENUMERATED_PANEL_KEY);

    cardLayout.show(cardPanel, goal.isEnumerated() ? ENUMERATED_PANEL_KEY : COUNT_PANEL_KEY);

    mEnumerated.addActionListener(e -> {
      cardLayout.show(cardPanel, mEnumerated.isSelected() ? ENUMERATED_PANEL_KEY : COUNT_PANEL_KEY);
    });
    mEnumerated.setSelected(goal.isEnumerated());

    rubricEditor = new RubricEditor(goal);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    add(rubricEditor, gbc);

    // object initialized

  }

  /**
   * If the enumerated box is unselected, then all values are removed from the
   * goal to ensure that {@link Goal#isEnumerated()} is consistent with the state
   * of the object.
   * 
   * @see fll.xml.ui.AbstractGoalEditor#commitChanges()
   */
  @Override
  public void commitChanges() {
    super.commitChanges();

    try {
      mMultiplierEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to multiplier, assuming bad value and ignoring", e);
    }

    if (!mEnumerated.isSelected()) {
      // clear all values
      getGoal().removeAllValues();

      try {
        mInitialValueEditor.commitEdit();
      } catch (final ParseException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Got parse exception committing initial value changes, assuming bad value and ignoring", e);
        }
      }

      try {
        mMaxEditor.commitEdit();
      } catch (final ParseException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Got parse exception committing max changes, assuming bad value and ignoring", e);
        }
      }

      try {
        mMinEditor.commitEdit();
      } catch (final ParseException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Got parse exception committing min changes, assuming bad value and ignoring", e);
        }
      }

    } else {
      enumEditor.commitChanges();
    }

    rubricEditor.commitChanges();
  }

  @Override
  public Goal getGoal() {
    return (Goal) super.getGoal();
  }

  private void checkCountValid(final Collection<String> messages) {
    if (getGoal().getMin() >= getGoal().getMax()) {
      messages.add("Minimum value must be less than maximum value");
    }
    if (getGoal().getInitialValue() < getGoal().getMin()
        || getGoal().getInitialValue() > getGoal().getMax()) {
      messages.add("Initial value must be between minimum value and maximum value");
    }
  }

  private void checkEnumeratedValid(final Collection<String> messages) {
    if (Double.isNaN(getGoal().getInitialValue())) {
      messages.add("One initial value must be set");
    }

    final Set<String> values = new HashSet<>();
    for (final EnumeratedValue enumValue : getGoal().getValues()) {
      final String value = enumValue.getValue();

      final boolean newValue = values.add(value);
      if (!newValue) {
        messages.add(String.format("Values must be unique, the value \"%s\" is used more than once", value));
      }
    }
  }

  @Override
  protected void gatherValidityMessages(final Collection<String> messages) {
    super.gatherValidityMessages(messages);

    if (!mEnumerated.isSelected()) {
      checkCountValid(messages);
    } else {
      checkEnumeratedValid(messages);
    }

    final boolean rubricValid = rubricEditor.checkValidity(messages);
    if (!rubricValid) {
      messages.add("The rubic has invalid elements");
    }
  }

}
