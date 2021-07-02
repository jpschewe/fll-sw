/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.scheduler.PerformanceTime;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.web.ApplicationAttributes;

/**
 * Java code for scoreEntry/select_team.jsp.
 */
public final class SelectTeam {

  private SelectTeam() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param application get application variables
   * @param pageContext set page variables
   * @param session session variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?")) {
        prep.setInt(1, Queries.getCurrentTournament(connection));
        try (ResultSet rs = prep.executeQuery()) {
          final int maxRunNumber;
          if (rs.next()) {
            maxRunNumber = rs.getInt(1);
          } else {
            maxRunNumber = 1;
          }
          pageContext.setAttribute("maxRunNumber", maxRunNumber);
        } // result set
      } // prepared statement

      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final List<TournamentTeam> tournamentTeams = new LinkedList<>(Queries.getTournamentTeams(connection,
                                                                                               tournament.getTournamentID())
                                                                           .values());

      final @Nullable String scoreEntrySelectedTable = (String) session.getAttribute("scoreEntrySelectedTable");

      if (TournamentSchedule.scheduleExistsInDatabase(connection, tournament.getTournamentID())) {
        // sort teams based on the schedule

        final TournamentSchedule schedule = new TournamentSchedule(connection, tournament.getTournamentID());

        // get latest performance round for each team
        final Map<Integer, Integer> maxRunNumbers = getMaxRunNumbers(connection, tournament);

        final Comparator<TournamentTeam> comparator = new TeamSort(scoreEntrySelectedTable, schedule, maxRunNumbers);
        tournamentTeams.sort(comparator);
      }

      pageContext.setAttribute("tournamentTeams", tournamentTeams);

    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  /**
   * Find the maximum run number all teams. Does not ignore unverified
   * scores. If a team is not in the result, then the next run number is 1.
   * 
   * @param connection database connection
   * @param tournament the tournament to check
   * @return the maximum run number recorded for each team
   * @throws SQLException on a database error
   */
  private static Map<Integer, Integer> getMaxRunNumbers(final Connection connection,
                                                        final Tournament tournament)
      throws SQLException {

    final Map<Integer, Integer> maxRunNumbers = new HashMap<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber, COUNT(TeamNumber) as max_run FROM Performance"
            + " WHERE Tournament = ?"
            + " GROUP BY TeamNumber")) {
      prep.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt("TeamNumber");
          final int runNumber = rs.getInt("max_run");
          maxRunNumbers.put(teamNumber, runNumber);
        }
      } // result set
    } // prepared statement

    return maxRunNumbers;
  }

  @SuppressFBWarnings(value = "SE_COMPARATOR_SHOULD_BE_SERIALIZABLE", justification = "only used for sort, not stored")
  private static final class TeamSort implements Comparator<TournamentTeam> {
    private final Map<Integer, Integer> maxRunNumbers;

    private final @Nullable String scoreEntrySelectedTable;

    private final @Nullable TournamentSchedule schedule;

    TeamSort(final @Nullable String scoreEntrySelectedTable,
             final @Nullable TournamentSchedule schedule,
             final Map<Integer, Integer> maxRunNumbers) {
      this.maxRunNumbers = maxRunNumbers;
      this.schedule = schedule;
      this.scoreEntrySelectedTable = scoreEntrySelectedTable;
    }

    @Override
    public int compare(final TournamentTeam one,
                       final TournamentTeam two) {
      final int oneNextRun = maxRunNumbers.getOrDefault(one.getTeamNumber(), 0)
          + 1;
      final int twoNextRun = maxRunNumbers.getOrDefault(two.getTeamNumber(), 0)
          + 1;

      if (null == schedule) {
        return Integer.compare(oneNextRun, twoNextRun);
      } else {
        final @Nullable TeamScheduleInfo oneSched = schedule.getSchedInfoForTeam(one.getTeamNumber());
        final @Nullable TeamScheduleInfo twoSched = schedule.getSchedInfoForTeam(two.getTeamNumber());
        if (null == oneSched
            && null == twoSched) {
          return Integer.compare(oneNextRun, twoNextRun);
        } else if (null == oneSched) {
          // one is after two
          return 1;
        } else if (null == twoSched) {
          // one is before two
          return -1;
        } else {
          // subtract 1 from the run number to get to a zero based index
          final @Nullable PerformanceTime oneTime = oneSched.enumerateRegularMatchPlayPerformances()//
                                                            .filter(p -> p.getRight().longValue() == oneNextRun
                                                                - 1)//
                                                            .findFirst().map(Pair::getLeft).orElse(null);

          final @Nullable PerformanceTime twoTime = twoSched.enumerateRegularMatchPlayPerformances()//
                                                            .filter(p -> p.getRight().longValue() == twoNextRun
                                                                - 1)//
                                                            .findFirst().map(Pair::getLeft).orElse(null);

          if (null == oneTime
              && null == twoTime) {
            return Integer.compare(oneNextRun, twoNextRun);
          } else if (null == oneTime) {
            // one doesn't have anymore runs left, it is after two
            return 1;
          } else if (null == twoTime) {
            // two doesn't have anymore runs left, it is after one
            return -1;
          } else {
            final String oneTable = oneTime.getTableAndSide();
            final String twoTable = twoTime.getTableAndSide();

            if (oneTable.equals(scoreEntrySelectedTable)
                && !twoTable.equals(scoreEntrySelectedTable)) {
              // prefer selected table
              return -1;
            } else if (!oneTable.equals(scoreEntrySelectedTable)
                && twoTable.equals(scoreEntrySelectedTable)) {
              // prefer selected table
              return 1;
            } else if (oneTime.equals(twoTime)) {
              return Integer.compare(one.getTeamNumber(), two.getTeamNumber());
            } else {
              return oneTime.compareTo(twoTime);
            }
          }
        }
      }

    }

  }
}
