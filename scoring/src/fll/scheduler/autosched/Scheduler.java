/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.autosched;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import JaCoP.constraints.Sum;
import JaCoP.constraints.XeqC;
import JaCoP.constraints.XeqY;
import JaCoP.constraints.XlteqC;
import JaCoP.constraints.XlteqY;
import JaCoP.constraints.XmulCeqZ;
import JaCoP.constraints.XplusYeqZ;
import JaCoP.constraints.XplusYlteqZ;
import JaCoP.core.IntVar;
import JaCoP.core.Store;
import JaCoP.search.DepthFirstSearch;
import JaCoP.search.IndomainMin;
import JaCoP.search.Search;
import JaCoP.search.SelectChoicePoint;
import JaCoP.search.SimpleSelect;
import fll.util.LogUtils;

/**
 * Implements the set of constraints that will create a schedule for a
 * tournament. This uses the an MILP solver.
 */
public class Scheduler {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Key = judging group Value = teams in the group
   */
  private final Map<Integer, List<Team>> mTeams = new HashMap<Integer, List<Team>>();

  private final Store mStore = new Store();

  private final SchedParams mParams;

  private final IntVar mObjective;

  /**
   * Variable equal to 1. Used for certain constraints that require a variable.
   */
  private final IntVar mOne;

  public Scheduler(final SchedParams params) {
    mParams = params;

    final List<Integer> teamInformation = mParams.getTeams();
    int teamNumber = 1;
    for (int group = 0; group < teamInformation.size(); ++group) {
      final int numTeams = teamInformation.get(group);
      final List<Team> teams = new LinkedList<Team>();
      for (int n = 0; n < numTeams; ++n) {
        final Team team = new Team(String.format("Team %d", teamNumber), mStore, group, mParams.getNSubjective(),
                                   mParams.getNTables(), mParams.getMaxTimeSlots());
        ++teamNumber;
        teams.add(team);
      }
      mTeams.put(group, teams);
    }

    mObjective = new IntVar(mStore, "Objective");
    mOne = new IntVar(mStore, "one", 1, 1);
    buildModel();
  }

  /**
   * Build the constraint model.
   */
  private void buildModel() {
    stationBusySubjective();
    stationBusyPerformance();
    stationStartSubjective();
    stationStartPerformance();
    noOverlapSubjective();
    noOverlapPerformance();
    teamSubjective();
    teamPerformance();
    subjSubjChangetime();
    subjPerfChangetime();
    perfPerfChangetime();
    perfSubjChangetime();
    performanceChangetime();
    perfUseBothSides();
    teamJudging();
    performanceStart();
    objective();
  }

