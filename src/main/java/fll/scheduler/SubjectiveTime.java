/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Represents a subjective judging time from the schedule.
 */
public final class SubjectiveTime implements Serializable {

  /**
   * @param name name of the column in the schedule
   * @param time the time
   */
  public SubjectiveTime(final String name,
                        final LocalTime time) {
    this.name = name;
    this.time = time;
  }

  private final String name;

  /**
   * Name of what is being judged.
   * This is the column name, not necessarily the
   * category name.
   */
  public String getName() {
    return name;
  }

  private final LocalTime time;

  /**
   * Time of the judging session.
   */
  public LocalTime getTime() {
    return time;
  }
}
