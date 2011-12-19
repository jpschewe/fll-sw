/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.mtu.eggplant.util.ComparisonUtils;
import fll.Utilities;
import fll.util.FLLRuntimeException;

/**
 * Check for violations in a schedule.
 */
public class ScheduleChecker {

  private long specialPerformanceChangetime = Utilities.convertMinutesToMilliseconds(30);

  private final TournamentSchedule schedule;

  private final SchedParams params;

  public ScheduleChecker(final SchedParams params,
                         final TournamentSchedule schedule) {
    this.params = params;
    this.schedule = schedule;
  }

  private void verifyNumTeamsAtTable(final Collection<ConstraintViolation> violations) {
    for (final Map.Entry<Date, Map<String, List<TeamScheduleInfo>>> dateEntry : schedule.getMatches().entrySet()) {
      for (final Map.Entry<String, List<TeamScheduleInfo>> timeEntry : dateEntry.getValue().entrySet()) {
        final List<TeamScheduleInfo> tableMatches = timeEntry.getValue();
        if (tableMatches.size() > 2) {
          final List<Integer> teams = new LinkedList<Integer>();
          for (final TeamScheduleInfo team : tableMatches) {
            teams.add(team.getTeamNumber());
          }
          final String message = String.format("Too many teams competing on table: %s at time: %s. Teams: %s",
                                               timeEntry.getKey(),
                                               TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(dateEntry.getKey()),
                                               teams);
          violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
        }
      }
    }
  }

