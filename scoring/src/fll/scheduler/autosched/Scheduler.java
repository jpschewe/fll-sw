/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.autosched;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import JaCoP.constraints.Constraint;
import JaCoP.constraints.Sum;
import JaCoP.constraints.XeqY;
import JaCoP.constraints.XlteqY;
import JaCoP.constraints.XmulCeqZ;
import JaCoP.constraints.XplusYeqZ;
import JaCoP.constraints.XplusYlteqZ;
import JaCoP.core.IntDomain;
import JaCoP.core.IntVar;
import JaCoP.core.IntervalDomain;
import JaCoP.core.Store;
import JaCoP.search.DepthFirstSearch;
import JaCoP.search.IndomainMin;
import JaCoP.search.SelectChoicePoint;
import JaCoP.search.SimpleSelect;
import au.com.bytecode.opencsv.CSVWriter;
import fll.Utilities;
import fll.scheduler.TournamentSchedule;
import fll.scheduler.autosched.Team.PerformanceSlot;
import fll.util.FLLRuntimeException;
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

  private final IntDomain mDefaultDomain;

  /**
   * Variable equal to 1. Used for certain constraints that require a variable.
   */
  private final IntVar mOne;

  private final IntVar mZero;

  /**
   * @throws FLLRuntimeException if the number of teams is not even
   */
  public Scheduler(final SchedParams params) throws FLLRuntimeException {
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

    // FIXME debug
    // if (teamNumber % 2 == 0) {
    // throw new FLLRuntimeException("Must have an even number of teams: "
    // + (teamNumber - 1));
    // }

    mDefaultDomain = new IntervalDomain(-10000, 10000);
    mObjective = new IntVar(mStore, "Objective", mDefaultDomain);
    mOne = new IntVar(mStore, "one", 1, 1);
    mZero = new IntVar(mStore, "zero", 0, 0);

    buildModel();
  }

  /**
   * Output the schedule. The writer is not closed by this method.
   * 
   * @param startTime the time to start the tournament at
   * @param writer where to write the data
   * @throws IOException
   */
  public void outputSchedule(final Date startTime,
                             final Writer writer) throws IOException {
    final CSVWriter csvwriter = new CSVWriter(writer);
    final List<String> headers = new LinkedList<String>();
    headers.add(TournamentSchedule.TEAM_NUMBER_HEADER);
    for (int n = 0; n < mParams.getNSubjective(); ++n) {
      headers.add(mParams.getSubjectiveName(n));
    }
    headers.add(TournamentSchedule.JUDGE_GROUP_HEADER);
    for (int round = 0; round < mParams.getNRounds(); ++round) {
      headers.add(String.format(TournamentSchedule.PERF_HEADER_FORMAT, round + 1));
      headers.add(String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round + 1));
    }
    csvwriter.writeNext(headers.toArray(new String[headers.size()]));

    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        final List<String> line = new LinkedList<String>();
        line.add(i.getName());
        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          final int slot = i.getSubjectiveTimeslot(n);
          // FIXME this should be an exception
          if (-1 == slot) {
            line.add("");
          } else {
            final Date time = convertSlotToTime(startTime, slot);
            line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(time));
          }
        }

        line.add(String.valueOf(i.getJudgingGroup() + 1));

        for (int round = 0; round < mParams.getNRounds(); ++round) {
          final PerformanceSlot slot = i.getPerformanceTimeslot(round);
          // FIXME this should be an exception
          if (null == slot) {
            line.add("");
            line.add("");
          } else {
            final Date time = convertSlotToTime(startTime, slot.getTimeslot());
            line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(time));
            line.add(String.format("Table%d %d", slot.getTable() + 1, slot.getSide() + 1));
          }
        }
        csvwriter.writeNext(line.toArray(new String[line.size()]));
      }
    }

    csvwriter.flush();
  }

  private Date convertSlotToTime(final Date startTime,
                                 final int slot) {
    final int minutes = slot
        * mParams.getTInc();
    final long milliseconds = Utilities.convertMinutesToMilliseconds(minutes);
    return new Date(startTime.getTime()
        + milliseconds);
  }

  /**
   * Build the constraint model.
   */
  private void buildModel() {
    stationBusySubjective();
    stationStartSubjective();
    noOverlapSubjective();
//    subjectiveEOS();
    teamSubjective();

//    stationBusyPerformance();
//    stationStartPerformance();
//    performanceEOS();
//    noOverlapPerformance();
//    teamPerformance();
//    perfUseBothSides();
//    performanceStart();

//    subjSubjChangetime();
//    subjPerfChangetime();
//    perfPerfChangetime();
//    perfSubjChangetime();
//    performanceChangetime();

//    teamJudging();

//    objective();
  }

  private void addConstraint(final Constraint c,
                             final String name) {
    c.id = name;
    mStore.impose(c);
  }

  private void subjectiveEOS() {
    final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = mParams.getMaxTimeSlots()
              - mParams.getSubjectiveTimeSlots(n); t < mParams.getMaxTimeSlots(); ++t) {
            sumVars.add(i.getSZ(n, t));
          }
        }
      }
    }
    final IntVar sum = new IntVar(mStore, "subjectiveEOS.sum", mDefaultDomain);
    addConstraint(new Sum(sumVars, sum), "subjectiveEOS.sum");
    addConstraint(new XeqY(sum, mZero), "subjectiveEOS");
  }

  private void performanceEOS() {
    final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int t = mParams.getMaxTimeSlots()
              - mParams.getPerformanceTimeSlots(); t < mParams.getMaxTimeSlots(); ++t) {
            sumVars.add(i.getPZ(b, 0, t));
            sumVars.add(i.getPZ(b, 1, t));
          }
        }
      }
    }
    final IntVar sum = new IntVar(mStore, "performanceEOS.sum", mDefaultDomain);
    addConstraint(new Sum(sumVars, sum), "performanceEOS.sum");
    addConstraint(new XeqY(sum, mZero), "performanceEOS");
  }

  private void objective() {
    final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          for (int n = 0; n < mParams.getNSubjective(); ++n) {
            final IntVar temp = new IntVar(mStore, String.format("objective.syTemp[%s][%d][%d]", i.getName(), n, t),
                                           mDefaultDomain);
            mStore.impose(new XmulCeqZ(i.getSY(n, t), t, temp));
            sumVars.add(temp);
          }

          for (int b = 0; b < mParams.getNTables(); ++b) {
            for (int s = 0; s < 2; ++s) {
              final IntVar temp = new IntVar(mStore, String.format("objective.pyTemp[%s][%d][%d][%d]", i.getName(), b,
                                                                   s, t), mDefaultDomain);
              mStore.impose(new XmulCeqZ(i.getPY(b, s, t), t, temp));
              sumVars.add(temp);
            }
          }

        }
      }
    }
    final IntVar sum = new IntVar(mStore, "objective.sum", mDefaultDomain);
    addConstraint(new Sum(sumVars, sum), "objective.sum");
    addConstraint(new XeqY(sum, mObjective), "objective");
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
    final IntVar sum = new IntVar(mStore, "performanceStart.sum", mDefaultDomain);
    addConstraint(new Sum(sumVars, sum), "performanceStart.sum");
    addConstraint(new XeqY(sum, mZero), "performanceStart");
  }

  private void teamJudging() {
    final int numStations = mParams.getNSubjective()
        + mParams.getNRounds();
    final IntVar numStationsVar = new IntVar(mStore, "numJudgingStations", numStations, numStations);
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

        final IntVar sum = new IntVar(mStore, String.format("teamJudging.sum[%s]", i.getName()), mDefaultDomain);
        mStore.impose(new Sum(sumVars, sum));
        mStore.impose(new XeqY(sum, numStationsVar));
      }
    }
  }

  private void perfUseBothSides() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
          for (int b = 0; b < mParams.getNTables(); ++b) {
            mStore.impose(new XeqY(i.getPY(b, 0, t), i.getPY(b, 1, t)));
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
                                             String.format("performanceChangetime.sumT[%s][%d]", i.getName(), t),
                                             mDefaultDomain);
              mStore.impose(new Sum(sumVarsT, sumT));
              final IntVar sumU = new IntVar(mStore,
                                             String.format("performanceChangetime.sumU[%s][%d]", i.getName(), u),
                                             mDefaultDomain);
              mStore.impose(new Sum(sumVarsU, sumU));

              mStore.impose(new XplusYlteqZ(sumT, sumU, mOne));
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
                        mStore.impose(new XplusYlteqZ(pyt, pyu, mOne));
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
                    mStore.impose(new XplusYlteqZ(pyt, syu, mOne));
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
                    mStore.impose(new XplusYlteqZ(syt, pyu, mOne));
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
                    mStore.impose(new XplusYlteqZ(syt, syu, mOne));
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
    final IntVar nRounds = new IntVar(mStore, "NRounds", mParams.getNRounds(), mParams.getNRounds());
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {

        final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
        for (int b = 0; b < mParams.getNTables(); ++b) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            sumVars.add(i.getPZ(b, 0, t));
            sumVars.add(i.getPZ(b, 1, t));
          }
        }
        final IntVar sum = new IntVar(mStore, String.format("teamPerformance.sum[%s]", i.getName()), mDefaultDomain);
        mStore.impose(new Sum(sumVars, sum));
        mStore.impose(new XeqY(sum, nRounds));
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
          final String sumName = String.format("teamSubjective.sum[%s][%d]", i.getName(), n);
          final String name = String.format("teamSubjective[%s][%d]", i.getName(), n);
          final IntVar sum = new IntVar(mStore, sumName, mDefaultDomain);
          addConstraint(new Sum(sumVars, sum), sumName);
          addConstraint(new XeqY(sum, mOne), name);
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
          final String sumName = String.format("noOverlapSubjective.sum[%d][%d][%d]", g, n, t);
          final String name = String.format("noOverlapSubjective[%d][%d][%d]", g, n, t);
          final IntVar sum = new IntVar(mStore, sumName, mDefaultDomain);
          addConstraint(new Sum(sumVars, sum), sumName);
          addConstraint(new XlteqY(sum, mOne), name);
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

          final IntVar sum = new IntVar(mStore, String.format("noOverlapPerformance.sum[%d][%d][%d]", b, s, t),
                                        mDefaultDomain);
          mStore.impose(new Sum(sumVars, sum));
          mStore.impose(new XlteqY(sum, mOne));
        }
      }
    }
  }

  private void stationBusySubjective() {
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = mParams.getSubjectiveTimeSlots(n) - 1; t < mParams.getMaxTimeSlots(); ++t) {
            final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
            for (int u = 0; u < mParams.getSubjectiveTimeSlots(n); ++u) {
              final IntVar sz = i.getSZ(n, t
                  - u);
              if (null != sz) {
                sumVars.add(sz);
              }
            }
            final String sumName = String.format("stationBusySubjective.sum[%s][%d][%d]", i.getName(), n, t);
            final String name = String.format("stationBusySubjective[%s][%d][%d]", i.getName(), n, t);
            final IntVar sum = new IntVar(mStore, sumName, mDefaultDomain);
            addConstraint(new Sum(sumVars, sum), sumName);
            addConstraint(new XlteqY(sum, i.getSY(n, t)), name);
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
            for (int t = mParams.getPerformanceChangetimeSlots() - 1; t < mParams.getMaxTimeSlots(); ++t) {
              final ArrayList<IntVar> sumVars = new ArrayList<IntVar>();
              for (int u = 0; u < mParams.getPerformanceTimeSlots(); ++u) {
                final IntVar pz = i.getPZ(b, s, t
                    - u);
                if (null != pz) {
                  sumVars.add(pz);
                }
              }
              final IntVar sum = new IntVar(mStore, String.format("stationBusyPerformance.sum[%s][%d][%d][%d]",
                                                                  i.getName(), b, s, t), mDefaultDomain);
              mStore.impose(new Sum(sumVars, sum));
              mStore.impose(new XlteqY(sum, i.getPY(b, s, t)));
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
          for (int t = 1; t < mParams.getMaxTimeSlots(); ++t) {
            final IntVar sztMinus1 = i.getSY(n, t - 1);
            if (null != sztMinus1) {
              final String tempName = String.format("stationStartSubjective.temp[%s][%d][%d]", i.getName(), n, t);
              final String name = String.format("stationStartSubjective[%s][%d][%d]", i.getName(), n, t);
              final IntVar temp = new IntVar(mStore, tempName, mDefaultDomain);
              addConstraint(new XplusYeqZ(i.getSZ(n, t), sztMinus1, temp), tempName);
              addConstraint(new XlteqY(i.getSY(n, t), temp), name);
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
            for (int t = 1; t < mParams.getMaxTimeSlots(); ++t) {
              final IntVar sztMinus1 = i.getSY(b, t - 1);
              if (null != sztMinus1) {
                final IntVar temp = new IntVar(mStore, String.format("stationStartPerformance.temp[%s][%d][%d][%d]",
                                                                     i.getName(), b, s, t), mDefaultDomain);
                mStore.impose(new XplusYeqZ(i.getPZ(b, s, t), sztMinus1, temp));
                mStore.impose(new XlteqY(i.getPY(b, s, t), temp));
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

    final IntVar[] vars = choiceVariables.toArray(new IntVar[choiceVariables.size()]);
    final SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(vars, null, new IndomainMin<IntVar>());

    final DepthFirstSearch<IntVar> search = new DepthFirstSearch<IntVar>();

    // final TraceGenerator<IntVar> trace = new TraceGenerator<IntVar>(search,
    // select, vars);
    LOGGER.debug("--All variables before search");
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            LOGGER.debug(i.getSZ(n, t));
            LOGGER.debug(i.getSY(n, t));
          }
        }
      }
    }
    LOGGER.debug("--end all variables before search");
    final boolean result = search.labeling(mStore, select, mObjective);
    if (result) {
      LOGGER.debug("*** Objective: "
          + search.getCostVariable() + " = " + search.getCostValue());
    }
    LOGGER.debug("--All variables after search");
    for (final Map.Entry<Integer, List<Team>> entry : mTeams.entrySet()) {
      for (final Team i : entry.getValue()) {
        for (int n = 0; n < mParams.getNSubjective(); ++n) {
          for (int t = 0; t < mParams.getMaxTimeSlots(); ++t) {
            LOGGER.debug(i.getSZ(n, t));
            LOGGER.debug(i.getSY(n, t));
          }
        }
      }
    }
    LOGGER.debug("--end all variables after search");

    LOGGER.debug("Timeout? "
        + search.timeOutOccured);
    final long T2 = System.currentTimeMillis();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("*** Execution time = "
          + (T2 - T1) + " ms");
    }

    return result;

  }

}
