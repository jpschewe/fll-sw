/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.time.LocalTime;

import javax.swing.JFormattedTextField;

/**
 * Class that uses {@link TournamentSchedule#formatTime(LocalTime)} and
 * {@link TournamentSchedule#parseTime(String)} to handle the formatting.
 */
public class TimeFormat extends JFormattedTextField.AbstractFormatter {

  @Override
  public LocalTime stringToValue(final String text) {
    return TournamentSchedule.parseTime(text);
  }

  @Override
  public String valueToString(final Object o) {
    if (null == o) {
      return null;
    } else if (o instanceof LocalTime) {
      return valueToString((LocalTime) o);
    } else {
      throw new IllegalArgumentException("Cannot format given Object as a LocalTime");
    }
  }

  public String valueToString(final LocalTime time) {
    return TournamentSchedule.formatTime(time);
  }

}