  private void objective() {
    final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          for (int n = 0; n < mParams.getNSubjective(); ++n) {
            final IntVar temp = new IntVar(mStore, String.format("objective.syTemp[%s][%d][%d]", i.getName(), n, t));
            new XmulCeqZ(i.getSY(n, t), t, temp);
            sumVars.add(temp);
          }

          for (int b = 0; b < mParams.getNTables(); ++b) {
            for (int s = 0; s < 2; ++s) {
              final IntVar temp = new IntVar(mStore, String.format("objective.pyTemp[%s][%d][%d][%d]", i.getName(), b,
                                                                   s, t));
              new XmulCeqZ(i.getPY(b, s, t), t, temp);
              sumVars.add(temp);
            }
          }

        }
      }
    }
    final IntVar sum = new IntVar(mStore, "objective.sum");
    new Sum(sumVars, sum);
    new XeqY(sum, mObjective);
  }

  private void performanceStart() {
    final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int t = 0; t < 60 / mParams.getTInc(); ++t) {
            if (null != i.getPY(b, 0, t)) {
              sumVars.add(i.getPY(b, 0, t));
              sumVars.add(i.getPY(b, 1, t));
            }
          }
        }
      }
    }
    final IntVar sum = new IntVar(mStore, "performanceStart.sum");
    new Sum(sumVars, sum);
    new XeqC(sum, 0);
  }

  private void teamJudging() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();

        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          for (int n = 0; n < mParams.getNSubjective(); ++n) {
            sumVars.add(i.getSZ(n, t));
          }

          for (int b = 0; b < mParams.getNTables(); ++b) {
            sumVars.add(i.getPZ(b, 0, t));
            sumVars.add(i.getPZ(b, 1, t));
          }
        }

        final IntVar sum = new IntVar(mStore, String.format("teamJudging.sum[%s]", i.getName()));
        new Sum(sumVars, sum);
        new XeqC(sum, mParams.getNSubjective()
            + mParams.getNRounds());
      }
    }
  }

  private void perfUseBothSides() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          for (int b = 0; b < mParams.getNTables(); ++b) {
            new XeqY(i.getPY(b, 0, t), i.getPY(b, 1, t));
          }
        }
      }
    }
  }

  private void performanceChangetime() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          for (int u = 0; u < mParams.getPerformanceTimeSlots()
              + mParams.getPerformanceChangetimeSlots(); ++u) {
            if (null != i.getPY(0, 0, t
                + u)) {
              final ArrayList<IntVar> sumVarsT = new ArrayList<IntVar>();
              final ArrayList<IntVar> sumVarsU = new ArrayList<IntVar>();
              for (int b = 0; b < mParams.getNTables(); ++b) {
                sumVarsT.add(i.getPY(b, 0, t));
                sumVarsT.add(i.getPY(b, 1, t));

                sumVarsU.add(i.getPY(b, 0, t
                    + u));
                sumVarsU.add(i.getPY(b, 1, t
                    + u));
              }

              final IntVar sumT = new IntVar(mStore,
                                             String.format("performanceChangetime.sumT[%s][%d]", i.getName(), t));
              new Sum(sumVarsT, sumT);
              final IntVar sumU = new IntVar(mStore,
                                             String.format("performanceChangetime.sumU[%s][%d]", i.getName(), u));
              new Sum(sumVarsU, sumU);

              new XplusYlteqZ(sumT, sumU, mOne);
            }
          }
        }
      }
    }
  }

  private void perfPerfChangetime() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int s = 0; s < 2; ++s) {
            for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
              final IntVar pyt = i.getPY(b, s, t);

              for (int d = 0; d < mParams.getNTables(); ++d) {
                for (int e = 0; e < 2; ++e) {
                  if (d != b
                      && e != s) {
                    for (int u = 0; u < mParams.getPerformanceTimeSlots()
                        + mParams.getChangetimeSlots(); ++u) {
                      final IntVar pyu = i.getPY(d, e, t
                          + u);
                      if (null != pyu) {
                        new XplusYlteqZ(pyt, pyu, mOne);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void perfSubjChangetime() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int s = 0; s < 2; ++s) {
            for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
              final IntVar pyt = i.getPY(b, s, t);

              for (int n = 0; n < mParams.getNSubjective(); ++n) {
                for (int u = 0; u < mParams.getPerformanceChangetimeSlots(); ++u) {
                  final IntVar syu = i.getSY(n, t
                      + u);
                  if (null != syu) {
                    new XplusYlteqZ(pyt, syu, mOne);
                  }
                }
              }

            }
          }
        }
      }
    }
  }

  private void subjPerfChangetime() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {

        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            final IntVar syt = i.getSY(n, t);

            for (int b = 0; b < mParams.getNTables(); ++b) {
              for (int s = 0; s < 2; ++s) {
                for (int u = 0; u < mParams.getSubjectiveTimeSlots(n)
                    + mParams.getChangetimeSlots(); ++u) {
                  final IntVar pyu = i.getPY(b, s, t
                      + u);
                  if (null != pyu) {
                    new XplusYlteqZ(syt, pyu, mOne);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void subjSubjChangetime() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {

        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {

            final IntVar syt = i.getSY(n, t);
            for (int d = 0; d < mParams.getNSubjective(); ++d) {
              if (d != n) {

                for (int u = 0; u < mParams.getSubjectiveTimeSlots(n)
                    + mParams.getChangetimeSlots(); ++u) {
                  final IntVar syu = i.getSY(d, t
                      + u);
                  if (null != syu) {
                    new XplusYlteqZ(syt, syu, mOne);
                  }
                }

              }
            }

          }
        }

      }
    }
  }

  private void teamPerformance() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {

        final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            sumVars.add(i.getPZ(b, 0, t));
            sumVars.add(i.getPZ(b, 1, t));
          }
        }
        final IntVar sum = new IntVar(mStore, String.format("teamPerformance.sum[%s]", i.getName()));
        new Sum(sumVars, sum);
        new XeqC(sum, mParams.getNRounds());
      }
    }
  }

  private void teamSubjective() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int n = 0; n < mParams.getNSubjective(); ++n) {

          final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            sumVars.add(i.getSZ(n, t));
          }
          final IntVar sum = new IntVar(mStore, String.format("teamSubjective.sum[%s][%d]", i.getName(), n));
          new Sum(sumVars, sum);
          new XeqC(sum, 1);
        }
      }
    }
  }

  private void noOverlapSubjective() {
    for (int n = 0; n < mParams.getNSubjective(); ++n) {
      for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
        for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
          final int g = entry.getKey();
          final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();

          for (final Team i : entry.getValue()) {
            sumVars.add(i.getSY(n, t));
          }
          final IntVar sum = new IntVar(mStore, String.format("noOverlapSubjective.sum[%d][%d][%d]", g, n, t));
          new Sum(sumVars, sum);
          new XlteqC(sum, 1);
        }
      }
    }
  }

  private void noOverlapPerformance() {
    for (int b = 0; b < mParams.getNTables(); ++b) {
      for (int s = 0; s < 2; ++s) {
        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();

          for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
            for (final Team i : entry.getValue()) {
              sumVars.add(i.getPY(b, s, t));
            }
          }

          final IntVar sum = new IntVar(mStore, String.format("noOverlapPerformance.sum[%d][%d][%d]", b, s, t));
          new Sum(sumVars, sum);
          new XlteqC(sum, 1);

        }
      }
    }
  }

  private void stationBusySubjective() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
            for (int u = 1; u < mParams.getSubjectiveTimeSlots(n); ++u) {
              final IntVar sz = i.getSZ(n, t
                  - u + 1);
              if (null != sz) {
                sumVars.add(sz);
              }
            }
            final IntVar sum = new IntVar(mStore, String.format("stationBusySubjective.sum[%s][%d][%d]", i.getName(),
                                                                n, t));
            new Sum(sumVars, sum);
            new XlteqY(sum, i.getSY(n, t));

          }
        }
      }
    }
  }

  private void stationBusyPerformance() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int s = 0; s < 2; ++s) {
            for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
              final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
              for (int u = 1; u < mParams.getPerformanceTimeSlots(); ++u) {
                final IntVar pz = i.getPZ(b, s, t
                    - u + 1);
                if (null != pz) {
                  sumVars.add(pz);
                }
              }
              final IntVar sum = new IntVar(mStore, String.format("stationBusyPerformance.sum[%s][%d][%d][%d]",
                                                                  i.getName(), b, s, t));
              new Sum(sumVars, sum);
              new XlteqY(sum, i.getPY(b, s, t));
            }
          }
        }
      }
    }
  }

  private void stationStartSubjective() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            final IntVar sztMinus1 = i.getSY(n, t - 1);
            if (null != sztMinus1) {
              final IntVar temp = new IntVar(mStore, String.format("stationStartSubjective.temp[%s][%d][%d]",
                                                                   i.getName(), n, t));
              new XplusYeqZ(i.getSZ(n, t), sztMinus1, temp);
              new XlteqY(i.getSY(n, t), temp);
            }
          }
        }
      }
    }
  }

  private void stationStartPerformance() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int s = 0; s < 2; ++s) {
            for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
              final IntVar sztMinus1 = i.getSY(b, t - 1);
              if (null != sztMinus1) {
                final IntVar temp = new IntVar(mStore, String.format("stationStartPerformance.temp[%s][%d][%d][%d]",
                                                                     i.getName(), b, s, t));
                new XplusYeqZ(i.getPZ(b, s, t), sztMinus1, temp);
                new XlteqY(i.getPY(b, s, t), temp);
              }
            }
          }
        }
      }
    }
  }

  public boolean solve() {

    final long T1 = System.currentTimeMillis();

    // get all variables from all teams
    final List<IntVar> choiceVariables = new LinkedList<IntVar>();
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          // performance
          for (int b = 0; b < mParams.getNTables(); ++b) {
            for (int s = 0; s < 2; ++s) {
              choiceVariables.add(i.getPY(b, s, t));
              choiceVariables.add(i.getPZ(b, s, t));
            }
          }

          // subjective
          for (int n = 0; n < mParams.getNSubjective(); ++n) {
            choiceVariables.add(i.getSY(n, t));
            choiceVariables.add(i.getSZ(n, t));
          }
        }
      }
    }

    final SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(choiceVariables.toArray(new IntVar[0]), null,
                                                                      new IndomainMin<IntVar>());

    final Search<IntVar> search = new DepthFirstSearch<IntVar>();

    final boolean result = search.labeling(mStore, select, mObjective);

    if (result) {
      mStore.print();
    }

    final long T2 = System.currentTimeMillis();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("\n\t*** Execution time = "
          + (T2 - T1) + " ms");
    }

    return result;

  }

}
