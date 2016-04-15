/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import fll.Utilities;

/**
 * Parameters for {@link GreedySolver}.
 */
public class SolverParams extends SchedParams {

  /**
   * @param properties
   * @throws ParseException if there is a problem parsing the properties
   */
  public SolverParams(Properties properties) throws ParseException {
    super(properties);

    this.startTime = TournamentSchedule.parseDate(properties.getProperty(GreedySolver.START_TIME_KEY));

    this.tinc = Utilities.readIntProperty(properties, GreedySolver.TINC_KEY);

  }

  private Date startTime;

  /**
   * The start time of the tournament. Nothing is scheduled before this time.
   */
  public final Date getStartTime() {
    return null == startTime ? null : new Date(startTime.getTime());
  }

  /**
   * @see #getStartTime()
   */
  public final void setStartTime(final Date v) {
    this.startTime = null == v ? null : new Date(v.getTime());
  }

  private int tinc;

  /**
   * The number of minutes to use as the base time
   * type. Normally this is set to 1, however if all activities
   * should be scheduled on 5 minute intervals, then setting this to 5 would
   * ensure that and speed up the solver. All specified time intervals must
   * be even divisible by this number.
   */
  public final int getTimeIncrement() {
    return tinc;
  }

  /**
   * @see #getTimeIncrement()
   */
  public final void setTimeIncrement(int v) {
    tinc = v;
  }

}
