/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.Color;
import java.text.ParseException;
import java.time.Duration;
import java.time.format.DateTimeParseException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import org.apache.log4j.Logger;

import fll.util.FLLInternalException;
import fll.util.LogUtils;

/**
 * Field for displaying schedule durations.
 */
/* package */ class ScheduleDurationField extends JFormattedTextField {

  public static final String MASKFORMAT = "##:##";

  /**
   * Default constructor, sets value to 0.
   */
  public ScheduleDurationField() {
    this(Duration.ofMinutes(0));
  }

  /**
   * @param value the duration to use for the initial value, may not be null
   */
  public ScheduleDurationField(final Duration value) {
    setInputVerifier(new TimeVerifier());

    try {
      final MaskFormatter mf = new MaskFormatter(MASKFORMAT);
      mf.setPlaceholderCharacter('_');
      mf.setValueClass(String.class);
      final DefaultFormatterFactory dff = new DefaultFormatterFactory(mf);
      setFormatterFactory(dff);
    } catch (final ParseException pe) {
      throw new FLLInternalException("Invalid format for MaskFormatter", pe);
    }

    setDuration(value);
  }

  /**
   * Retrieve the current value as a {@link Duration} object
   * 
   * @return Current value as a {@link Duration} object
   * @throws java.text.ParseException
   */
  public Duration getDuration() {
    final String str = getDurationText((String) getValue());
    final Duration d = parseDuration(str);
    return d;
  }

  /**
   * Parse a duration of the format ##:## assuming hours and minutes.
   * Leading underscores are ignored.
   * 
   * @param str the string to parse
   * @return null if the string cannot be parsed as a duration
   */
  private static Duration parseDuration(final String str) {
    final int colonIndex = str.indexOf(':');
    if (colonIndex < 0) {
      return null;
    }

    String hoursStr = str.substring(0, colonIndex);
    while (!hoursStr.isEmpty()
        && hoursStr.startsWith("_")) {
      hoursStr = hoursStr.substring(1);
    }

    final String minutesStr = str.substring(colonIndex
        + 1);

    final Duration minutes  = Duration.ofMinutes(Integer.parseInt(minutesStr));
    if(hoursStr.isEmpty()) {
      return minutes;
    } else {
      final Duration total = minutes.plusHours(Integer.parseInt(hoursStr));
      return total;
    }
  }

  /**
   * Set the current value.
   * 
   * @param duration the new value, cannot be null
   */
  public void setDuration(final Duration duration) {
    final long asMinutes = duration.toMinutes();
    final long hours = asMinutes / 60;
    final long minutes = asMinutes - (hours * 60);
    final String formatted = String.format("%02d:%02d", hours, minutes);
    setValue(formatted);
  }

  /**
   * Remove leading underscore if it exists.
   * This allows the string to be parsed as a valid duration if the hours is
   * only a
   * single digit.
   * 
   * @param raw the raw string
   * @return the string without the leading underscore
   */
  private static String getDurationText(final String raw) {
    if (raw.startsWith("_")) {
      return raw.substring(1);
    } else {
      return raw;
    }
  }

  private static class TimeVerifier extends InputVerifier {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Color INVALID_COLOR = Color.red;

    public static final Color VALID_COLOR = Color.black;

    /**
     * Check if the contents of the component are a valid schedule time.
     */
    @Override
    public boolean verify(final JComponent input) {
      if (input instanceof JFormattedTextField) {
        final JFormattedTextField field = (JFormattedTextField) input;
        try {
          final String text = getDurationText(field.getText());
          TournamentSchedule.parseTime(text);
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
}
