/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

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

  public TeamAtSubjectiveTime(final TeamScheduleInfo teamInfo,
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
                          .compare(this.getTeamInfo().getTeamNumber(), other.getTeamInfo().getTeamNumber()) //
                          .result();
  }

}
