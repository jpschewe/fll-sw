/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.GetHttpSessionConfigurator;
import fll.web.WebUtils;
import fll.web.display.DisplayHandler;
import fll.web.display.DisplayInfo;
import fll.web.display.UnknownDisplayException;
import fll.xml.ScoreType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Provide updates on the state of head to head brackets.
 */
@ServerEndpoint(value = "/playoff/H2HUpdateWebSocket", configurator = GetHttpSessionConfigurator.class)
public class H2HUpdateWebSocket {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

  /*
   * Note about UUIDs and how they relate to DisplayInfo UUIDs.
   * 
   * The UUIDs can be used to map from bracket names to web socket sessions and to
   * map from
   * the individual UUID to a web socket session.
   * The UUID is used as a way to refer to a web socket session.
   * 
   * When the web socket is from a display page, then the UUID matches the UUID
   * from a DisplayInfo object.
   * When the web socket is from another head to head page, the UUID has been
   * generated inside addSession.
   * This allows the remote control display page to update information in this
   * class using UUIDs from DisplayInfo objects.
   * However one cannot assume the every UUID here corresponds to a DisplayInfo
   * object.
   * 
   */

  // bracketname -> uuids
  private static final Map<String, Set<String>> SESSIONS = new HashMap<>();

  // uuid -> session
  private static final Map<String, Session> ALL_SESSIONS = new HashMap<>();

  private static final Object SESSIONS_LOCK = new Object();

  private @MonotonicNonNull Session session;

  private @MonotonicNonNull String h2hUuid;

  @OnOpen
  public void start(final Session session) {
    this.session = session;
  }

  /**
   * Add the session and send out messages for all bracket information for the
   * specified brackets.
   * 
   * @param displayUuid the UUID of the display, will be null or empty if no
   *          display is associated with this web socket
   * @param session the session to add
   * @param allBracketInfo the brackets that the session is interested in
   * @throws IOException on an error writing to the websocket
   * @throws SQLException on a database error
   * @return UUID for head to head updates
   */
  private String addSession(final @Nullable String displayUuid,
                            final Session session,
                            final Collection<BracketInfo> allBracketInfo,
                            final Connection connection,
                            final int currentTournament)
      throws SQLException {
    synchronized (SESSIONS_LOCK) {

      final String h2hUuid;
      if (!StringUtils.isBlank(displayUuid)) {
        h2hUuid = displayUuid;
        try {
          final DisplayInfo displayInfo = DisplayHandler.resolveDisplay(displayUuid);
          if (!updateDisplayedBracket(displayInfo, session)) {
            removeH2HDisplay(h2hUuid);
            DisplayHandler.removeDisplay(displayUuid);
            return h2hUuid;
          }
        } catch (final UnknownDisplayException e) {
          LOGGER.warn("Cannot find display {}, dropping from head to head", displayUuid);
          removeH2HDisplay(h2hUuid);
          DisplayHandler.removeDisplay(displayUuid);
          return h2hUuid;
        }
      } else {
        // not associated with a display
        h2hUuid = UUID.randomUUID().toString();
      }

      ALL_SESSIONS.put(h2hUuid, session);

      final ObjectMapper mapper = Utilities.createJsonMapper();
      for (final BracketInfo bracketInfo : allBracketInfo) {

        SESSIONS.computeIfAbsent(bracketInfo.getBracketName(), k -> new HashSet<>()).add(h2hUuid);

        // send the current information for the bracket to the session so that
        // it's current
        final Collection<BracketUpdate> updates = Queries.getH2HBracketData(connection, currentTournament,
                                                                            bracketInfo.getBracketName(),
                                                                            bracketInfo.getFirstRound(),
                                                                            bracketInfo.getLastRound());
        for (final BracketUpdate update : updates) {

          final BracketUpdateMessage message = new BracketUpdateMessage(update);

          try {
            final String messageText = mapper.writeValueAsString(message);
            if (!WebUtils.sendWebsocketTextMessage(session, messageText)) {
              removeH2HDisplay(h2hUuid);
            }
          } catch (final JsonProcessingException e) {
            throw new FLLRuntimeException("Error converting bracket update to JSON", e);
          }
        } // foreach update

      } // foreach bracket

      return h2hUuid;
    } // session lock
  }

  private static void removeH2HDisplay(final String h2hUuid) {
    synchronized (SESSIONS_LOCK) {
      final Session session = ALL_SESSIONS.get(h2hUuid);
      if (null != session) {
        try {
          session.close();
        } catch (final IOException ioe) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got error closing session, ignoring", ioe);
          }
        }
      }

