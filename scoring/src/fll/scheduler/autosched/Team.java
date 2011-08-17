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
    if (category < 0
        || category >= sy.length || timeslot < 0 || timeslot >= sy[category].length) {
      return null;
    }
    return sy[category][timeslot];
  }

  public IntVar getSZ(final int category,
                      final int timeslot) {
    if (category < 0
        || category >= sz.length || timeslot < 0 || timeslot >= sz[category].length) {
      return null;
    }
    return sz[category][timeslot];
  }

  public IntVar getPY(final int table,
                      final int side,
                      final int timeslot) {
    if (table < 0
        || table >= py.length || side < 0 || side >= py[table].length || timeslot < 0
        || timeslot >= py[table][side].length) {
      return null;
    }
    return py[table][side][timeslot];
  }

  public IntVar getPZ(final int table,
                      final int side,
                      final int timeslot) {
    if (table < 0
        || table >= pz.length || side < 0 || side >= pz[table].length || timeslot < 0
        || timeslot >= pz[table][side].length) {
      return null;
    }
    return pz[table][side][timeslot];
  }

  // FIXME int getSubjectiveTimeslot(final int category)
  // FIXME {table, side, time} getPerformanceTimeslot(final int round)

}
