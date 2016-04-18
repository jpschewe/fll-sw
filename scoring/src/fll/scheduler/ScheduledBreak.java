/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

/**
 * Holds information about a break in the schedule.
 */
public final class ScheduledBreak {

  /**
   * @param start start of break in {@link SolverParams#getTimeIncrement()}
   *          units
   * @param end end of break in {@link SolverParams#getTimeIncrement()} units
   */
  public ScheduledBreak(final int start,
                        final int end) {
    this.start = start;
    this.end = end;
  }

  private int start;

  public int getStart() {
    return this.start;
  }

  private int end;

  public int getEnd() {
    return this.end;
  }

}