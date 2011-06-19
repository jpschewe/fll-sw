/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.util.Date;

import net.mtu.eggplant.util.DateUtils;

/**
 * Constraint violation during scheduling.
 */
public final class ConstraintViolation {

  public static final int NO_TEAM = -1;

  private final int team;

  public int getTeam() {
    return team;
  }

  private final Date presentation;

  public Date getPresentation() {
    return DateUtils.copyDate(presentation);
  }

  private final Date technical;

  public Date getTechnical() {
    return DateUtils.copyDate(technical);
  }

  private final Date performance;

  public Date getPerformance() {
    return DateUtils.copyDate(performance);
  }

  private final String message;

  public String getMessage() {
    return message;
  }

  private final boolean isHard;

  public boolean isHard() {
    return isHard;
  }

  /**
   * @param isHard is this a hard constraint or a soft constraint violation?
   * @param team the team with the problem, may be {@link #NO_TEAM}
   * @param presentation if a presentation problem, the time of the presentation
   *          judging, may be null
   * @param technical if a technical problem, the time of the technical judging,
   *          may be null
   * @param performance if a performance problem, the time of the performance,
   *          may be null
   * @param message message to report
   */
  public ConstraintViolation(final boolean isHard,
                             final int team,
                             final Date presentation,
                             final Date technical,
                             final Date performance,
                             final String message) {
    //FIXME how to handle arbitrary subjective categories
    this.isHard = isHard;
    this.team = team;
    this.presentation = DateUtils.copyDate(presentation);
    this.technical = DateUtils.copyDate(technical);
    this.performance = DateUtils.copyDate(performance);
    this.message = message;
  }

}
