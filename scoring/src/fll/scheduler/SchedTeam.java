/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

/**
 * Used by {@link GreedyScheduler} to track team information.
 */
/* packet */final class SchedTeam {
  public SchedTeam(final int index,
                   final int group) {
    this.index = index;
    this.group = group;
  }

  private final int index;

  public int getIndex() {
    return index;
  }

  private final int group;

  public int getGroup() {
    return group;
  }

  @Override
  public int hashCode() {
    return getIndex()
        ^ getGroup();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    } else if (o.getClass() == SchedTeam.class) {
      final SchedTeam other = (SchedTeam) o;
      return getGroup() == other.getGroup()
          && getIndex() == other.getIndex();
    } else {
      return false;
    }
  }

}