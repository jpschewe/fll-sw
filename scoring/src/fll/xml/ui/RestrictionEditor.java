/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;

import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import org.apache.log4j.Logger;

import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.xml.GoalScope;
import fll.xml.Restriction;

/**
 * Editor for {@link Restriction} objects.
 * {@link #commitChanges()} must be called to ensure that changes are committed
 * to the object.
 */
public class RestrictionEditor extends PolynomialEditor {

  private static final Logger LOGGER = LogUtils.getLogger();

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

  public RestrictionEditor(@Nonnull final Restriction restriction,
                           @Nonnull final GoalScope goalScope) {
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

    final Box upperBoundBox = Box.createHorizontalBox();
    this.add(upperBoundBox);
    upperBoundBox.add(new JLabel(" <= "));
    upperBound = FormatterUtils.createDoubleField();
    upperBound.setValue(restriction.getUpperBound());
    upperBoundBox.add(upperBound);
    upperBoundBox.add(Box.createHorizontalGlue());

    messageEditor = FormatterUtils.createDatabaseNameField();
    messageEditor.setValue(restriction.getMessage());
    messageEditor.setToolTipText("Message to display when the restriction is violated");
    this.add(messageEditor);
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

}
