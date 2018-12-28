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
 * A schedule constraint violation.
 */
public final class ConstraintViolation implements Serializable {

  /**
   * The type of constraint.
   */
  public static enum Type {
    // note that soft has a lower ordinal than hard because it's declared first
    /**
     * Soft constraint violations are minor warnings. It would be nice to get rid of
     * them, but not a real big deal.
     */
    SOFT,
    /**
     * Hard constraint violations are those that can not be violated and still have
     * a valid schedule. An example of this type of violation is a team being in 2
     * places at once.
     */
    HARD;
  }

  private final int team;

  /**
   * @return the team with the problem, may be {@link Team#NULL_TEAM_NUMBER}
   */
  public int getTeam() {
    return team;
  }

  private final LocalTime performance;

  /**
   * @return if a performance problem, the time of the performance,
   *         otherwise null
   */
  public LocalTime getPerformance() {
    return performance;
  }

  private final String message;

  /**
   * @return the message for the constraint violation
   */
  public String getMessage() {
    return message;
  }

  private final Type type;

  /**
   * @return the type of this constraint violation
   */
  public Type getType() {
    return type;
  }

  /**
   * @param type see {@link #getType()}
   * @param team see {@link #getTeam()}
   * @param subjective1 First subjective time problem, may be null. Added to
   *          {@link #getSubjectiveTimes()} if not null.
   * @param subjective2 second subjective time problem, may be null. Added to
   *          {@link #getSubjectiveTimes()} if not null.
   * @param performance see {@link #getPerformance()}
   * @param message see {@link #getMessage()}
   */
  public ConstraintViolation(final Type type,
                             final int team,
                             final SubjectiveTime subjective1,
                             final SubjectiveTime subjective2,
                             final LocalTime performance,
                             final String message) {
    this.type = type;
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
