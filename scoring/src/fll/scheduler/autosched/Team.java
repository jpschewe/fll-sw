/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.autosched;

import JaCoP.core.IntVar;
import JaCoP.core.Store;

/**
 * A team for automatic scheduling.
 */
/* package */final class Team {

  public Team(final String name,
              final Store store,
              final int judgingGroup,
              final int numSubjective,
              final int numTables,
              final int numTimeslots) {
    this.judgingGroup = judgingGroup;
    this.name = name;
    this.sy = new IntVar[numSubjective][numTimeslots];
    this.sz = new IntVar[numSubjective][numTimeslots];
    this.py = new IntVar[numTables][2][numTimeslots];
    this.pz = new IntVar[numTables][2][numTimeslots];
    for (int n = 0; n < numSubjective; ++n) {
      for (int t = 0; t < numTimeslots; ++t) {
        sy[n][t] = new IntVar(store, String.format("%s.sy[%d][%d]", name, n, t), 0, 1);
        sz[n][t] = new IntVar(store, String.format("%s.sz[%d][%d]", name, n, t), 0, 1);
      }
    }

    for (int b = 0; b < numTables; ++b) {
      for (int t = 0; t < numTimeslots; ++t) {
        py[b][0][t] = new IntVar(store, String.format("%s.py[%d][%d][%d]", name, b, 0, t), 0, 1);
        py[b][1][t] = new IntVar(store, String.format("%s.py[%d][%d][%d]", name, b, 1, t), 0, 1);
        pz[b][0][t] = new IntVar(store, String.format("%s.pz[%d][%d][%d]", name, b, 0, t), 0, 1);
        pz[b][1][t] = new IntVar(store, String.format("%s.pz[%d][%d][%d]", name, b, 1, t), 0, 1);
      }
    }

  }

  private final String name;

  public String getName() {
    return name;
  }

  private final IntVar sy[][];

  private final IntVar sz[][];

  private final IntVar py[][][];

  private final IntVar pz[][][];

  private final int judgingGroup;

  public int getJudgingGroup() {
    return judgingGroup;
  }

  public IntVar getSY(final int category,
                      final int timeslot) {
    return sy[category][timeslot];
  }

  public IntVar getSZ(final int category,
                      final int timeslot) {
    return sz[category][timeslot];
  }

  public IntVar getPY(final int table,
                      final int side,
                      final int timeslot) {
    return py[table][side][timeslot];
  }

  public IntVar getPZ(final int table,
                      final int side,
                      final int timeslot) {
    return pz[table][side][timeslot];
  }

  /**
   * The timeslot that this team has for the specified category. This function
   * should not be called until the schedule has been solved.
   * 
   * @return the timeslot, -1 if no timeslot is found
   */
  int getSubjectiveTimeslot(final int category) {
    for (int t = 0; t < sz[category].length; ++t) {
      if (sz[category][t].value() > 0) {
        return t;
      }
    }
    return -1;
  }

  /**
   * Get the performance timeslot for the specified round.
   * 
   * @param round to look for, 0 based
   * @return null if no slot is found
   */
  PerformanceSlot getPerformanceTimeslot(final int round) {
    int numFound = 0;
    for (int table = 0; table < pz.length; ++table) {
      for (int side = 0; side < pz[table].length; ++side) {
        for (int t = 0; t < pz[table][side].length; ++t) {
          if (pz[table][side][t].value() > 0) {
            ++numFound;
          }
          if (round + 1 == numFound) {
            return new PerformanceSlot(table, side, t);
          }
        }
      }
    }
    return null;
  }

  public static final class PerformanceSlot {
    public PerformanceSlot(final int table,
                           final int side,
                           final int timeslot) {
      mTable = table;
      mSide = side;
      mTimeslot = timeslot;
    }

    private final int mTable;

    public int getTable() {
      return mTable;
    }

    private final int mSide;

    public int getSide() {
      return mSide;
    }

    private final int mTimeslot;

    public int getTimeslot() {
      return mTimeslot;
    }
  }
}
