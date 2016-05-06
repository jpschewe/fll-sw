/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.text.ParseException;
import java.time.LocalTime;

import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import fll.util.FLLInternalException;

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
    return TournamentSchedule.parseTime((String) getValue());
  }

  /**
   * Set the current value.
   * 
   * @param time the new value, cannot be null
   */
  public void setTime(final LocalTime time) {
    setValue(TournamentSchedule.formatTime(time));
  }

}
