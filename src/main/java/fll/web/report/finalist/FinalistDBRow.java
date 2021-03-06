/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public final class FinalistDBRow implements Serializable {

  /**
   * @param categoryName {@link #getCategoryName()}
   * @param time {@link #getTime()}
   * @param teamNumber {@link #getTeamNumber()}
   */
  public FinalistDBRow(@JsonProperty("categoryName") final String categoryName,
                       @JsonProperty("time") final LocalTime time,
                       @JsonProperty("teamNumber") final int teamNumber) {
    this.categoryName = categoryName;
    this.time = time;
    this.teamNumber = teamNumber;
  }

  private final String categoryName;

  /**
   * @return name of the category being judged in
   */
  public String getCategoryName() {
    return categoryName;
  }

  private final LocalTime time;

  /**
   * @return the time of the finalist session
   */
  public LocalTime getTime() {
    return time;
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
