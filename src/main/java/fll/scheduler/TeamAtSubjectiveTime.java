/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.ComparisonChain;

/**
 * A team at a particular subjective station.
 * Sort is by time, then station, then award group, then team number.
 */
/* package */ class TeamAtSubjectiveTime implements Comparable<TeamAtSubjectiveTime> {

  /**
   * @return the team information
   */
  public TeamScheduleInfo getTeamInfo() {
    return teamInfo;
  }

  private final TeamScheduleInfo teamInfo;

  /**
   * @return the time and station information
   */
  public SubjectiveTime getSubjTime() {
    return subjTime;
  }

  private final SubjectiveTime subjTime;

  /* package */ TeamAtSubjectiveTime(final TeamScheduleInfo teamInfo,
                                     final SubjectiveTime subjTime) {
    this.teamInfo = teamInfo;
    this.subjTime = subjTime;
  }

  @Override
  public int compareTo(final TeamAtSubjectiveTime other) {
    return ComparisonChain.start() //
                          .compare(this.getSubjTime().getTime(), other.getSubjTime().getTime()) //
                          .compare(this.getSubjTime().getName(), other.getSubjTime().getName()) //
                          .compare(this.getTeamInfo().getAwardGroup(), other.getTeamInfo().getAwardGroup()) //
                          .compare(this.getTeamInfo().getJudgingGroup(), other.getTeamInfo().getJudgingGroup()) //
                          .compare(this.getTeamInfo().getTeamNumber(), other.getTeamInfo().getTeamNumber()) //
                          .result();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getSubjTime(), this.getTeamInfo());
  }

  @Override
  @EnsuresNonNullIf(expression="#1", result=true)
  public boolean equals(final @Nullable Object o) {
    if (o == this) {
      return true;
    } else if (null == o) {
      return false;
    } else if (o.getClass().equals(this.getClass())) {
      return 0 == compareTo((TeamAtSubjectiveTime) o);
    } else {
      return false;
    }
  }

}
