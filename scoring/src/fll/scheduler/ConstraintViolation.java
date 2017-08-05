/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Constraint violation during scheduling.
 */
public final class ConstraintViolation implements Serializable {

  private final int team;

  public int getTeam() {
    return team;
  }

  private final LocalTime performance;

  public LocalTime getPerformance() {
    return performance;
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
   * @param team the team with the problem, may be {@link Team#NULL_TEAM_NUMBER}
   * @param subjective1 First subjective time problem, may be null
   * @param subjective2 second subjective time problem, may be null
   * @param performance if a performance problem, the time of the performance,
   *          may be null
   * @param message message to report
   */
  public ConstraintViolation(final boolean isHard,
                             final int team,
                             final SubjectiveTime subjective1,
                             final SubjectiveTime subjective2,
                             final LocalTime performance,
                             final String message) {
    this.isHard = isHard;
    this.team = team;
    if (null != subjective1) {
      subjectiveTimes.add(subjective1);
    }
    if (null != subjective2) {
      subjectiveTimes.add(subjective2);
    }
    this.performance = performance;
    this.message = message;
  }

  private final Collection<SubjectiveTime> subjectiveTimes = new LinkedList<SubjectiveTime>();

  /**
   * The subjective times that are violated.
   * 
   * @return non-null
   */
  public Collection<SubjectiveTime> getSubjectiveTimes() {
    return Collections.unmodifiableCollection(subjectiveTimes);
  }

}
