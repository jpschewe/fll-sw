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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

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

      final @Nullable String scoreEntrySelectedTable = (String) session.getAttribute("scoreEntrySelectedTable");

      // get latest performance round for each team
      final Map<Integer, Integer> maxRunNumbers = getMaxRunNumbers(connection, tournament);

      final @Nullable TournamentSchedule schedule;
      if (TournamentSchedule.scheduleExistsInDatabase(connection, tournament.getTournamentID())) {
        schedule = new TournamentSchedule(connection, tournament.getTournamentID());
      } else {
        schedule = null;
      }

      final List<SelectTeamData> teamSelectData = Queries.getTournamentTeams(connection, tournament.getTournamentID())
                                                         .values().stream() //
                                                         .map(team -> {
                                                           final int nextRunNumber = maxRunNumbers.getOrDefault(team.getTeamNumber(),
                                                                                                                0)
                                                               + 1;
                                                           final @Nullable PerformanceTime nextPerformance = getNextPerformance(schedule,
                                                                                                                                team.getTeamNumber(),
                                                                                                                                nextRunNumber);
                                                           return new SelectTeamData(scoreEntrySelectedTable, team,
                                                                                     nextPerformance, nextRunNumber);
                                                         }) //
                                                         .collect(Collectors.toList());
      Collections.sort(teamSelectData);
      pageContext.setAttribute("teamSelectData", teamSelectData);

    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  private static @Nullable PerformanceTime getNextPerformance(final @Nullable TournamentSchedule schedule,
                                                              final int teamNumber,
                                                              final int nextRunNumber) {
    if (null == schedule) {
      return null;
    } else {
      final @Nullable TeamScheduleInfo sched = schedule.getSchedInfoForTeam(teamNumber);
      if (null == sched) {
        return null;
      } else {
        // subtract 1 from the run number to get to a zero based index
        final @Nullable PerformanceTime time = sched.enumerateRegularMatchPlayPerformances()//
                                                    .filter(p -> p.getRight().longValue() == (nextRunNumber
                                                        - 1))//
                                                    .findFirst().map(Pair::getLeft).orElse(null);

        return time;
      }
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

  /**
   * Data class for displaying team information on the web.
   */
  @SuppressFBWarnings(value = "SE_COMPARATOR_SHOULD_BE_SERIALIZABLE", justification = "only used for sort, not stored")
  public static final class SelectTeamData implements Comparable<SelectTeamData> {

    private final TournamentTeam team;

    private final @Nullable PerformanceTime nextPerformance;

    private final int nextRunNumber;

    private final @Nullable String scoreEntrySelectedTable;

    SelectTeamData(final @Nullable String scoreEntrySelectedTable,
                   final TournamentTeam team,
                   final @Nullable PerformanceTime nextPerformance,
                   final int nextRunNumber) {
      this.scoreEntrySelectedTable = scoreEntrySelectedTable;
      this.team = team;
      this.nextPerformance = nextPerformance;
      this.nextRunNumber = nextRunNumber;
    }

    /**
     * @return what to display in the team selection box
     */
    public String getDisplayString() {
      final String scheduleInfo;
      if (null != nextPerformance) {
        scheduleInfo = String.format(" @ %s - %s", TournamentSchedule.formatTime(nextPerformance.getTime()),
                                     nextPerformance.getTableAndSide());
      } else {
        scheduleInfo = "";
      }
      return String.format("%d [%s] - %d%s", team.getTeamNumber(), team.getTrimmedTeamName(), nextRunNumber,
                           scheduleInfo);
    }

    /**
     * @return team
     */
    public TournamentTeam getTeam() {
      return team;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.nextPerformance, this.nextRunNumber);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      } else if (null == o) {
        return false;
      } else if (this.getClass().equals(o.getClass())) {
        final SelectTeamData other = (SelectTeamData) o;
        return 0 == this.compareTo(other);
      } else {
        return false;
      }
    }

    @Override
    public int compareTo(final SelectTeamData other) {
      if (null == this.nextPerformance
          && null == other.nextPerformance) {
        return Integer.compare(this.nextRunNumber, other.nextRunNumber);
      } else if (null == this.nextPerformance) {
        // this is after other
        return 1;
      } else if (null == other.nextPerformance) {
        // this is before other
        return -1;
      } else {
        final String oneTable = this.nextPerformance.getTableAndSide();
        final String twoTable = other.nextPerformance.getTableAndSide();

        if (oneTable.equals(scoreEntrySelectedTable)
            && !twoTable.equals(scoreEntrySelectedTable)) {
          // prefer selected table
          return -1;
        } else if (!oneTable.equals(scoreEntrySelectedTable)
            && twoTable.equals(scoreEntrySelectedTable)) {
          // prefer selected table
          return 1;
        } else if (this.nextPerformance.equals(other.nextPerformance)) {
          return Integer.compare(this.team.getTeamNumber(), other.team.getTeamNumber());
        } else {
          return this.nextPerformance.compareTo(other.nextPerformance);
        }
      }
    }
  }

}
