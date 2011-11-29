/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents performance judging time.
 */
public final class PerformanceTime implements Comparable<PerformanceTime>, Serializable {

  /**
   * @param round zero-based index
   * @param time the time
   * @param table the table color
   * @param side the table side
   */
  public PerformanceTime(final int round,
                         final Date time,
                         final String table,
                         final int side) {
    this.round = round;
    this.table = table;
    this.side = side;
    this.time = time == null ? null : new Date(time.getTime());
  }

  private final int round;

  public int getRound() {
    return round;
  }

  private final String table;

  public String getTable() {
    return table;
  }

  private final int side;

  public int getSide() {
    return side;
  }

  private final Date time;

  public Date getTime() {
    return null == time ? null : new Date(time.getTime());
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
    return Integer.valueOf(this.side).compareTo(Integer.valueOf(other.side));
  }

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
    return "round: "
        + getRound() + " time: " + TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(getTime()) + " table: "
        + getTable() + " side: " + getSide();
  }
}