/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.util.FLLInternalException;
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

  private static final @GuardedBy("SESSIONS_LOCK") Set<Session> ALL_SESSIONS = new HashSet<>();

  private static final Object SESSIONS_LOCK = new Object();

  private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

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
        final PerformanceRunData runData = new PerformanceRunData(unverifiedData);
        final ObjectMapper jsonMapper = Utilities.createJsonMapper();
        final String msg = jsonMapper.writeValueAsString(runData);
        sendToClient(session, msg);
      } catch (final SQLException e) {
        LOGGER.error("Error getting peprformance run data to send to client", e);
      }

      synchronized (SESSIONS_LOCK) {
        ALL_SESSIONS.add(session);
      }
    } catch (final IOException e) {
      LOGGER.warn("Got error sending intial message to client, not adding", e);
    }
  }

  /**
   * @param session the session for the closed websocket
   */
  @OnClose
  public void onClose(final Session session) {
    synchronized (SESSIONS_LOCK) {
      internalRemoveSession(session);
    }
  }

  /**
   * Error handler.
   * 
   * @param session the session that had the error
   * @param t the exception
   */
  @OnError
  public void error(final Session session,
                    final Throwable t) {
    synchronized (SESSIONS_LOCK) {
      LOGGER.error("Caught websocket error, closing session", t);

      internalRemoveSession(session);
    }
  }

  /**
   * Close the session and remove it from the global list without locking.
   */
  private static void internalRemoveSession(final Session session) {
    try {
      session.close();
    } catch (final IOException ioe) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got error closing session, ignoring", ioe);
      }
    }

    ALL_SESSIONS.remove(session);
  }

  /**
   * Notify all clients of changes to the performance run data.
   * 
   * @param connection database connection to retrieve data
   * @throws SQLException on a database error
   */
  public static void notifyToUpdate(Connection connection) throws SQLException {
    final Collection<UnverifiedRunData> unverifiedData = getUnverifiedRunData(connection);
    sendData(unverifiedData);
  }

  // FIXME modify client side to clear the unverified options and create new ones
  // based on the data
  private static void sendData(final Collection<UnverifiedRunData> unverifiedData) {
    THREAD_POOL.execute(() -> {
      try {
        final PerformanceRunData runData = new PerformanceRunData(unverifiedData);
        final ObjectMapper jsonMapper = Utilities.createJsonMapper();
        final String msg = jsonMapper.writeValueAsString(runData);

        synchronized (SESSIONS_LOCK) {
          final Set<Session> toRemove = new HashSet<>();

          for (final Session session : ALL_SESSIONS) {
            try {
              sendToClient(session, msg);
            } catch (final IOException e) {
              LOGGER.warn("Error sending to client: "
                  + e.getMessage(), e);
              toRemove.add(session);
            }
          } // foreach session

          // cleanup dead sessions
          for (final Session session : toRemove) {
            internalRemoveSession(session);
          }
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
     * @param unverified {@link #getUnverified()}
     */
    public PerformanceRunData(final Collection<UnverifiedRunData> unverified) {
      this.unverified = unverified;
    }

    private final Collection<UnverifiedRunData> unverified;

    /**
     * @return the unverified runs data
     */
    public Collection<UnverifiedRunData> getUnverified() {
      return unverified;
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
}
