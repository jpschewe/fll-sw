/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
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
import fll.util.TextAreaEditor;
import fll.xml.RubricRange;

/**
 * Edit for a rubric range.
 */
public class RubricRangeEditor extends JPanel {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JFormattedTextField mTitle;

  private final JFormattedTextField mShortDescription;

  private final TextAreaEditor mDescription;

  private final JFormattedTextField mMin;

  private final JFormattedTextField mMax;

  private final RubricRange mRange;

  /**
   * @return the range being edited
   */
  public RubricRange getRange() {
    return mRange;
  }

  /**
   * @param range the object to edit
   */
  public RubricRangeEditor(@Nonnull final RubricRange range) {
    super(new GridBagLayout());
    mRange = range;

    GridBagConstraints gbc;

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

}
