/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Holds information about a break in the schedule.
 */
public final class ScheduledBreak implements Serializable {

  /**
   * @param start {@link #getStart()}
   * @param duration {@link #getDuration()}
   */
  public ScheduledBreak(final LocalTime start,
                        final Duration duration) {
    this.start = start;
    this.duration = duration;
  }

  private LocalTime start;

  /**
   * @return start time of the break
   */
  public LocalTime getStart() {
    return this.start;
  }

  private Duration duration;

  /**
   * @return duration of the break
   */
  public Duration getDuration() {
    return this.duration;
  }

  /**
   * @return end time of the break
   */
  public LocalTime getEnd() {
    return this.start.plus(this.duration);
  }

}