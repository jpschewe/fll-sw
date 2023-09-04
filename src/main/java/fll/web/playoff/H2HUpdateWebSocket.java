/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
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

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;
import fll.web.GetHttpSessionConfigurator;
import fll.web.display.DisplayHandler;
import fll.xml.ScoreType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Provide updates on the state of head to head brackets.
 */
@ServerEndpoint(value = "/playoff/H2HUpdateWebSocket", configurator = GetHttpSessionConfigurator.class)
public class H2HUpdateWebSocket {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  // bracketname -> display uuids
  private static final Map<String, Set<String>> SESSIONS = new HashMap<>();

  // uuid -> session
  private static final Map<String, Session> ALL_SESSIONS = new HashMap<>();

  private static final Object SESSIONS_LOCK = new Object();

  /**
   * Add the session and send out messages for all bracket information for the
   * specified brackets.
   * 
   * @param displayUuid the UUID of the display
   * @param session the session to add
   * @param allBracketInfo the brackets that the session is interested in
   * @throws IOException
   * @throws SQLException
   */
  private static void addSession(final String displayUuid,
                                 final Session session,
                                 final Collection<BracketInfo> allBracketInfo,
                                 final Connection connection,
                                 final int currentTournament)
      throws IOException, SQLException {
    synchronized (SESSIONS_LOCK) {
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      ALL_SESSIONS.put(displayUuid, session);

      updateDisplayedBracket(displayUuid, session);

      for (final BracketInfo bracketInfo : allBracketInfo) {

        if (!SESSIONS.containsKey(bracketInfo.getBracketName())) {
          SESSIONS.put(bracketInfo.getBracketName(), new HashSet<String>());
        }

        SESSIONS.get(bracketInfo.getBracketName()).add(displayUuid);

        // send the current information for the bracket to the session so that
        // it's current
        final Collection<BracketUpdate> updates = Queries.getH2HBracketData(connection, currentTournament,
                                                                            bracketInfo.getBracketName(),
                                                                            bracketInfo.getFirstRound(),
                                                                            bracketInfo.getLastRound());
        for (final BracketUpdate update : updates) {

          final BracketMessage message = new BracketMessage();
          message.isBracketUpdate = true;
          message.bracketUpdate = update;

          final StringWriter writer = new StringWriter();
          try {
            jsonMapper.writeValue(writer, message);
          } catch (final IOException e) {
            throw new FLLInternalException("Error writing JSON for brackets", e);
          }
          session.getBasicRemote().sendText(writer.toString());
        } // foreach update

      }
    }
  }

  /**
   * @param session the session
   * @param msg the message received
   */
  @OnMessage
  public void receiveTextMessage(final Session session,
                                 final String msg) {
    LOGGER.trace("Received message '{}'", msg);

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    // may get passed a javascript object with extra fields, just ignore them
    // this happens when BracketInfo is subclassed and the subclass is passed
    // in
    jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      final RegisterMessage message = jsonMapper.readValue(msg, RegisterMessage.class);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Display UUID: '{}'", message.uuid);
        for (final BracketInfo bracketInfo : message.brackInfo) {
          LOGGER.trace("Bracket name: {} first: {} last: {}", bracketInfo.getBracketName(), bracketInfo.getFirstRound(),
                       bracketInfo.getLastRound());
        }
      }

      final String uuid;
      if (!StringUtils.isBlank(message.uuid)) {
        uuid = message.uuid;
      } else {
        uuid = UUID.randomUUID().toString();
      }

