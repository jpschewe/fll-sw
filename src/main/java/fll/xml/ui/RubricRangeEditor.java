/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.util.FormatterUtils;
import fll.util.TextAreaEditor;
import fll.xml.RubricRange;

/**
 * Edit for a rubric range.
 */
public final class RubricRangeEditor extends JPanel implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JFormattedTextField mTitle;

  private final JFormattedTextField mShortDescription;

  private final TextAreaEditor mDescription;

  private final JFormattedTextField mMin;

  private final JFormattedTextField mMax;

  private final RubricRange mRange;

  private final ValidityPanel rangeValid;

  /**
   * @return the range being edited
   */
  public RubricRange getRange() {
    return mRange;
  }

  /**
   * @param range the object to edit
   */
  public RubricRangeEditor(final RubricRange range) {
    super(new GridBagLayout());
    mRange = range;

    GridBagConstraints gbc;

    rangeValid = new ValidityPanel();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(rangeValid, gbc);

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Title: "), gbc);

    mTitle = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mTitle, gbc);
    mTitle.setValue(range.getTitle());

    mTitle.addPropertyChangeListener("value", e -> {
      mRange.setTitle(mTitle.getText());
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Short description: "), gbc);

    mShortDescription = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mShortDescription, gbc);
    mShortDescription.setValue(range.getShortDescription());

    mShortDescription.addPropertyChangeListener("value", e -> {
      mRange.setShortDescription(mShortDescription.getText());
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Description: "), gbc);

    mDescription = new TextAreaEditor(2, 40);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mDescription, gbc);
    mDescription.setText(range.getDescription());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Min: "), gbc);

    mMin = FormatterUtils.createIntegerField(0, Integer.MAX_VALUE);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mMin, gbc);
    mMin.setValue(range.getMin());

    mMin.addPropertyChangeListener("value", e -> {
      final Number value = (Number) mMin.getValue();
      if (null != value) {
        final int newValue = value.intValue();
        mRange.setMin(newValue);
      }
    });

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Max: "), gbc);

    mMax = FormatterUtils.createIntegerField(0, Integer.MAX_VALUE);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mMax, gbc);
    mMax.setValue(range.getMax());

    mMax.addPropertyChangeListener("value", e -> {
      final Number value = (Number) mMax.getValue();
      if (null != value) {
        final int newValue = value.intValue();
        mRange.setMax(newValue);
      }
    });

  }

  /**
   * Commit changes to all editors.
   */
  public void commitChanges() {
    try {
      mTitle.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to title, assuming bad value and ignoring", e);
    }

    try {
      mShortDescription.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to short description, assuming bad value and ignoring", e);
    }

    mRange.setDescription(mDescription.getText());

    try {
      mMin.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to min, assuming bad value and ignoring", e);
    }

    try {
      mMax.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to max, assuming bad value and ignoring", e);
    }

  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    final List<String> messages = new LinkedList<>();

    if (getRange().getMin() > getRange().getMax()) {
      messages.add("Minimum value must be less than or equal to maximum value");
    }

    if (!messages.isEmpty()) {
      rangeValid.setInvalid(String.join("<br/>", messages));
      return false;
    } else {
      rangeValid.setValid();
      return true;
    }
  }
}
