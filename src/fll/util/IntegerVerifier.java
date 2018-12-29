/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.Color;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

import org.apache.log4j.Logger;

/**
 * Ensure that the value is a valid integer between the minimum value and the
 * maximum value (inclusive).
 */
public class IntegerVerifier extends InputVerifier {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final Color INVALID_COLOR = Color.red;

  public static final Color VALID_COLOR = Color.black;

  private final int minValue;

  private final int maxValue;

  public IntegerVerifier(final int minValue,
                         final int maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  @Override
  public boolean verify(final JComponent input) {
    if (input instanceof JFormattedTextField) {
      final JFormattedTextField field = (JFormattedTextField) input;
      try {
        final String text = field.getText();
        final int value = Integer.parseInt(text);

        if (minValue <= value
            && value <= maxValue) {
          input.setForeground(VALID_COLOR);
          return true;
        } else {
          input.setForeground(INVALID_COLOR);
          return false;
        }
      } catch (final NumberFormatException e) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Unable to parse integer", e);
        }
        input.setForeground(INVALID_COLOR);
        return false;
      }
    } else {
      input.setForeground(INVALID_COLOR);
      return false;
    }
  }

}