      final HttpSession httpSession = getHttpSession(session);
      final ServletContext httpApplication = httpSession.getServletContext();
      final DataSource datasource = ApplicationAttributes.getDataSource(httpApplication);
      try (Connection connection = datasource.getConnection()) {
        final int currentTournament = Queries.getCurrentTournament(connection);
        addSession(uuid, session, message.brackInfo, connection, currentTournament);
      }

    } catch (final IOException e) {
      throw new FLLRuntimeException("Error parsing bracket info from #"
          + msg
          + "#", e);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error getting playoff data from the database", e);
    }
  }

  private static HttpSession getHttpSession(final Session session) {
    final HttpSession httpSession = (HttpSession) session.getUserProperties()
                                                         .get(GetHttpSessionConfigurator.HTTP_SESSION_KEY);

    if (null == httpSession) {
      throw new FLLRuntimeException("Unable to find httpSession in the userProperties");
    }

    return httpSession;
  }

  private static void updateDisplayedBracket(final String displayUuid,
                                             final Session session) {

    if (session.isOpen()) {

      try {
        final DisplayInfo displayInfo = DisplayHandler.resolveDisplay(displayUuid);

        final BracketMessage message = new BracketMessage();
        message.isDisplayUpdate = true;

        for (final DisplayInfo.H2HBracketDisplay h2hBracket : displayInfo.getBrackets()) {
          final BracketInfo bracketInfo = new BracketInfo(h2hBracket.getBracket(), h2hBracket.getFirstRound(),
                                                          h2hBracket.getFirstRound()
                                                              + 2);

          message.allBracketInfo.add(bracketInfo);
        } // foreach h2h bracket

        // expose all bracketInfo to the javascript
        final ObjectMapper jsonMapper = Utilities.createJsonMapper();
        try (StringWriter writer = new StringWriter()) {
          jsonMapper.writeValue(writer, message);
          final String allBracketInfoJson = writer.toString();

          session.getBasicRemote().sendText(allBracketInfoJson);
        } catch (final IOException e) {
          throw new FLLInternalException("Error writing JSON for allBracketInfo", e);
        }

      } catch (final IllegalStateException e) {
        LOGGER.error("Got an illegal state exception, likely from an invalid HttpSession object, skipping update of session: "
            + session.getId()
            + " websocket session: "
            + session.getId()
            + " open: "
            + session.isOpen(), e);
      }
    } // open session
  }

  /**
   * Send each display the most recent bracket information to show.
   */
  public static void updateDisplayedBracket() {
    synchronized (SESSIONS_LOCK) {
      for (final Map.Entry<String, Session> entry : ALL_SESSIONS.entrySet()) {
        updateDisplayedBracket(entry.getKey(), entry.getValue());
      } // foreach session

    } // sessions lock

  }

  /**
   * Update the head to head display.
   * 
   * @param bracketName the bracket being updated
   * @param dbLine the line in the database
   * @param playoffRound the playoff round
   * @param maxPlayoffRound the last playoff round for the bracket
   * @param teamNumber the team that is at the specified row and playoff round,
   *          may be null
   * @param teamName team name, may be null
   * @param score the score for the team, may be null
   * @param performanceScoreType used to format the score
   * @param table the table for the team, may be null
   */
  private static void updateBracket(final String bracketName,
                                    final int dbLine,
                                    final int playoffRound,
                                    final int maxPlayoffRound,
                                    final @Nullable Integer teamNumber,
                                    final @Nullable String teamName,
                                    final @Nullable Double score,
                                    final ScoreType performanceScoreType,
                                    final boolean noShow,
                                    final boolean verified,
                                    final @Nullable String table) {

    LOGGER.trace("Sending H2H update team: {} bracket: {} dbLine: {} playoffRound: {} score: {} table: {}", teamNumber,
                 bracketName, dbLine, playoffRound, score, table);

    final BracketMessage message = new BracketMessage();
    message.isBracketUpdate = true;
    message.bracketUpdate = new BracketUpdate(bracketName, dbLine, playoffRound, maxPlayoffRound, teamNumber, teamName,
                                              score, performanceScoreType, noShow, verified, table);

    synchronized (SESSIONS_LOCK) {
      if (!SESSIONS.containsKey(bracketName)) {
        return;
      }

      final Set<String> toRemove = new HashSet<>();

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      final StringWriter writer = new StringWriter();

      try {
        jsonMapper.writeValue(writer, message);
      } catch (final IOException e) {
        throw new FLLInternalException("Error writing JSON for brackets", e);
      }
      final String messageText = writer.toString();

      final Set<String> uuids = SESSIONS.get(bracketName);
      for (final String uuid : uuids) {
        final Session session = ALL_SESSIONS.get(uuid);
        if (null == session) {
          LOGGER.warn("Found invalid uuid in sessions: {}", uuid);
          continue;
        }

        if (session.isOpen()) {
          try {
            session.getBasicRemote().sendText(messageText);
          } catch (final IOException ioe) {
            LOGGER.error("Got error sending message to session ("
                + session.getId()
                + "), dropping session", ioe);
            toRemove.add(uuid);
          }
        } else {
          toRemove.add(uuid);
        }
      } // foreach session

      // cleanup dead sessions
      for (final String uuid : toRemove) {
        final Session session = ALL_SESSIONS.get(uuid);
        if (null != session) {
          try {
            session.close();
          } catch (final IOException ioe) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Got error closing session, ignoring", ioe);
            }
          }
        }
        uuids.remove(uuid);
        ALL_SESSIONS.remove(uuid);
      } // foreach session to remove

    } // end lock

  }

  /**
   * @param session session that has an error
   * @param t the exception
   */
  @OnError
  public void error(@SuppressWarnings("unused") final Session session,
                    final Throwable t) {
    LOGGER.error("Caught websocket error", t);
  }

  /**
   * Update a particular bracket.
   * This cannot be used with internal teams as the playoff line cannot be
   * reliably determined for internal teams.
   * 
   * @param connection the database connection
   * @param performanceScoreType used to determine how to convert the score to a
   *          string
   * @param headToHeadBracket the bracket name to look at
   * @param team the team
   * @param performanceRunNumber the performance run number
   * @throws SQLException if there is a problem talking to the database
   * @throws IllegalArgumentException if teamNumber is for an internal team
   * @see Team#isInternal
   */
  public static void updateBracket(final Connection connection,
                                   final ScoreType performanceScoreType,
                                   final String headToHeadBracket,
                                   final Team team,
                                   final int performanceRunNumber)
      throws SQLException {
    if (team.isInternal()) {
      throw new IllegalArgumentException("Cannot reliably determine playoff dbline for internal teams");
    }

    final int tournamentId = Queries.getCurrentTournament(connection);

    final int dbLine = Queries.getPlayoffTableLineNumber(connection, tournamentId, team.getTeamNumber(),
                                                         performanceRunNumber);
    updateBracket(connection, performanceScoreType, headToHeadBracket, team, performanceRunNumber, dbLine);
  }

  /**
   * Update the display for the information in this bracket. This function queries
   * the database for the the information to display.
   * 
   * @param connection the database connection
   * @param performanceScoreType the type of scores for performance (used for
   *          display)
   * @param headToHeadBracket the bracket name
   * @param team the team
   * @param performanceRunNumber the run number
   * @param dbLine the line in the playoff table to find the bracket entry at
   * @throws SQLException on a database error
   */
  public static void updateBracket(final Connection connection,
                                   final ScoreType performanceScoreType,
                                   final String headToHeadBracket,
                                   final Team team,
                                   final int performanceRunNumber,
                                   final int dbLine)
      throws SQLException {
    final int tournamentId = Queries.getCurrentTournament(connection);

    final int teamNumber = team.getTeamNumber();
    final int playoffRound = Playoff.getPlayoffRound(connection, tournamentId, headToHeadBracket, performanceRunNumber);
    final int maxPlayoffRound = Playoff.getMaxPlayoffRound(connection, tournamentId, headToHeadBracket);

    final Double score = Queries.getPerformanceScore(connection, tournamentId, teamNumber, performanceRunNumber);
    final boolean noShow = Queries.isNoShow(connection, tournamentId, teamNumber, performanceRunNumber);
    final boolean verified = Queries.isVerified(connection, tournamentId, teamNumber, performanceRunNumber);

    final String table = Queries.getAssignedTable(connection, tournamentId, headToHeadBracket, playoffRound, dbLine);

    updateBracket(headToHeadBracket, dbLine, playoffRound, maxPlayoffRound, teamNumber, team.getTeamName(), score,
                  performanceScoreType, noShow, verified, table);
  }

  // CHECKSTYLE:OFF - data class for websocket

  public static final class RegisterMessage {
    /**
     * Uuid of the display.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public String uuid = "";

    /**
     * Bracket information.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public Collection<BracketInfo> brackInfo = Collections.emptyList();
  }

  /**
   * Message sent on the WebSocket.
   */
  public static final class BracketMessage {

    /**
     * If true, then {@link #bracketUpdate} must be populated.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public boolean isBracketUpdate = false;

    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public @Nullable BracketUpdate bracketUpdate = null;

    /**
     * If true then {@link #allBracketInfo} must be populated.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public boolean isDisplayUpdate = false;

    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public List<BracketInfo> allBracketInfo = new LinkedList<>();

  }
  // CHECKSTYLE:ON

}
