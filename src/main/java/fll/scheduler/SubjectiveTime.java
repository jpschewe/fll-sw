/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.time.LocalTime;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a subjective judging time from the schedule.
 */
public final class SubjectiveTime implements Comparable<SubjectiveTime>, Serializable {

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
   * This is the column name, not necessarily the
   * category name.
   * 
   * @return Name of what is being judged.
   */
  public String getName() {
    return name;
  }

  private final LocalTime time;

  /**
   * @return Time of the judging session.
   */
  public LocalTime getTime() {
    return time;
  }

  @Override
  public int compareTo(final SubjectiveTime o) {
    if (null == o) {
      return 1;
    } else if (this == o) {
      return 0;
    } else {
      final int nameCompare = this.name.compareTo(o.name);
      if (0 == nameCompare) {
        return this.time.compareTo(o.time);
      } else {
        return nameCompare;
      }
    }
  }

  @Override
  public int hashCode() {
    return time.hashCode();
  }

  @Override
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean equals(final @Nullable Object o) {
    if (o == null) {
      return false;
    } else if (this == o) {
      return true;
    } else if (o.getClass() == SubjectiveTime.class) {
      return 0 == compareTo((SubjectiveTime) o);
    } else {
      return false;
    }
  }

}
