/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.EOFException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
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
import fll.web.GetHttpSessionConfigurator;
import fll.web.playoff.Playoff;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import net.mtu.eggplant.util.StringUtils;

/**
 * Notify the select team page of a change in the list of performance runs.
 */
@ServerEndpoint(value = "/scoreEntry/PerformanceRunsEndpoint", configurator = GetHttpSessionConfigurator.class)
public class PerformanceRunsEndpoint {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final ConcurrentHashMap<String, Session> ALL_SESSIONS = new ConcurrentHashMap<>();

  private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

  private @MonotonicNonNull Session session;

  private @MonotonicNonNull String uuid;

  /**
   * @param session the newly opened session
   */
  @OnOpen
  public void onOpen(final Session session) {
    try {
      final HttpSession httpSession = GetHttpSessionConfigurator.getHttpSession(session);
      final ServletContext application = httpSession.getServletContext();
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {
        final Collection<UnverifiedRunData> unverifiedData = getUnverifiedRunData(connection);
        final List<SelectTeamData> teamSelectData = getTeamSelectData(connection);
        final PerformanceRunData runData = new PerformanceRunData(teamSelectData, unverifiedData);
        final ObjectMapper jsonMapper = Utilities.createJsonMapper();
        final String msg = jsonMapper.writeValueAsString(runData);
        sendToClient(session, msg);
      } catch (final SQLException e) {
        throw new FLLRuntimeException("Error getting peprformance run data to send to client", e);
      }

      this.uuid = UUID.randomUUID().toString();
      this.session = session;
      ALL_SESSIONS.put(uuid, session);
    } catch (final IOException e) {
      LOGGER.warn("Got error sending intial message to client, not adding", e);
    }
  }

  @OnClose
  public void onClose() {
    if (null != uuid) {
      internalRemoveSession(uuid);
    }
  }

  /**
   * Error handler.
   * 
   * @param t the exception
   */
  @OnError
  public void error(final Throwable t) {
    if (t instanceof EOFException) {
      LOGGER.debug("{}: Socket closed.", uuid);
    } else {
      LOGGER.error("{}: websocket error", uuid, t);
      if (null != uuid) {
        internalRemoveSession(uuid);
      }
    }
  }

  /**
   * Close the session and remove it from the global list.
   */
  private static void internalRemoveSession(final String uuid) {
    final @Nullable Session client = ALL_SESSIONS.remove(uuid);
    if (null != client) {
      try {
        client.close();
      } catch (final IOException e) {
        LOGGER.debug("Got error closing websocket, ignoring", e);
      }
    }
  }

  /**
   * Notify all clients of changes to the performance run data.
   * 
   * @param connection database connection to retrieve data
   * @throws SQLException on a database error
   */
  public static void notifyToUpdate(Connection connection) throws SQLException {
    final Collection<UnverifiedRunData> unverifiedData = getUnverifiedRunData(connection);
    final List<SelectTeamData> teamSelectData = getTeamSelectData(connection);
    sendData(teamSelectData, unverifiedData);
  }

  private static void sendData(final List<SelectTeamData> teamSelectData,
                               final Collection<UnverifiedRunData> unverifiedData) {
    THREAD_POOL.execute(() -> {
      try {
        final PerformanceRunData runData = new PerformanceRunData(teamSelectData, unverifiedData);
        final ObjectMapper jsonMapper = Utilities.createJsonMapper();
        final String msg = jsonMapper.writeValueAsString(runData);

        final Set<String> toRemove = new HashSet<>();

        for (final Map.Entry<String, Session> entry : ALL_SESSIONS.entrySet()) {
          final Session session = entry.getValue();
          try {
            sendToClient(session, msg);
          } catch (final IOException e) {
            LOGGER.warn("Error sending to client: "
                + e.getMessage(), e);
            toRemove.add(entry.getKey());
          }
        } // foreach session

        // cleanup dead sessions
        for (final String uuid : toRemove) {
          internalRemoveSession(uuid);
        }
      } catch (final JsonProcessingException e) {
        throw new FLLInternalException("Error converting message to JSON", e);
      }
    });
  }

  private static void sendToClient(final Session client,
                                   final String message)
      throws IOException {
    if (!client.isOpen()) {
      throw new IOException("Client has closd the connection");
    }
    try {
      client.getBasicRemote().sendText(message);
    } catch (final IllegalStateException e) {
      throw new IOException("Illegal state exception writing to client", e);
    }
  }

