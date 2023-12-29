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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.scheduler.PerformanceTime;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.WebUtils;
import fll.web.playoff.Playoff;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Java code for scoreEntry/select_team.jsp.
 */
public final class SelectTeam {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private SelectTeam() {
  }

  /**
   * @param application get application variables
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
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

      // get latest performance round for each team
      final Map<Integer, Integer> maxRunNumbers = Collections.unmodifiableMap(getMaxRunNumbers(connection, tournament));

      final @Nullable TournamentSchedule schedule;
      if (TournamentSchedule.scheduleExistsInDatabase(connection, tournament.getTournamentID())) {
        schedule = new TournamentSchedule(connection, tournament.getTournamentID());
      } else {
        schedule = null;
      }

      final List<String> unfinishedBrackets = Collections.unmodifiableList(Playoff.getUnfinishedPlayoffBrackets(connection,
                                                                                                                tournament.getTournamentID()));

      final List<SelectTeamData> teamSelectData = Queries.getTournamentTeams(connection, tournament.getTournamentID())
                                                         .values().stream() //
                                                         .filter(team -> !team.isInternal()) //
                                                         .map(team -> teamToTeamSelectData(connection, tournament,
                                                                                           maxRunNumbers, schedule,
                                                                                           unfinishedBrackets, team)) //
                                                         .collect(Collectors.toList());

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      // assume that the string is going to be put inside single quotes in the
      // javascript code
      final String teamSelectDataJson = WebUtils.escapeStringForJsonParse(jsonMapper.writeValueAsString(teamSelectData));
      pageContext.setAttribute("teamSelectDataJson", teamSelectDataJson);

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    } catch (final JsonProcessingException e) {
      throw new FLLInternalException("Error converting data to JSON", e);
    }

  }

  private static SelectTeamData teamToTeamSelectData(Connection connection,
                                                     final Tournament tournament,
                                                     final Map<Integer, Integer> maxRunNumbers,
                                                     final @Nullable TournamentSchedule schedule,
                                                     final List<String> unfinishedBrackets,
                                                     final TournamentTeam team) {
    final int nextRunNumber = maxRunNumbers.getOrDefault(team.getTeamNumber(), 0)
        + 1;
    final @Nullable PerformanceTime nextPerformance = getNextPerformance(schedule, team.getTeamNumber(), nextRunNumber);
    if (null != nextPerformance) {
      return new SelectTeamData(team, nextPerformance, nextRunNumber);
    } else {

      final String nextTableAndSide = getNextTableAndSide(connection, tournament, team.getTeamNumber(), nextRunNumber);

      try {
        final String playoffBracketName = Playoff.getPlayoffBracketsForTeam(connection, team.getTeamNumber()).stream() //
                                                 .filter(b -> unfinishedBrackets.contains(b)) //
                                                 .findFirst() //
                                                 .orElse("");
        final String runNumberDisplay;
        if (!playoffBracketName.isEmpty()) {
          final int playoffRound = Playoff.getPlayoffRound(connection, tournament.getTournamentID(), playoffBracketName,
                                                           nextRunNumber);

          final int playoffMatch = Playoff.getPlayoffMatch(connection, tournament.getTournamentID(), playoffBracketName,
                                                           nextRunNumber);
          runNumberDisplay = String.format("%s P%d M%d", playoffBracketName, playoffRound, playoffMatch);
        } else {
          runNumberDisplay = String.valueOf(nextRunNumber);
        }
        return new SelectTeamData(team, nextTableAndSide, nextRunNumber, runNumberDisplay);
      } catch (final SQLException e) {
        LOGGER.warn("Got error retrieving playoff bracket information for team {} run {}", team.getTeamNumber(),
                    nextRunNumber, e);
        return new SelectTeamData(team, nextTableAndSide, nextRunNumber, String.valueOf(nextRunNumber));
      }
    }
  }

  /**
   * @return the next table and side or the empty string if this cannot be
   *         determined.
   */
  private static String getNextTableAndSide(final Connection connection,
                                            final Tournament tournament,
                                            final int teamNumber,
                                            final int nextRunNumber) {
    try {
      if (nextRunNumber <= TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID())) {
        // no schedule, not going to know the table
        return "";
      } else {
        // get information from the playoffs
        return Playoff.getTableForRun(connection, tournament, teamNumber, nextRunNumber);
      }
    } catch (final SQLException e) {
      LOGGER.warn("Got error retrieving next table and side for team {} run {}", teamNumber, nextRunNumber, e);
      return "";
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
   * Data class for displaying team information on the web. Sorting is done inside
   * the javascript.
   */
  public static final class SelectTeamData {

    private final TournamentTeam team;

    private final @Nullable PerformanceTime nextPerformance;

    private final int nextRunNumber;

    private final String nextTableAndSide;

    private final String runNumberDisplay;

    SelectTeamData(final TournamentTeam team,
                   final PerformanceTime nextPerformance,
                   final int nextRunNumber) {
      this.team = team;
      this.nextPerformance = nextPerformance;
      this.nextRunNumber = nextRunNumber;
      this.nextTableAndSide = nextPerformance.getTableAndSide();
      this.runNumberDisplay = String.valueOf(nextRunNumber);
    }

    SelectTeamData(final TournamentTeam team,
                   final String nextTableAndSide,
                   final int nextRunNumber,
                   final String runNumberDisplay) {
      this.team = team;
      this.nextPerformance = null;
      this.nextRunNumber = nextRunNumber;
      this.nextTableAndSide = nextTableAndSide;
      this.runNumberDisplay = runNumberDisplay;
    }

    /**
     * @return what to display in the team selection box
     */
    public String getDisplayString() {
      final String scheduleInfo;
      if (null != nextPerformance) {
        scheduleInfo = String.format(" @ %s", TournamentSchedule.formatTime(nextPerformance.getTime()));
      } else {
        scheduleInfo = "";
      }
      final String tableInfo;
      if (nextTableAndSide.isEmpty()) {
        tableInfo = "";
      } else {
        tableInfo = String.format(" - %s", nextTableAndSide);
      }
      return String.format("%d [%s] - %s%s%s", team.getTeamNumber(), team.getTrimmedTeamName(), runNumberDisplay,
                           scheduleInfo, tableInfo);
    }

    /**
     * @return team
     */
    public TournamentTeam getTeam() {
      return team;
    }

    /**
     * @return the next performance
     */
    public @Nullable PerformanceTime getNextPerformance() {
      return nextPerformance;
    }

    /**
     * @return next run number
     */
    public int getNextRunNumber() {
      return nextRunNumber;
    }

    /**
     * @return the table and side the team is performing on next, the empty string
     *         if the table isn't known
     */
    public String getNextTableAndSide() {
      return nextTableAndSide;
    }
  }

}
