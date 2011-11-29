/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * The objective value from a schedule. It is assumed that ObectiveValue objects
 * from the same solver be compared. Comparing ObjectiveValue objects across
 * solvers is undefined.
 */
/* package */class ObjectiveValue implements Comparable<ObjectiveValue> {

  private final int solutionNumber;

  private final int latestPerformanceTime;

  private final int[] numTeams;

  private final int[] latestSubjectiveTime;

  private final int numWarnings;

  /**
   * @param solutionNumber the number of solution that this is
   * @param latestPerformanceTime the timeslot for the latest performance time
   * @param numTeams the number of teams in each group
   * @param latestSubjectiveTime the latest subjective time for each group
   * @param numWarnings the number of soft constraint violations out of
   *          ScheduleChecker
   */
  public ObjectiveValue(final int solutionNumber,
                        final int latestPerformanceTime,
                        final int[] numTeams,
                        final int[] latestSubjectiveTime,
                        final int numWarnings) {
    if (numTeams.length != latestSubjectiveTime.length) {
      throw new IllegalArgumentException("numTeams.length must equal latestSubjectiveTime.length");
    }

    this.solutionNumber = solutionNumber;
    this.latestPerformanceTime = latestPerformanceTime;
    this.numTeams = new int[numTeams.length];
    System.arraycopy(numTeams, 0, this.numTeams, 0, numTeams.length);
    this.latestSubjectiveTime = new int[latestSubjectiveTime.length];
    System.arraycopy(latestSubjectiveTime, 0, this.latestSubjectiveTime, 0, latestSubjectiveTime.length);
    this.numWarnings = numWarnings;
  }

  private int compareSubjective(final ObjectiveValue other) {
    if (other.numTeams.length != this.numTeams.length) {
      throw new IllegalArgumentException("Cannot compare ObjectiveValues with different number of teams or groups");
    }

    final int[] groupCompare = new int[this.numTeams.length];
    boolean equal = true;
    for (int group = 0; group < this.numTeams.length; ++group) {
      if (this.numTeams[group] != other.numTeams[group]) {
        throw new IllegalArgumentException("Cannot compare ObjectiveValues with different number of teams in group");
      }

      if (this.latestSubjectiveTime[group] < other.latestSubjectiveTime[group]) {
        groupCompare[group] = -1;
        equal = false;
      } else if (this.latestSubjectiveTime[group] > other.latestSubjectiveTime[group]) {
        groupCompare[group] = 1;
        equal = false;
      } else {
        groupCompare[group] = 0;
      }
    }
    if (equal) {
      return 0;
    }

    // for checking when done
    final boolean[] allScheduled = new boolean[this.numTeams.length];
    Arrays.fill(allScheduled, true);

    final boolean[] groupsCompared = new boolean[this.numTeams.length];
    Arrays.fill(groupsCompared, false);

    while (!Arrays.equals(groupsCompared, allScheduled)) {
      int minNumTeams = Integer.MAX_VALUE;
      int groupToCompare = -1;
      for (int group = 0; group < this.numTeams.length; ++group) {
        if (!groupsCompared[group]
            && this.numTeams[group] < minNumTeams) {
          groupToCompare = group;
          minNumTeams = this.numTeams[group];
        }
      }

      if (groupToCompare == -1) {
        throw new RuntimeException("Internal error, shoudl never have groupToCompare be -1 after search");
      }
      groupsCompared[groupToCompare] = true;
      if (this.latestSubjectiveTime[groupToCompare] < other.latestSubjectiveTime[groupToCompare]) {
        return -1;
      } else if (this.latestSubjectiveTime[groupToCompare] > other.latestSubjectiveTime[groupToCompare]) {
        return 1;
      }
    }

    throw new RuntimeException("Should never get to the end of this function as we checked for equality above");

  }

  @Override
  public int compareTo(final ObjectiveValue other) {
    if (this.latestPerformanceTime < other.latestPerformanceTime) {
      return -1;
    } else if (this.latestPerformanceTime > other.latestPerformanceTime) {
      return 1;
    } else {
      final int subjectiveCompare = compareSubjective(other);
      if (subjectiveCompare != 0) {
        return subjectiveCompare;
      } else if (this.numWarnings < other.numWarnings) {
        return -1;
      } else if (this.numWarnings > other.numWarnings) {
        return 1;
      }
    }
    return 0;
  }

  @Override
  public boolean equals(final Object o) {
    if (null == o) {
      return false;
    } else if (o == this) {
      return true;
    } else if (o.getClass() == this.getClass()) {
      final ObjectiveValue other = (ObjectiveValue) o;
      return 0 == compareTo(other);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return latestPerformanceTime;
  }

  @Override
  public String toString() {
    final StringWriter strWriter = new StringWriter();
    final PrintWriter writer = new PrintWriter(strWriter);

    writer.println("Solution: "
        + this.solutionNumber);
    writer.println("latestPerformanceTime: "
        + this.latestPerformanceTime);
    for (int group = 0; group < this.numTeams.length; ++group) {
      writer.println("group "
          + group + " numTeams: " + this.numTeams[group] + " latestSubjectiveTime: " + this.latestSubjectiveTime[group]);
    }
    writer.println("Warnings: "
        + this.numWarnings);

    writer.flush();
    return strWriter.toString();
  }

  public int getLatestPerformanceTime() {
    return latestPerformanceTime;
  }

}
