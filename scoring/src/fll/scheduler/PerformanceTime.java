/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.util.Date;

/**
 * Represents performance judging time.
 */
public final class PerformanceTime implements Comparable<PerformanceTime> {
  public PerformanceTime(final Date time,
                         final String table,
                         final int side) {
    this.table = table;
    this.side = side;
    this.time = time == null ? null : new Date(time.getTime());
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

  public int compareTo(final PerformanceTime other) {
    if (null != this.time) {
      final int timeCompare = this.time.compareTo(other.time);
      if (0 == timeCompare) {
        return this.table.compareTo(other.table);
      } else {
        return timeCompare;
      }
    } else if (null == other.time) {
      return this.table.compareTo(other.table);
    } else {
      return -1
          * other.compareTo(this);
    }
  }

}