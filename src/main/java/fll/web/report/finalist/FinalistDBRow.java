/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public final class FinalistDBRow implements Serializable {

  /**
   * @param endTime {@link #getEndTime()}
   * @param time {@link #getTime()}
   * @param categories {@link #getCategories()}
   */
  public FinalistDBRow(@JsonProperty("time") final LocalTime time,
                       @JsonProperty("endTime") final LocalTime endTime,
                       @JsonProperty("categories") final Map<String, Integer> categories) {
    this.time = time;
    this.endTime = endTime;
    this.categories = Collections.unmodifiableMap(new HashMap<>(categories));
  }

  private final LocalTime time;

  /**
   * @return the time of the finalist session
   */
  public LocalTime getTime() {
    return time;
  }

  private final LocalTime endTime;

  /**
   * @return the end time of the finalist session
   */
  public LocalTime getEndTime() {
    return endTime;
  }

  private final Map<String, Integer> categories;

  /**
   * Specifies the mapping of category names to teams for this row.
   * 
   * @return category name to team number
   */
  public Map<String, Integer> getCategories() {
    return categories;
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
