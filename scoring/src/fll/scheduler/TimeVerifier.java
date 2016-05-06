/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.Color;
import java.time.format.DateTimeParseException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Verify times for {@link TournamentSchedule}
 */
/* packet */ class TimeVerifier extends InputVerifier {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final Color INVALID_COLOR = Color.red;

  public static final Color VALID_COLOR = Color.black;

  /**
   * 
   */
  public TimeVerifier() {
  }

  /**
   * Check if the contents of the component are a valid schedule time.
   */
  @Override
  public boolean verify(final JComponent input) {
    if (input instanceof JFormattedTextField) {
      final JFormattedTextField field = (JFormattedTextField) input;
      try {
        TournamentSchedule.parseTime(field.getText());
        input.setForeground(VALID_COLOR);
        return true;
      } catch (final DateTimeParseException e) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Unable to parse schedule time", e);
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
