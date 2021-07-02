/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Comparator;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents performance judging time.
 */
public final class PerformanceTime implements Comparable<PerformanceTime>, Serializable {

  /**
   * @param time see {@link #getTime()}
   * @param table see {@link #getTable()}
   * @param side see {@link #getTable()}
   * @param practice see {@link #isPractice()}
   */
  public PerformanceTime(final LocalTime time,
                         final String table,
                         final int side,
                         final boolean practice) {
    this.table = table;
    this.side = side;
    this.time = time;
    this.practice = practice;
  }

  private final boolean practice;

  /**
   * @return true if this is a practice round
   */
  public boolean isPractice() {
    return practice;
  }

  private final String table;

  /**
   * @return the table for the performance
   */
  public String getTable() {
    return table;
  }

  private final int side;

  /**
   * @return One based side number.
   */
  public int getSide() {
    return side;
  }

  /**
   * @return table and side as they would appear in a printed schedule
   */
  public String getTableAndSide() {
    return String.format("%s %d", getTable(), getSide());
  }

  private final LocalTime time;

  /**
   * @return the time of the performance
   */
  public LocalTime getTime() {
    return this.time;
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
      return Integer.compare(this.side, other.side);
    }
  }

  /**
   * Sorts first by time (earliest first), then table (lexiographic), then
   * practice vs. regular match.
   */
  @Override
  public int compareTo(final PerformanceTime other) {
    if (null != this.time) {
      final int timeCompare = this.time.compareTo(other.time);
      if (0 != timeCompare) {
        return timeCompare;
      } else {
        final int tableCompare = compareTable(other);
        if (0 == tableCompare) {
          return Boolean.compare(this.practice, other.practice);
        } else {
          return tableCompare;
        }
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

  /**
   * Sort performance times by table, then by time.
   * Null is not supported.
   */
  public static final class ByTableThenTime implements Comparator<PerformanceTime>, Serializable {

    /**
     * Single instance to save memory.
     */
    public static final ByTableThenTime INSTANCE = new ByTableThenTime();

    private ByTableThenTime() {
    }

    @Override
    public int compare(final PerformanceTime o1,
                       final PerformanceTime o2) {
      final int tableCompare = o1.compareTable(o2);
      if (0 == tableCompare) {
        final int timeCompare = o1.time.compareTo(o2.time);
        return timeCompare;
      } else {
        return tableCompare;
      }
    }

  }
}