      ALL_SESSIONS.remove(h2hUuid);
      for (final Map.Entry<String, Set<String>> entry : SESSIONS.entrySet()) {
        entry.getValue().remove(h2hUuid);
      } // foreach set of sessions
    } // sessions lock
  }

  /**
   * @param msg the message received
   */
  @OnMessage
  public void receiveTextMessage(final String msg) {
    if (null == this.session) {
      LOGGER.error("Received websocket message before receiving the session from start, ignoring");
      return;
    }

    LOGGER.trace("Received message '{}'", msg);

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    // may get passed a javascript object with extra fields, just ignore them
    // this happens when BracketInfo is subclassed and the subclass is passed
    // in
    jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    try {
      final JsonNode raw = jsonMapper.readTree(msg);
      final Message message = Message.parseMessage(jsonMapper, raw);

      switch (message.getType()) {
      case REGISTER:
        final RegisterMessage registerMessage = (RegisterMessage) message;

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Display UUID: '{}'", registerMessage.getDisplayUuid());
          for (final BracketInfo bracketInfo : registerMessage.getBracketInfo()) {
            LOGGER.trace("Bracket name: {} first: {} last: {}", bracketInfo.getBracketName(),
                         bracketInfo.getFirstRound(), bracketInfo.getLastRound());
          }
        }

        final HttpSession httpSession = GetHttpSessionConfigurator.getHttpSession(session);
        final ServletContext httpApplication = httpSession.getServletContext();
        final DataSource datasource = ApplicationAttributes.getDataSource(httpApplication);
        try (Connection connection = datasource.getConnection()) {
          final int currentTournament = Queries.getCurrentTournament(connection);
          h2hUuid = addSession(registerMessage.getDisplayUuid(), session, registerMessage.getBracketInfo(), connection,
                               currentTournament);
        }
        break;
      case BRACKET_UPDATE:
        LOGGER.warn("{}: Received BRACKET_UPDATE message from client, ignoring", h2hUuid);
        break;
      case DISPLAY_UPDATE:
        LOGGER.warn("{}: Received DISPLAY_UPDATE message from client, ignoring", h2hUuid);
        break;
      default:
        LOGGER.error("{}: Received unknown message type from client: {}", h2hUuid, message.getType());
      }
    } catch (final JsonProcessingException e) {
      throw new FLLRuntimeException("Error parsing bracket info from #"
          + msg
          + "#", e);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  /**
   * Notify a specific display to update the brackets it's showing.
   * If the {@link DisplayInfo} isn't showing the brackets, nothing is sent to the
   * display. This is executed asynchronously.
   * 
   * @param displayInfo the display information for the display
   */
  public static void updateDisplayedBracket(final DisplayInfo displayInfo) {
    // used to find the sockets to talk to
    final String h2hUuid = displayInfo.getUuid();

    try {
      // make sure that we update the correct bracket information used
      final DisplayInfo resolved = DisplayHandler.resolveDisplay(displayInfo.getUuid());

      if (resolved.isHeadToHead()) {
        synchronized (SESSIONS_LOCK) {
          final @Nullable Session session = ALL_SESSIONS.get(h2hUuid);
          if (null != session) {
            THREAD_POOL.execute(() -> {
              if (!updateDisplayedBracket(resolved, session)) {
                removeH2HDisplay(h2hUuid);
              }
            });
          } else {
            LOGGER.warn("Found display info with uuid {} that is displaying head to head and doesn't have a head to head web socket",
                        h2hUuid);
          }
        } // session lock
      } // is head to head
    } catch (final UnknownDisplayException e) {
      LOGGER.warn("Unable to find display {}, removing from head to head", displayInfo.getUuid());
      removeH2HDisplay(h2hUuid);
    }
  }

  /**
   * Notify a head to head display what brackets it should be displaying.
   * 
   * @return false if there was an error sending
   */
  private static boolean updateDisplayedBracket(final DisplayInfo displayInfo,
                                                final Session session) {

    final List<BracketInfo> allBracketInfo = new LinkedList<>();

    for (final DisplayInfo.H2HBracketDisplay h2hBracket : displayInfo.getBrackets()) {
      final BracketInfo bracketInfo = new BracketInfo(h2hBracket.getBracket(), h2hBracket.getFirstRound(),
                                                      h2hBracket.getFirstRound()
                                                          + RemoteControlBrackets.NUM_ROUNDS_TO_DISPLAY
                                                          - 1);

      allBracketInfo.add(bracketInfo);
    } // foreach h2h bracket

    final DisplayUpdateMessage message = new DisplayUpdateMessage(allBracketInfo);

    // expose all bracketInfo to the javascript
    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    try {
      final String allBracketInfoJson = jsonMapper.writeValueAsString(message);

      return WebUtils.sendWebsocketTextMessage(session, allBracketInfoJson);
    } catch (final JsonProcessingException e) {
      throw new FLLInternalException("Error writing JSON for allBracketInfo", e);
    }
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

    final BracketUpdate bracketUpdate = new BracketUpdate(bracketName, dbLine, playoffRound, maxPlayoffRound,
                                                          teamNumber, teamName, score, performanceScoreType, noShow,
                                                          verified, table);
    final BracketUpdateMessage message = new BracketUpdateMessage(bracketUpdate);

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

        if (!WebUtils.sendWebsocketTextMessage(session, messageText)) {
          toRemove.add(uuid);
        }
      } // foreach session

      // cleanup dead sessions
      for (final String uuid : toRemove) {
        removeH2HDisplay(uuid);
      } // foreach session to remove

    } // end lock

  }

  @OnClose
  public void end() {
    if (null != h2hUuid) {
      removeH2HDisplay(h2hUuid);
    }
  }

  @OnError
  public void onError(final Throwable t) throws Throwable {
    if (t instanceof EOFException) {
      LOGGER.debug("{}: Socket closed.", h2hUuid, t);
    } else {
      LOGGER.error("{}: Display socket error", h2hUuid, t);
    }
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
   * the database for the the information to display. The update is sent
   * asynchronously.
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

    THREAD_POOL.execute(() -> {
      updateBracket(headToHeadBracket, dbLine, playoffRound, maxPlayoffRound, teamNumber, team.getTeamName(), score,
                    performanceScoreType, noShow, verified, table);
    });
  }

}