  private static List<SelectTeamData> getTeamSelectData(final Connection connection) throws SQLException {
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

    final List<PerformanceRunsEndpoint.SelectTeamData> teamSelectData = Queries.getTournamentTeams(connection,
                                                                                                   tournament.getTournamentID())
                                                                               .values().stream() //
                                                                               .filter(team -> !team.isInternal()) //
                                                                               .map(team -> teamToTeamSelectData(connection,
                                                                                                                 tournament,
                                                                                                                 maxRunNumbers,
                                                                                                                 schedule,
                                                                                                                 unfinishedBrackets,
                                                                                                                 team)) //
                                                                               .collect(Collectors.toList());

    return teamSelectData;
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

  private static PerformanceRunsEndpoint.SelectTeamData teamToTeamSelectData(Connection connection,
                                                                             final Tournament tournament,
                                                                             final Map<Integer, Integer> maxRunNumbers,
                                                                             final @Nullable TournamentSchedule schedule,
                                                                             final List<String> unfinishedBrackets,
                                                                             final TournamentTeam team) {
    final int nextRunNumber = maxRunNumbers.getOrDefault(team.getTeamNumber(), 0)
        + 1;
    final @Nullable PerformanceTime nextPerformance = getNextPerformance(schedule, team.getTeamNumber(), nextRunNumber);
    if (null != nextPerformance) {
      return new PerformanceRunsEndpoint.SelectTeamData(team, nextPerformance, nextRunNumber);
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
        return new PerformanceRunsEndpoint.SelectTeamData(team, nextTableAndSide, nextRunNumber, runNumberDisplay);
      } catch (final SQLException e) {
        LOGGER.warn("Got error retrieving playoff bracket information for team {} run {}", team.getTeamNumber(),
                    nextRunNumber, e);
        return new PerformanceRunsEndpoint.SelectTeamData(team, nextTableAndSide, nextRunNumber,
                                                          String.valueOf(nextRunNumber));
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

  private static Collection<UnverifiedRunData> getUnverifiedRunData(final Connection connection) throws SQLException {
    final Collection<UnverifiedRunData> data = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT"
        + "     Performance.TeamNumber"
        + "    ,Performance.RunNumber"
        + "    ,Teams.TeamName"
        + "     FROM Performance, Teams"
        + "     WHERE Verified != TRUE"
        + "       AND Tournament = ?"
        + "       AND Teams.TeamNumber = Performance.TeamNumber"
        + "       ORDER BY Performance.RunNumber, Teams.TeamNumber")) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final List<String> unfinishedBrackets = Playoff.getUnfinishedPlayoffBrackets(connection,
                                                                                   tournament.getTournamentID());

      prep.setInt(1, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final int runNumber = rs.getInt(2);
          final String name = castNonNull(rs.getString(3));
          final String trimmedName = StringUtils.trimString(name, Team.MAX_TEAM_NAME_LEN);
          final String escapedName = StringEscapeUtils.escapeEcmaScript(trimmedName);

          final String playoffBracketName = Playoff.getPlayoffBracketsForTeam(connection, teamNumber).stream() //
                                                   .filter(b -> unfinishedBrackets.contains(b)) //
                                                   .findFirst() //
                                                   .orElse("");

          final String runNumberDisplay;
          if (!playoffBracketName.isEmpty()) {
            final int playoffRound = Playoff.getPlayoffRound(connection, tournament.getTournamentID(),
                                                             playoffBracketName, runNumber);
            runNumberDisplay = String.format("%s P%d", playoffBracketName, playoffRound);
          } else {
            runNumberDisplay = String.valueOf(runNumber);
          }

          final String displayString;
          displayString = String.format("Run %s - %d [%s]", runNumberDisplay, teamNumber, escapedName);

          final UnverifiedRunData d = new UnverifiedRunData(teamNumber, runNumber, displayString);
          data.add(d);
        } // foreach result
      }
    }

    return data;
  }

  /**
   * Data class for all data sent.
   */
  public static final class PerformanceRunData {

    /**
     * @param teamSelectData {@link #getTeamSelectData()}
     * @param unverified {@link #getUnverified()}
     */
    public PerformanceRunData(final List<SelectTeamData> teamSelectData,
                              final Collection<UnverifiedRunData> unverified) {
      this.teamSelectData = teamSelectData;
      this.unverified = unverified;
    }

    private final Collection<UnverifiedRunData> unverified;

    /**
     * @return the unverified runs data
     */
    public Collection<UnverifiedRunData> getUnverified() {
      return unverified;
    }

    private final List<SelectTeamData> teamSelectData;

    /**
     * @return information about the next runs for each team
     */
    public List<SelectTeamData> getTeamSelectData() {
      return teamSelectData;
    }

  }

  /**
   * Data class for unverified run information.
   */
  public static final class UnverifiedRunData {

    private final int teamNumber;

    private final int runNumber;

    private final String display;

    UnverifiedRunData(final int teamNumber,
                      final int runNumber,
                      final String display) {
      this.teamNumber = teamNumber;
      this.runNumber = runNumber;
      this.display = display;
    }

    /**
     * @return team number
     */
    public int getTeamNumber() {
      return teamNumber;
    }

    /**
     * @return run number
     */
    public int getRunNumber() {
      return runNumber;
    }

    /**
     * @return what to display to the user
     */
    public String getDisplay() {
      return display;
    }
  }

  /**
   * Data class for displaying team information on the web.
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
