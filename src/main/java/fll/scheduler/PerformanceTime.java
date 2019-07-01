/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Represents performance judging time.
 */
public final class PerformanceTime implements Comparable<PerformanceTime>, Serializable {

  /**
   * @param time see {@link #getTime()}
   * @param table see {@link #getTable()}
   * @param side see {@link #getTable()}
   * @param round see {@link #getRound()}
   * @param practice see {@link #isPractice()}
   */
  public PerformanceTime(final LocalTime time,
                         final String table,
                         final int side,
                         final int round,
                         final boolean practice) {
    this.table = table;
    this.side = side;
    this.time = time;
    this.round = round;
    this.practice = practice;
  }

  private final boolean practice;

  /**
   * @return true if this is a practice round
   */
  public boolean isPractice() {
    return practice;
  }

  private final int round;

  /**
   * The round number is based on {@link #isPractice()}. There are 2 round 1s, one
   * for practice and one for regular match play.
   *
   * @return 1 based number for displaying round information.
   */
  public int getRound() {
    return round;
  }

  private final String table;

  public String getTable() {
    return table;
  }

  private final int side;

  /**
   * One based side number
   */
  public int getSide() {
    return side;
  }

  private final LocalTime time;

  public LocalTime getTime() {
    return this.time;
  }

  @Override
  public int hashCode() {
    return null == time ? 13 : time.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    } else if (this == o) {
      return true;
    } else if (o.getClass() == PerformanceTime.class) {
      return 0 == compareTo((PerformanceTime) o);
    } else {
      return false;
    }
  }

  private int compareTable(final PerformanceTime other) {
    final int tableCompare = this.table.compareTo(other.table);
    if (0 != tableCompare) {
      return tableCompare;
    } else {
      return compareSide(other);
    }
  }

  private int compareSide(final PerformanceTime other) {
    final int cmp = Integer.compare(this.side, other.side);
    if (0 == cmp) {
      return Boolean.compare(this.practice, other.practice);
    } else {
      return cmp;
    }
  }

  @Override
  public int compareTo(final PerformanceTime other) {
    if (null != this.time) {
      final int timeCompare = this.time.compareTo(other.time);
      if (0 != timeCompare) {
        return timeCompare;
      } else {
        return compareTable(other);
      }
    } else if (null == other.time) {
      return compareTable(other);
    } else {
      return -1
          * other.compareTo(this);
    }
  }

  @Override
  public String toString() {
    return "time: "
        + TournamentSchedule.formatTime(getTime())
        + " table: "
        + getTable()
        + " side: "
        + getSide();
  }
}