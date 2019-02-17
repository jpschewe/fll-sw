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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import org.apache.log4j.Logger;

import fll.util.FLLInternalException;
import fll.util.LogUtils;

/**
 * Field for displaying schedule times.
 * The input verifier is not automatically added so that this
 * class can be used with a {@link TableCellEditor} as well.
 * 
 * @see TournamentSchedule#parseTime(String)
 * @see TournamentSchedule#formatTime(java.time.LocalTime)
 */
/* package */ class ScheduleTimeField extends JFormattedTextField {

  public static final String MASKFORMAT = "##:##";

  /**
   * Default constructor, sets time to now.
   * 
   * @param nullAllowed if true allow null times
   */
  public ScheduleTimeField() {
    this(LocalTime.now());
  }

  /**
   * @param value the initial value for the widget
   */
  public ScheduleTimeField(final LocalTime value) {
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
   * @return Current value as a {@link LocalTime} object, may be null
   * @throws java.text.ParseException
   */
  public LocalTime getTime() {
    final String str = getTimeText(getText());
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
   * @param time the new value
   */
  public void setTime(final LocalTime time) {
    if (null != time) {
      final String formatted = TIME_FORMAT.format(time);
      setValue(formatted);
    } else {
      setValue(null);
    }
  }

  /**
   * Remove leading underscore if it exists.
   * This allows the string to be parsed as a valid time if the hours is only a
   * single digit.
   * 
   * @param raw the raw string
   * @return the string without the leading underscore, may be null
   */
  private static String getTimeText(final String raw) {
    if (null == raw) {
      return null;
    } else {
      final Matcher emptyMatch = EMPTY_FIELD_PATTERN.matcher(raw);
      if (emptyMatch.matches()) {
        return null;
      } else if (raw.startsWith("_")) {
        return raw.substring(1);
      } else {
        return raw;
      }
    }
  }

  private static final Pattern EMPTY_FIELD_PATTERN = Pattern.compile("^_+:_+$");

  /**
   * Input verifier to be used with {@link ScheduleTimeField}.
   */
  public static class TimeVerifier extends InputVerifier {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Color INVALID_COLOR = Color.red;

    public static final Color VALID_COLOR = Color.black;

    private final boolean nullAllowed;

    public TimeVerifier(final boolean pNullAllowed) {
      this.nullAllowed = pNullAllowed;
    }

    /**
     * Check if the contents of the component are a valid schedule time.
     */
    @Override
    public boolean verify(final JComponent input) {
      if (input instanceof JFormattedTextField) {
        final JFormattedTextField field = (JFormattedTextField) input;
        try {
          final String text = getTimeText(field.getText());
          if (null == text) {
            return nullAllowed;
          } else {
            TournamentSchedule.parseTime(text);
            input.setForeground(VALID_COLOR);
            return true;
          }
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
