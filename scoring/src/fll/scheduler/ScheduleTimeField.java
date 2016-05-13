/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.Color;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
 * Field for displaying schedule times.
 * 
 * @see TimeVerifier
 * @see TournamentSchedule#parseTime(String)
 * @see TournamentSchedule#formatTime(java.time.LocalTime)
 */
/* package */ class ScheduleTimeField extends JFormattedTextField {

  public static final String MASKFORMAT = "##:##";

  /**
   * Default constructor, sets time to now.
   */
  public ScheduleTimeField() {
    this(LocalTime.now());
  }

  /**
   * @param value
   */
  public ScheduleTimeField(final LocalTime value) {
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

    setTime(value);
  }

  /**
   * Retrieve the current value as a {@link LocalTime} object
   * 
   * @return Current value as a {@link LocalTime} object
   * @throws java.text.ParseException
   */
  public LocalTime getTime() {
    final String str = getTimeText((String) getValue());
    return TournamentSchedule.parseTime(str);
  }

  /**
   * Always output without 24-hour time and without AM/PM.
   * Make sure there are always 2 digits in the hours field. This
   * is the same as the time format used by TournamentSchedule as it's
   * output format, except that we're making sure there is a leading 0.
   */
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm");

  /**
   * Set the current value.
   * 
   * @param time the new value, cannot be null
   */
  public void setTime(final LocalTime time) {
    final String formatted = TIME_FORMAT.format(time);
    setValue(formatted);
  }

  /**
   * Remove leading underscore if it exists.
   * This allows the string to be parsed as a valid time if the hours is only a
   * single digit.
   * 
   * @param raw the raw string
   * @return the string without the leading underscore
   */
  private static String getTimeText(final String raw) {
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
          final String text = getTimeText(field.getText());
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