  /**
   * Verify that there are no more than <code>numberOfTables</code> teams
   * performing at the same time.
   */
  private void verifyPerformanceAtTime(final Collection<ConstraintViolation> violations) {
    // constraint: tournament:1
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
        final Set<TeamScheduleInfo> teams;
        if (teamsAtTime.containsKey(si.getPerfTime(round))) {
          teams = teamsAtTime.get(si.getPerfTime(round));
        } else {
          teams = new HashSet<TeamScheduleInfo>();
          teamsAtTime.put(si.getPerfTime(round), teams);
        }
        teams.add(si);
      }
    }

    for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > schedule.getTableColors().size() * 2) {
        final String message = String.format("There are too many teams in performance at %s",
                                             TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
        violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
      }
    }
  }

  /**
   * Ensure that no more than 1 team is in a subjective judging station at once.
   */
  private void verifySubjectiveAtTime(final Collection<ConstraintViolation> violations) {
    // constraint: tournament:1
    // category -> time -> teams
    final Map<String, Map<Date, Set<TeamScheduleInfo>>> allSubjective = new HashMap<String, Map<Date, Set<TeamScheduleInfo>>>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      for (final SubjectiveTime subj : si.getSubjectiveTimes()) {
        final Map<Date, Set<TeamScheduleInfo>> teamsAtTime;
        if (allSubjective.containsKey(subj.getName())) {
          teamsAtTime = allSubjective.get(subj.getName());
        } else {
          teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
          allSubjective.put(subj.getName(), teamsAtTime);
        }
        final Set<TeamScheduleInfo> teams;
        if (teamsAtTime.containsKey(subj.getTime())) {
          teams = teamsAtTime.get(subj.getTime());
        } else {
          teams = new HashSet<TeamScheduleInfo>();
          teamsAtTime.put(subj.getTime(), teams);
        }
        teams.add(si);
      }
    }

    for (final Map.Entry<String, Map<Date, Set<TeamScheduleInfo>>> topEntry : allSubjective.entrySet()) {

      for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : topEntry.getValue().entrySet()) {
        if (entry.getValue().size() > schedule.getJudges().size()) {
          final String message = String.format("There are too many teams in %s at %s", topEntry.getKey(),
                                               TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
          violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null,
                                                 new SubjectiveTime(topEntry.getKey(), entry.getKey()), null, message));
        }

        final Set<String> judges = new HashSet<String>();
        for (final TeamScheduleInfo ti : entry.getValue()) {
          if (!judges.add(ti.getJudgingStation())) {
            final String message = String.format("%s judge %s cannot see more than one team at %s", topEntry.getKey(),
                                                 ti.getJudgingStation(),
                                                 TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
            violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null,
                                                   new SubjectiveTime(topEntry.getKey(), entry.getKey()), null, message));
          }
        }
      }
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "IM_BAD_CHECK_FOR_ODD", justification = "The size of a container cannot be negative")
  private void verifyTeam(final Collection<ConstraintViolation> violations,
                          final TeamScheduleInfo ti) {
    // Relationship between each subjective category
    for (final SubjectiveTime category1 : ti.getSubjectiveTimes()) {
      for (final SubjectiveTime category2 : ti.getSubjectiveTimes()) {
        if (!category1.getName().equals(category2.getName())) {
          final Date cat1Time = category1.getTime();
          final Date cat2Time = category2.getTime();

          if (cat1Time.before(cat2Time)) {
            if (cat1Time.getTime()
                + getSubjectiveDuration(category1.getName()) > cat2Time.getTime()) {
              final String message = String.format("Team %d is still in %s when they need to start %s",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            } else if (cat1Time.getTime()
                + getSubjectiveDuration(category1.getName()) + getChangetime() > cat2Time.getTime()) {
              final String message = String.format("Team %d has doesn't have enough time between %s and %s (need %d minutes)",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName(),
                                                   getChangetimeAsMinutes());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            }
          } else {
            if (cat2Time.getTime()
                + getSubjectiveDuration(category2.getName()) > cat1Time.getTime()) {
              final String message = String.format("Team %d is still in %s when they need start %s",
                                                   ti.getTeamNumber(), category2.getName(), category1.getName());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            } else if (cat2Time.getTime()
                + getSubjectiveDuration(category2.getName()) + getChangetime() > cat1Time.getTime()) {
              final String message = String.format("Team %d has doesn't have enough time between %s and %s (need %d minutes)",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName(),
                                                   getChangetimeAsMinutes());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            }
          }
        }
      }
    }

    checkConstraintTeam3(violations, ti);

    // constraint: team:4
    for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
      final String performanceName = String.valueOf(round + 1);
      verifyPerformanceVsSubjective(violations, ti, performanceName, ti.getPerfTime(round));
    }

    // constraint: team:5
    final Set<String> tableSides = new HashSet<String>();
    for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
      final TeamScheduleInfo opponent = schedule.findOpponent(ti, round);
      if (null != opponent) {
        if(opponent.getTeamNumber() == ti.getTeamNumber()) {
          throw new FLLRuntimeException("Internal error, findOpponent is broken and returned the same team");
        }
        
        if (!tableSides.add(ti.getPerfTableColor(round)
            + " " + ti.getPerfTableSide(round))) {
          final String tableMessage = String.format("Team %d is competing on %s %d more than once", ti.getTeamNumber(),
                                                    ti.getPerfTableColor(round), ti.getPerfTableSide(round));
          violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, null, tableMessage));
        }

        /* Not sure I like this check
        if (!ComparisonUtils.safeEquals(ti.getDivision(), opponent.getDivision())) {
          final String divMessage = String.format("Team %d in division %s is competing against team %d from division %s round %d",
                                                  ti.getTeamNumber(), ti.getDivision(), opponent.getTeamNumber(),
                                                  opponent.getDivision(), (round + 1));
          violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, ti.getPerfTime(round), divMessage));
        }
        */

        int opponentSide = -1;
        // figure out which round matches up
        for (int oround = 0; oround < schedule.getNumberOfRounds(); ++oround) {
          if (opponent.getPerfTime(oround).equals(ti.getPerfTime(round))) {
            opponentSide = opponent.getPerfTableSide(oround);
            break;
          }
        }
        if (-1 == opponentSide) {
          final String message = String.format("Unable to find time match for rounds between team %d and team %d at time %s",
                                               ti.getTeamNumber(), opponent.getTeamNumber(),
                                               TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(ti.getPerfTime(round)));
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerfTime(round), message));
        } else {
          if (opponentSide == ti.getPerfTableSide(round)) {
            final String message = String.format("Team %d and team %d are both on table %s side %d at the same time for round %d",
                                                 ti.getTeamNumber(), opponent.getTeamNumber(),
                                                 ti.getPerfTableColor(round), ti.getPerfTableSide(round), (round + 1));
            violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerfTime(round), message));
          }
        }

        for (int r = round + 1; r < schedule.getNumberOfRounds(); ++r) {
          final TeamScheduleInfo otherOpponent = schedule.findOpponent(ti, r);
          if (otherOpponent != null
              && opponent.equals(otherOpponent)) {
            final String message = String.format("Team %d competes against %d more than once rounds: %d, %d",
                                                 ti.getTeamNumber(), opponent.getTeamNumber(), (round + 1), (r + 1));
            violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, null, message));
          }
        }
      } else {
        // only a problem if this is not the last round and we don't have an odd
        // number of teams
        if (!(round == schedule.getNumberOfRounds() - 1 && (schedule.getSchedule().size() % 2) == 1)) {
          final String message = String.format("Team %d has no opponent for round %d", ti.getTeamNumber(), (round + 1));
          violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, ti.getPerfTime(round), message));
        }
      }
    }
  }

  /**
   * Check constraint team:3.
   */
  private void checkConstraintTeam3(final Collection<ConstraintViolation> violations,
                                    final TeamScheduleInfo ti) {
    if (schedule.getNumberOfRounds() < 2) {
      // nothing to check
      return;
    }

    for (int round = 1; round < schedule.getNumberOfRounds(); ++round) {
      final int prevRound = round - 1;
      final long perfChangetime;
      final int prevRoundOpponentRound = schedule.findOpponentRound(ti, prevRound);
      final int curRoundOpponentRound = schedule.findOpponentRound(ti, round);
      if (prevRoundOpponentRound != prevRound
          || curRoundOpponentRound != round) {
        perfChangetime = getSpecialPerformanceChangetime();
      } else {
        perfChangetime = getPerformanceChangetime();
      }
      if (ti.getPerfTime(prevRound).getTime()
          + getPerformanceDuration() > ti.getPerfTime(round).getTime()) {
        final String message = String.format("Team %d is still in performance %d when they are to start performance %d: %s - %s",
                                             ti.getTeamNumber(), prevRound + 1, round + 1,
                                             TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(ti.getPerfTime(prevRound)),
                                             TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(ti.getPerfTime(round)));
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerfTime(round), message));
      } else if (ti.getPerfTime(prevRound).getTime()
          + getPerformanceDuration() + perfChangetime > ti.getPerfTime(round).getTime()) {
        final String message = String.format("Team %d doesn't have enough time (%d minutes) between performance %d and performance %d: %s - %s",
                                             ti.getTeamNumber(), perfChangetime
                                                 / Utilities.MILLISECONDS_PER_SECOND / Utilities.SECONDS_PER_MINUTE,
                                             prevRound + 1, round + 1,
                                             TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(ti.getPerfTime(prevRound)),
                                             TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(ti.getPerfTime(round)));
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerfTime(round), message));
      }
    }

    checkForExtraPerformance(violations, ti, getChangetime());
  }

  /**
   * Check the performance constraints if this team is scheduled to stay for an
   * extra non-scored round.
   */
  private void checkForExtraPerformance(final Collection<ConstraintViolation> violations,
                                        final TeamScheduleInfo ti,
                                        final long changetime) {
    // check if the team needs to stay for any extra founds
    final String performanceName = "extra";
    for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
      final TeamScheduleInfo next = schedule.checkIfTeamNeedsToStay(ti, round);
      if (null != next) {

        // check for competing against a team twice with this extra run
        for (int r = 0; r < schedule.getNumberOfRounds(); ++r) {
          final TeamScheduleInfo otherOpponent = schedule.findOpponent(ti, r);
          if (otherOpponent != null
              && next.equals(otherOpponent)) {
            final String message = String.format("Team %d competes against %d more than once rounds: %d, extra",
                                                 ti.getTeamNumber(), next.getTeamNumber(), (r + 1));
            violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, null, message));
            violations.add(new ConstraintViolation(false, next.getTeamNumber(), null, null, null, message));
          }
        }

        // everything else checked out, only only need to check the end time
        // against subjective and the next round
        final Date performanceTime = next.getPerfTime(round);
        verifyPerformanceVsSubjective(violations, ti, performanceName, performanceTime);

        if (round + 1 < schedule.getNumberOfRounds()) {
          if (next.getPerfTime(round).getTime()
              + getPerformanceDuration() > ti.getPerfTime(round + 1).getTime()) {
            final String message = String.format("Team %d will be in performance round %d when it is starting the extra performance round: %s - %s",
                                                 ti.getTeamNumber(),
                                                 round,
                                                 TournamentSchedule.OUTPUT_DATE_FORMAT.get()
                                                                                      .format(next.getPerfTime(round)),
                                                 TournamentSchedule.OUTPUT_DATE_FORMAT.get()
                                                                                      .format(ti.getPerfTime(round + 1)));
            violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerfTime(round + 1), message));
          } else if (next.getPerfTime(round).getTime()
              + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerfTime(round + 1).getTime()) {
            final String message = String.format("Team %d doesn't have enough time (%d minutes) between performance %d and performance extra: %s - %s",
                                                 ti.getTeamNumber(),
                                                 changetime
                                                     / 1000 / Utilities.SECONDS_PER_MINUTE,
                                                 round,
                                                 TournamentSchedule.OUTPUT_DATE_FORMAT.get()
                                                                                      .format(next.getPerfTime(round)),
                                                 TournamentSchedule.OUTPUT_DATE_FORMAT.get()
                                                                                      .format(ti.getPerfTime(round + 1)));
            violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerfTime(round + 1), message));
          }
        }

      }
    }
  }

  private void verifyPerformanceVsSubjective(final Collection<ConstraintViolation> violations,
                                             final TeamScheduleInfo ti,
                                             final String performanceName,
                                             final Date performanceTime) {
    for (final SubjectiveTime subj : ti.getSubjectiveTimes()) {
      final Date time = subj.getTime();
      if (time.before(performanceTime)) {
        if (time.getTime()
            + getSubjectiveDuration(subj.getName()) > performanceTime.getTime()) {
          final String message = String.format("Team %d will be in %s when performance round %s starts",
                                               ti.getTeamNumber(), subj.getName(), performanceName);
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        } else if (time.getTime()
            + getSubjectiveDuration(subj.getName()) + getChangetime() > performanceTime.getTime()) {
          final String message = String.format("Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                                               ti.getTeamNumber(),
                                               TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(subj.getTime()),
                                               performanceName, getChangetimeAsMinutes());
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        }
      } else {
        if (performanceTime.getTime()
            + getPerformanceDuration() > time.getTime()) {
          final String message = String.format("Team %d wil be in %s when performance round %s starts",
                                               ti.getTeamNumber(), subj.getName(), performanceName);
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        } else if (performanceTime.getTime()
            + getPerformanceDuration() + getChangetime() > time.getTime()) {
          final String message = String.format("Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                                               ti.getTeamNumber(), subj.getName(), performanceName,
                                               getChangetimeAsMinutes());
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        }
      }
    }
  }

  /**
   * Verify the schedule.
   * 
   * @return the constraint violations found, empty if no violations
   * @throws IOException
   */
  public List<ConstraintViolation> verifySchedule() {
    final List<ConstraintViolation> constraintViolations = new LinkedList<ConstraintViolation>();

    for (final TeamScheduleInfo verify : schedule.getSchedule()) {
      verifyTeam(constraintViolations, verify);
    }

    verifyPerformanceAtTime(constraintViolations);
    verifyNumTeamsAtTable(constraintViolations);
    verifySubjectiveAtTime(constraintViolations);
    verifyNoOverlap(constraintViolations);

    return constraintViolations;
  }

  /**
   * Make sure that there are no overlaps in times at each scheduling location.
   * 
   * @param constraintViolations
   */
  private void verifyNoOverlap(final List<ConstraintViolation> violations) {
    final Map<String, SortedSet<Date>> tableToTime = new HashMap<String, SortedSet<Date>>();
    // category -> judge -> times
    final Map<String, Map<String, SortedSet<Date>>> subjectiveToTime = new HashMap<String, Map<String, SortedSet<Date>>>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      final String judge = si.getJudgingStation();
      for (final SubjectiveTime subj : si.getSubjectiveTimes()) {
        Map<String, SortedSet<Date>> judges = subjectiveToTime.get(subj.getName());
        if (null == judges) {
          judges = new HashMap<String, SortedSet<Date>>();
          subjectiveToTime.put(subj.getName(), judges);
        }
        SortedSet<Date> times = judges.get(judge);
        if (null == times) {
          times = new TreeSet<Date>();
          judges.put(judge, times);
        }
        times.add(subj.getTime());
      }

      for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
        final String table = si.getPerfTableColor(round);
        final int side = si.getPerfTableSide(round);
        final String tableKey = table
            + " " + side;
        SortedSet<Date> performance = tableToTime.get(tableKey);
        if (null == performance) {
          performance = new TreeSet<Date>();
          tableToTime.put(tableKey, performance);
        }
        performance.add(si.getPerfTime(round));
      }
    }

    // find violations
    for (final Map.Entry<String, Map<String, SortedSet<Date>>> categoryEntry : subjectiveToTime.entrySet()) {
      final String category = categoryEntry.getKey();
      final Map<String, SortedSet<Date>> judgeMap = categoryEntry.getValue();
      for (final Map.Entry<String, SortedSet<Date>> judgeEntry : judgeMap.entrySet()) {
        final String judge = judgeEntry.getKey();
        final SortedSet<Date> times = judgeEntry.getValue();
        Date prev = null;
        for (final Date current : times) {
          if (null != prev) {
            if (prev.getTime()
                + getSubjectiveDuration(category) > current.getTime()) {
              final String message = String.format("Overlap in %s for judge %s between %s and %s", category,
                                                   judge,
                                                   TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(prev),
                                                   TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(current));
              violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, new SubjectiveTime(category,
                                                                                                           prev), null,
                                                     null, message));
            }
          }

          prev = current;
        }
      }
    }

    for (final Map.Entry<String, SortedSet<Date>> entry : tableToTime.entrySet()) {
      Date prev = null;
      for (final Date current : entry.getValue()) {
        if (null != prev) {
          if (prev.getTime()
              + getPerformanceDuration() > current.getTime()) {
            final String message = String.format("Overlap in performance for table %s between %s and %s",
                                                 entry.getKey(),
                                                 TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(prev),
                                                 TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(current));
            violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, prev, message));
          }
        }

        prev = current;
      }
    }

  }

  /**
   * Time to allocate for a performance run.
   * 
   * @return the performanceDuration (milliseconds)
   */
  public long getPerformanceDuration() {
    return Utilities.convertMinutesToMilliseconds(params.getPerformanceMinutes());
  }

  /**
   * Time to allocate for either subjective judging.
   * 
   * @return the subjectiveDuration (milliseconds)
   */
  public long getSubjectiveDuration(final String name) {
    final SubjectiveStation station = params.getStationByName(name);
    return station.getDurationInMillis();
  }

  /**
   * This is the time required between events.
   * 
   * @return the changetime (milliseconds)
   */
  public long getChangetime() {
    return Utilities.convertMinutesToMilliseconds(params.getChangetimeMinutes());
  }

  public long getChangetimeAsMinutes() {
    return getChangetime()
        / Utilities.SECONDS_PER_MINUTE / Utilities.MILLISECONDS_PER_SECOND;
  }

  /**
   * This is the time required between performance runs for each team.
   * 
   * @return the performanceChangetime (milliseconds)
   */
  public long getPerformanceChangetime() {
    return Utilities.convertMinutesToMilliseconds(params.getPerformanceChangetimeMinutes());
  }

  /**
   * This is the time required between performance runs for the two teams in
   * involved in the performance run that crosses round 1 and round 2 when there
   * is an odd number of teams.
   * 
   * @return the specialPerformanceChangetime (milliseconds)
   */
  public long getSpecialPerformanceChangetime() {
    return specialPerformanceChangetime;
  }

  /**
   * @param specialPerformanceChangetime the specialPerformanceChangetime to set
   */
  public void setSpecialPerformanceChangetime(final long specialPerformanceChangetime) {
    this.specialPerformanceChangetime = specialPerformanceChangetime;
  }

}
