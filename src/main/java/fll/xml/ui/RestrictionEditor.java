/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import fll.util.FormatterUtils;

import fll.xml.GoalScope;
import fll.xml.Restriction;

/**
 * Editor for {@link Restriction} objects.
 * {@link #commitChanges()} must be called to ensure that changes are committed
 * to the object.
 */
public class RestrictionEditor extends PolynomialEditor {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final Restriction restriction;

  private final JFormattedTextField lowerBound;

  private final JFormattedTextField upperBound;

  private final JFormattedTextField messageEditor;

  /**
   * @return the object being edited
   */
  public Restriction getRestriction() {
    return restriction;
  }

  /**
   * @param restriction the object to edit
   * @param goalScope where to get goals
   */
  public RestrictionEditor(final Restriction restriction,
                           final GoalScope goalScope) {
    super(restriction, goalScope, null);
    this.restriction = restriction;

    final Box lowerBoundBox = Box.createHorizontalBox();
    // using 0 as the index to put it at the front
    this.add(lowerBoundBox, 0);
    lowerBound = FormatterUtils.createDoubleField();
    lowerBound.setValue(restriction.getLowerBound());
    lowerBoundBox.add(lowerBound);
    lowerBoundBox.add(new JLabel(" <= "));
    lowerBoundBox.add(Box.createHorizontalGlue());

    lowerBound.addPropertyChangeListener("value", e -> {
      final Number value = (Number) lowerBound.getValue();
      if (null != value) {
        final double newValue = value.doubleValue();
        this.restriction.setLowerBound(newValue);
      }
    });

    final Box upperBoundBox = Box.createHorizontalBox();
    this.add(upperBoundBox);
    upperBoundBox.add(new JLabel(" <= "));
    upperBound = FormatterUtils.createDoubleField();
    upperBound.setValue(restriction.getUpperBound());
    upperBoundBox.add(upperBound);
    upperBoundBox.add(Box.createHorizontalGlue());

    upperBound.addPropertyChangeListener("value", e -> {
      final Number value = (Number) upperBound.getValue();
      if (null != value) {
        final double newValue = value.doubleValue();
        this.restriction.setUpperBound(newValue);
      }
    });

    messageEditor = FormatterUtils.createStringField();
    messageEditor.setValue(restriction.getMessage());
    messageEditor.setToolTipText("Message to display when the restriction is violated");
    this.add(messageEditor);

    messageEditor.addPropertyChangeListener("value", e -> {
      this.restriction.setMessage(messageEditor.getText());
    });
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      lowerBound.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to lower bound, assuming bad value and ignoring", e);
    }

    try {
      upperBound.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to upper bound, assuming bad value and ignoring", e);
    }

    try {
      messageEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to message, assuming bad value and ignoring", e);
    }
  }

  @Override
  protected void gatherValidityMessages(final Collection<String> messages) {
    super.gatherValidityMessages(messages);

    if (restriction.getLowerBound() > restriction.getUpperBound()) {
      messages.add("Lower bound must be less than or equal to upper bound");
    }
    if (!Double.isFinite(restriction.getLowerBound())) {
      messages.add("Lower bound must be a finite number");
    }
    if (!Double.isFinite(restriction.getUpperBound())) {
      messages.add("Upper bound must be a finite number");
    }
    if (null == restriction.getMessage()
        || restriction.getMessage().isBlank()) {
      messages.add("Message must have some text");
    }

  }

}
