/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.util.FLLRuntimeException;

/**
 * Check for violations in a schedule.
 */
public class ScheduleChecker {

  private Duration specialPerformanceChangetime = Duration.ofMinutes(30);

  private final TournamentSchedule schedule;

  private final SchedParams params;

  /**
   * Initialize a scheduler checker.
   * 
   * @param params the parameters to use when checking the schedule
   * @param schedule the schedule to check
   */
  public ScheduleChecker(@Nonnull final SchedParams params,
                         @Nonnull final TournamentSchedule schedule) {
    this.params = params;
    this.schedule = schedule;
  }

  private void verifyNumTeamsAtTable(final Collection<ConstraintViolation> violations) {
    for (final Map.Entry<LocalTime, Map<String, List<TeamScheduleInfo>>> dateEntry : schedule.getMatches().entrySet()) {
      for (final Map.Entry<String, List<TeamScheduleInfo>> timeEntry : dateEntry.getValue().entrySet()) {
        final List<TeamScheduleInfo> tableMatches = timeEntry.getValue();
        if (tableMatches.size() > 2) {
          final List<Integer> teams = new LinkedList<Integer>();
          for (final TeamScheduleInfo team : tableMatches) {
            teams.add(team.getTeamNumber());
          }
          final String message = String.format("Too many teams competing on table: %s at time: %s. Teams: %s",
                                               timeEntry.getKey(), TournamentSchedule.formatTime(dateEntry.getKey()),
                                               teams);
          violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, Team.NULL_TEAM_NUMBER, null, null, null,
                                                 message));
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
    final Map<LocalTime, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<>();
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

    for (final Map.Entry<LocalTime, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > schedule.getTableColors().size()
          * 2) {
        final String message = String.format("There are too many teams in performance at %s",
                                             TournamentSchedule.formatTime(entry.getKey()));
        violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, Team.NULL_TEAM_NUMBER, null, null, null,
                                               message));
      }
    }
  }

  /**
   * Ensure that no more than 1 team is in a subjective judging station at once.
   */
  private void verifySubjectiveAtTime(final Collection<ConstraintViolation> violations) {
    // constraint: tournament:1
    // category -> time -> teams
    final Map<String, Map<LocalTime, Set<TeamScheduleInfo>>> allSubjective = new HashMap<>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      for (final SubjectiveTime subj : si.getSubjectiveTimes()) {
        final Map<LocalTime, Set<TeamScheduleInfo>> teamsAtTime;
        if (allSubjective.containsKey(subj.getName())) {
          teamsAtTime = allSubjective.get(subj.getName());
        } else {
          teamsAtTime = new HashMap<>();
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

    for (final Map.Entry<String, Map<LocalTime, Set<TeamScheduleInfo>>> topEntry : allSubjective.entrySet()) {

      for (final Map.Entry<LocalTime, Set<TeamScheduleInfo>> entry : topEntry.getValue().entrySet()) {
        if (entry.getValue().size() > schedule.getJudgingGroups().size()) {
          final String message = String.format("There are too many teams in %s at %s", topEntry.getKey(),
                                               TournamentSchedule.formatTime(entry.getKey()));
          violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, Team.NULL_TEAM_NUMBER, null,
                                                 new SubjectiveTime(topEntry.getKey(), entry.getKey()), null, message));
        }

        final Set<String> judges = new HashSet<String>();
        for (final TeamScheduleInfo ti : entry.getValue()) {
          if (!judges.add(ti.getJudgingGroup())) {
            final String message = String.format("%s judge %s cannot see more than one team at %s", topEntry.getKey(),
                                                 ti.getJudgingGroup(), TournamentSchedule.formatTime(entry.getKey()));
            violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, Team.NULL_TEAM_NUMBER, null,
                                                   new SubjectiveTime(topEntry.getKey(), entry.getKey()), null,
                                                   message));
          }
        }
      }
    }
  }

  @SuppressFBWarnings(value = "IM_BAD_CHECK_FOR_ODD", justification = "The size of a container cannot be negative")
  private void verifyTeam(final Collection<ConstraintViolation> violations,
                          final TeamScheduleInfo ti) {
    // Relationship between each subjective category
    for (final SubjectiveTime category1 : ti.getSubjectiveTimes()) {
      for (final SubjectiveTime category2 : ti.getSubjectiveTimes()) {
        if (!category1.getName().equals(category2.getName())) {
          final LocalTime cat1Start = category1.getTime();
          final LocalTime cat2Start = category2.getTime();

          if (cat1Start.isBefore(cat2Start)) {
            final LocalTime cat1End = cat1Start.plus(getSubjectiveDuration(category1.getName()));
            if (cat1End.isAfter(cat2Start)) {
              final String message = String.format("Team %d is still in %s when they need to start %s",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName());
              violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), category1,
                                                     category2, null, message));
              return;
            } else if (cat1End.plus(getChangetime()).isAfter(cat2Start)) {
              final String message = String.format("Team %d has doesn't have enough time between %s and %s (need %d)",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName(),
                                                   getChangetime().toMinutes());
              violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), category1,
                                                     category2, null, message));
              return;
            }
          } else {
            final LocalTime cat2End = cat2Start.plus(getSubjectiveDuration(category2.getName()));
            if (cat2End.isAfter(cat1Start)) {
              final String message = String.format("Team %d is still in %s when they need start %s", ti.getTeamNumber(),
                                                   category2.getName(), category1.getName());
              violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), category1,
                                                     category2, null, message));
              return;
            } else if (cat2End.plus(getChangetime()).isAfter(cat1Start)) {
              final String message = String.format("Team %d has doesn't have enough time between %s and %s (need %d)",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName(),
                                                   getChangetime().toMinutes());
              violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), category1,
                                                     category2, null, message));
              return;
            }
          }
        }
      }
    }

    checkConstraintTeam3(violations, ti);

    // constraint: team:4
    for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
      final String performanceName = String.valueOf(round
          + 1);
      verifyPerformanceVsSubjective(violations, ti, performanceName, ti.getPerfTime(round));
    }

    // constraint: team:5
    final Map<String, PerformanceTime> tableSides = new HashMap<String, PerformanceTime>();
    for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
      final TeamScheduleInfo opponent = schedule.findOpponent(ti, round);
      if (null != opponent) {
        if (opponent.getTeamNumber() == ti.getTeamNumber()) {
          throw new FLLRuntimeException("Internal error, findOpponent is broken and returned the same team");
        }

        final String key = ti.getPerfTableColor(round)
            + " "
            + ti.getPerfTableSide(round);
        if (tableSides.containsKey(key)) {
          final PerformanceTime otherTime = tableSides.get(key);
          final String tableMessage = String.format("Team %d is competing on %s %d more than once", ti.getTeamNumber(),
                                                    ti.getPerfTableColor(round), ti.getPerfTableSide(round));
          violations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, ti.getTeamNumber(), null, null,
                                                 ti.getPerfTime(round), tableMessage));
          violations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, ti.getTeamNumber(), null, null,
                                                 otherTime.getTime(), tableMessage));
        } else {
          tableSides.put(key, ti.getPerf(round));
        }

        /*
         * Not sure I like this check if
         * (!ComparisonUtils.safeEquals(ti.getDivision(),
         * opponent.getDivision())) { final String divMessage = String.format(
         * "Team %d in award group %s is competing against team %d from award group %s round %d"
         * , ti.getTeamNumber(), ti.getDivision(), opponent.getTeamNumber(),
         * opponent.getDivision(), (round + 1)); violations.add(new
         * ConstraintViolation(false, ti.getTeamNumber(), null, null,
         * ti.getPerfTime(round), divMessage)); }
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
                                               TournamentSchedule.formatTime(ti.getPerfTime(round)));
          violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, null,
                                                 ti.getPerfTime(round), message));
        } else {
          if (opponentSide == ti.getPerfTableSide(round)) {
            final String message = String.format("Team %d and team %d are both on table %s side %d at the same time for round %d",
                                                 ti.getTeamNumber(), opponent.getTeamNumber(),
                                                 ti.getPerfTableColor(round), ti.getPerfTableSide(round), (round
                                                     + 1));
            violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, null,
                                                   ti.getPerfTime(round), message));
          }
        }

        for (int r = round
            + 1; r < schedule.getNumberOfRounds(); ++r) {
          final TeamScheduleInfo otherOpponent = schedule.findOpponent(ti, r);
          if (otherOpponent != null
              && opponent.equals(otherOpponent)) {
            final String message = String.format("Team %d competes against %d more than once rounds: %d, %d",
                                                 ti.getTeamNumber(), opponent.getTeamNumber(), (round
                                                     + 1),
                                                 (r
                                                     + 1));
            violations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, ti.getTeamNumber(), null, null,
                                                   ti.getPerfTime(round), message));
            violations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, ti.getTeamNumber(), null, null,
                                                   ti.getPerfTime(r), message));
            violations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, opponent.getTeamNumber(), null, null,
                                                   ti.getPerfTime(round), message));
            violations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, opponent.getTeamNumber(), null, null,
                                                   ti.getPerfTime(r), message));
          }
        }
      } else {
        final String message = String.format("Team %d has no opponent for round %d", ti.getTeamNumber(), (round
            + 1));
        violations.add(new ConstraintViolation(ConstraintViolation.Type.SOFT, ti.getTeamNumber(), null, null,
                                               ti.getPerfTime(round), message));
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
      final int prevRound = round
          - 1;
      final Duration perfChangetime;
      final int prevRoundOpponentRound = schedule.findOpponentRound(ti, prevRound);
      final int curRoundOpponentRound = schedule.findOpponentRound(ti, round);
      if (prevRoundOpponentRound != prevRound
          || curRoundOpponentRound != round) {
        perfChangetime = getSpecialPerformanceChangetime();
      } else {
        perfChangetime = getPerformanceChangetime();
      }

      final LocalTime prevRoundStart = ti.getPerfTime(prevRound);
      final LocalTime prevRoundEnd = prevRoundStart.plus(getPerformanceDuration());
      final LocalTime roundStart = ti.getPerfTime(round);
      if (prevRoundEnd.isAfter(roundStart)) {
        final String message = String.format("Team %d is still in performance %d when they are to start performance %d: %s - %s",
                                             ti.getTeamNumber(), prevRound
                                                 + 1,
                                             round
                                                 + 1,
                                             TournamentSchedule.formatTime(ti.getPerfTime(prevRound)),
                                             TournamentSchedule.formatTime(ti.getPerfTime(round)));
        violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, null,
                                               ti.getPerfTime(round), message));
      } else if (prevRoundEnd.plus(perfChangetime).isAfter(roundStart)) {
        final String message = String.format("Team %d doesn't have enough time (%s) between performance %d and performance %d: %s - %s",
                                             ti.getTeamNumber(), perfChangetime.toString(), prevRound
                                                 + 1,
                                             round
                                                 + 1,
                                             TournamentSchedule.formatTime(ti.getPerfTime(prevRound)),
                                             TournamentSchedule.formatTime(ti.getPerfTime(round)));
        violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, null,
                                               ti.getPerfTime(round), message));
      }
    }
  }

  private void verifyPerformanceVsSubjective(final Collection<ConstraintViolation> violations,
                                             final TeamScheduleInfo ti,
                                             final String performanceName,
                                             final LocalTime performanceStart) {
    for (final SubjectiveTime subj : ti.getSubjectiveTimes()) {
      final LocalTime subjStart = subj.getTime();
      final LocalTime subjEnd = subjStart.plus(getSubjectiveDuration(subj.getName()));

      if (subjStart.isBefore(performanceStart)) {
        if (subjEnd.isAfter(performanceStart)) {
          final String message = String.format("Team %d will be in %s when performance round %s starts",
                                               ti.getTeamNumber(), subj.getName(), performanceName);
          violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, subj,
                                                 performanceStart, message));
        } else if (subjEnd.plus(getChangetime()).isAfter(performanceStart)) {
          final String message = String.format("Team %d has doesn't have enough time between %s and performance round %s (need %d)",
                                               ti.getTeamNumber(), TournamentSchedule.formatTime(subj.getTime()),
                                               performanceName, getChangetime().toMinutes());
          violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, subj,
                                                 performanceStart, message));
        }
      } else {
        final LocalTime performanceEnd = performanceStart.plus(getPerformanceDuration());
        if (performanceEnd.isAfter(subjStart)) {
          final String message = String.format("Team %d wil be in %s when performance round %s starts",
                                               ti.getTeamNumber(), subj.getName(), performanceName);
          violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, subj,
                                                 performanceStart, message));
        } else if (performanceEnd.plus(getChangetime()).isAfter(subjStart)) {
          final String message = String.format("Team %d has doesn't have enough time between %s and performance round %s (need %d)",
                                               ti.getTeamNumber(), subj.getName(), performanceName,
                                               getChangetime().toMinutes());
          violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, ti.getTeamNumber(), null, subj,
                                                 performanceStart, message));
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
    final Map<String, SortedSet<LocalTime>> tableToTime = new HashMap<>();
    // category -> judge -> times
    final Map<String, Map<String, SortedSet<LocalTime>>> subjectiveToTime = new HashMap<>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      final String judge = si.getJudgingGroup();
      for (final SubjectiveTime subj : si.getSubjectiveTimes()) {
        Map<String, SortedSet<LocalTime>> judges = subjectiveToTime.get(subj.getName());
        if (null == judges) {
          judges = new HashMap<>();
          subjectiveToTime.put(subj.getName(), judges);
        }
        SortedSet<LocalTime> times = judges.get(judge);
        if (null == times) {
          times = new TreeSet<LocalTime>();
          judges.put(judge, times);
        }
        times.add(subj.getTime());
      }

      for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
        final String table = si.getPerfTableColor(round);
        final int side = si.getPerfTableSide(round);
        final String tableKey = table
            + " "
            + side;
        SortedSet<LocalTime> performance = tableToTime.get(tableKey);
        if (null == performance) {
          performance = new TreeSet<>();
          tableToTime.put(tableKey, performance);
        }
        performance.add(si.getPerfTime(round));
      }
    }

    // find violations
    for (final Map.Entry<String, Map<String, SortedSet<LocalTime>>> categoryEntry : subjectiveToTime.entrySet()) {
      final String category = categoryEntry.getKey();
      final Map<String, SortedSet<LocalTime>> judgeMap = categoryEntry.getValue();
      for (final Map.Entry<String, SortedSet<LocalTime>> judgeEntry : judgeMap.entrySet()) {
        final String judge = judgeEntry.getKey();
        final SortedSet<LocalTime> times = judgeEntry.getValue();
        LocalTime prev = null;
        for (final LocalTime current : times) {
          if (null != prev) {
            if (prev.plus(getSubjectiveDuration(category)).isAfter(current)) {
              final String message = String.format("Overlap in %s for judge %s between %s and %s", category, judge,
                                                   TournamentSchedule.formatTime(prev),
                                                   TournamentSchedule.formatTime(current));
              violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, Team.NULL_TEAM_NUMBER,
                                                     new SubjectiveTime(category, prev), null, null, message));
            }
          }

          prev = current;
        }
      }
    }

    for (final Map.Entry<String, SortedSet<LocalTime>> entry : tableToTime.entrySet()) {
      LocalTime prev = null;
      for (final LocalTime current : entry.getValue()) {
        if (null != prev) {
          if (prev.plus(getPerformanceDuration()).isAfter(current)) {
            final String message = String.format("Overlap in performance for table %s between %s and %s",
                                                 entry.getKey(), TournamentSchedule.formatTime(prev),
                                                 TournamentSchedule.formatTime(current));
            violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, Team.NULL_TEAM_NUMBER, null, null,
                                                   prev, message));
          }
        }

        prev = current;
      }
    }

  }

  /**
   * Time to allocate for a performance run.
   * 
   * @return the performanceDuration
   */
  public Duration getPerformanceDuration() {
    return Duration.ofMinutes(params.getPerformanceMinutes());
  }

  /**
   * Time to allocate for either subjective judging.
   * 
   * @return the subjectiveDuration (milliseconds)
   */
  public Duration getSubjectiveDuration(final String name) {
    final SubjectiveStation station = params.getStationByName(name);
    return station.getDuration();
  }

  /**
   * This is the time required between events.
   * 
   * @return the changetime
   */
  public Duration getChangetime() {
    return Duration.ofMinutes(params.getChangetimeMinutes());
  }

  /**
   * The time required between performance runs.
   */
  public Duration getPerformanceChangetime() {
    return Duration.ofMinutes(params.getPerformanceChangetimeMinutes());
  }

  /**
   * This is the time required between performance runs for the two teams in
   * involved in the performance run that crosses round 1 and round 2 when there
   * is an odd number of teams.
   * 
   * @return the specialPerformanceChangetime
   */
  public Duration getSpecialPerformanceChangetime() {
    return specialPerformanceChangetime;
  }

  /**
   * @param specialPerformanceChangetime the specialPerformanceChangetime to set
   */
  public void setSpecialPerformanceChangetime(final Duration specialPerformanceChangetime) {
    this.specialPerformanceChangetime = specialPerformanceChangetime;
  }

}
