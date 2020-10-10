/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

/**
 * Ensure that the value is a valid double between the minimum value and the
 * maximum value (inclusive).
 */
public class DoubleVerifier extends InputVerifier {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final double minValue;

  private final double maxValue;

  /**
   * @param minValue the minimum valid value
   * @param maxValue the maximum vaid value
   */
  public DoubleVerifier(final double minValue,
                        final double maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  @Override
  public boolean verify(final JComponent input) {
    if (input instanceof JFormattedTextField) {
      final JFormattedTextField field = (JFormattedTextField) input;
      try {
        final String text = field.getText();
        final double value = Double.parseDouble(text);

        if (minValue <= value
            && value <= maxValue) {
          input.setForeground(FormatterUtils.VALID_COLOR);
          return true;
        } else {
          input.setForeground(FormatterUtils.INVALID_COLOR);
          return false;
        }
      } catch (final NumberFormatException e) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Unable to parse integer", e);
        }
        input.setForeground(FormatterUtils.INVALID_COLOR);
        return false;
      }
    } else {
      input.setForeground(FormatterUtils.INVALID_COLOR);
      return false;
    }
  }

}
