/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public final class FinalistDBRow implements Serializable {

  /**
   * @param categoryName {@link #getCategoryName()}
   * @param hour {@link #getHour()}
   * @param minute {@Link #getMinute()}
   * @param teamNumber {@link #getTeamNumber()}
   */
  public FinalistDBRow(@JsonProperty("categoryName") final String categoryName,
                       @JsonProperty("hour") final int hour,
                       @JsonProperty("minute") final int minute,
                       @JsonProperty("teamNumber") final int teamNumber) {
    this.categoryName = categoryName;
    this.hour = hour;
    this.minute = minute;
    this.teamNumber = teamNumber;
  }

  private final String categoryName;

  /**
   * @return name of the category being judged in
   */
  public String getCategoryName() {
    return categoryName;
  }

  private final int hour;

  /**
   * @return the hour of the finalist session
   */
  public int getHour() {
    return hour;
  }

  private final int minute;

  /**
   * @return the minute of the finalist session
   */
  public int getMinute() {
    return minute;
  }

  /**
   * @return the time as a {@link Date} object
   * @see #getHour()
   * @see #getMinute()
   */
  @JsonIgnore
  public Date getTime() {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR, getHour());
    cal.set(Calendar.MINUTE, getMinute());
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTime();
  }

  private final int teamNumber;

  /**
   * @return the team number
   */
  public int getTeamNumber() {
    return teamNumber;
  }

  /**
   * Singleton instance.
   */
  public static final TimeSort TIME_SORT_INSTANCE = new TimeSort();

  /**
   * Sort {@link FinalistDBRow} objects by time.
   */
  private static final class TimeSort implements Comparator<FinalistDBRow>, Serializable {

    @Override
    public int compare(final FinalistDBRow rowOne,
                       final FinalistDBRow rowTwo) {
      return rowOne.getTime().compareTo(rowTwo.getTime());
    }
  }

}
