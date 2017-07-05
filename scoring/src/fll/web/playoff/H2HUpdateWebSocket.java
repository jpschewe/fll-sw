/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;
import fll.web.GetHttpSessionConfigurator;
import fll.xml.ScoreType;

/**
 * Provide updates on the state of head to head brackets.
 */
@ServerEndpoint(value = "/playoff/H2HUpdateWebSocket", configurator = GetHttpSessionConfigurator.class)
public class H2HUpdateWebSocket {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final Map<String, Set<Session>> SESSIONS = new HashMap<>();

  private static final Set<Session> ALL_SESSIONS = new HashSet<>();

  private static final Object SESSIONS_LOCK = new Object();

  /**
   * Add the session and send out messages for all bracket information for the
   * specified brackets.
   * 
   * @param session the session to add
   * @param allBracketInfo the brackets that the session is interested in
   * @throws IOException
   * @throws SQLException
   */
  private static void addSession(final Session session,
                                 final Collection<BracketInfo> allBracketInfo,
                                 final Connection connection,
                                 final int currentTournament)
      throws IOException, SQLException {
    synchronized (SESSIONS_LOCK) {
      final ObjectMapper jsonMapper = new ObjectMapper();

      ALL_SESSIONS.add(session);

      updateDisplayedBracket(session);

      for (final BracketInfo bracketInfo : allBracketInfo) {

        if (!SESSIONS.containsKey(bracketInfo.getBracketName())) {
          SESSIONS.put(bracketInfo.getBracketName(), new HashSet<Session>());
        }

        SESSIONS.get(bracketInfo.getBracketName()).add(session);

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

  @OnMessage
  public void receiveTextMessage(final Session session,
                                 final String msg) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Received message '"
          + msg + "'");
    }

    final Reader reader = new StringReader(msg);

    try {
      final ObjectMapper jsonMapper = new ObjectMapper();

      // may get passed a javascript object with extra fields, just ignore them
      // this happens when BracketInfo is subclassed and the subclass is passed
      // in
      jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      final Collection<BracketInfo> allBracketInfo = jsonMapper.readValue(reader, BracketInfoTypeInformation.INSTANCE);

      if (LOGGER.isTraceEnabled()) {
        for (final BracketInfo bracketInfo : allBracketInfo) {
          LOGGER.trace("Bracket name: "
              + bracketInfo.getBracketName() + " first: " + bracketInfo.getFirstRound() + " last: "
              + bracketInfo.getLastRound());
        }
      }

      final HttpSession httpSession = (HttpSession) session.getUserProperties()
                                                           .get(GetHttpSessionConfigurator.HTTP_SESSION_KEY);
      final ServletContext httpApplication = httpSession.getServletContext();
      final DataSource datasource = ApplicationAttributes.getDataSource(httpApplication);
      try (final Connection connection = datasource.getConnection()) {
        final int currentTournament = Queries.getCurrentTournament(connection);
        addSession(session, allBracketInfo, connection, currentTournament);
      }

    } catch (final IOException e) {
      LOGGER.error("Error reading JSON data", e);
      throw new FLLRuntimeException("Error parsing string array", e);
    } catch (final SQLException e) {
      LOGGER.error("Error getting playoffdata from the database", e);
      throw new FLLRuntimeException("Error getting playoff data from the database", e);
    }
  }

  private static void updateDisplayedBracket(final Session session) {

    if (session.isOpen()) {

      final HttpSession httpSession = (HttpSession) session.getUserProperties()
                                                           .get(GetHttpSessionConfigurator.HTTP_SESSION_KEY);
      final ServletContext httpApplication = httpSession.getServletContext();

      final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(httpApplication, httpSession);

      final BracketMessage message = new BracketMessage();
      message.isDisplayUpdate = true;

      for (final DisplayInfo.H2HBracketDisplay h2hBracket : displayInfo.getBrackets()) {
        final BracketInfo bracketInfo = new BracketInfo(h2hBracket.getBracket(), h2hBracket.getFirstRound(),
                                                        h2hBracket.getFirstRound()
                                                            + 2);

        message.allBracketInfo.add(bracketInfo);
      } // foreach h2h bracket

      // expose all bracketInfo to the javascript
      final ObjectMapper jsonMapper = new ObjectMapper();
      try (final StringWriter writer = new StringWriter()) {
        jsonMapper.writeValue(writer, message);
        final String allBracketInfoJson = writer.toString();

        session.getBasicRemote().sendText(allBracketInfoJson);
      } catch (final IOException e) {
        throw new FLLInternalException("Error writing JSON for allBracketInfo", e);
      }
    } // open session
  }

  /**
   * Send each display the most recent bracket information to show.
   */
  public static void updateDisplayedBracket() {
    synchronized (SESSIONS_LOCK) {
      for (final Session session : ALL_SESSIONS) {
        updateDisplayedBracket(session);
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
  public static void updateBracket(final String bracketName,
                                   final int dbLine,
                                   final int playoffRound,
                                   final int maxPlayoffRound,
                                   final Integer teamNumber,
                                   final String teamName,
                                   final Double score,
                                   final ScoreType performanceScoreType,
                                   final boolean noShow,
                                   final boolean verified,
                                   final String table) {
    final BracketMessage message = new BracketMessage();
    message.isBracketUpdate = true;
    message.bracketUpdate = new BracketUpdate(bracketName, dbLine, playoffRound, maxPlayoffRound, teamNumber, teamName,
                                              score, performanceScoreType, noShow, verified, table);

    synchronized (SESSIONS_LOCK) {
      if (!SESSIONS.containsKey(bracketName)) {
        return;
      }

      final Set<Session> toRemove = new HashSet<>();

      final ObjectMapper jsonMapper = new ObjectMapper();

      final StringWriter writer = new StringWriter();

      try {
        jsonMapper.writeValue(writer, message);
      } catch (final IOException e) {
        throw new FLLInternalException("Error writing JSON for brackets", e);
      }
      final String messageText = writer.toString();

      final Set<Session> sessions = SESSIONS.get(bracketName);
      for (final Session session : sessions) {
        if (session.isOpen()) {
          try {
            session.getBasicRemote().sendText(messageText);
          } catch (final IOException ioe) {
            LOGGER.error("Got error sending message to session ("
                + session.getId() + "), dropping session", ioe);
            toRemove.add(session);
          }
        } else {
          toRemove.add(session);
        }
      } // foreach session

      // cleanup dead sessions
      for (final Session session : toRemove) {
        try {
          session.close();
        } catch (final IOException ioe) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got error closing session, ignoring", ioe);
          }
        }

        sessions.remove(session);
        ALL_SESSIONS.remove(session);
      } // foreach session to remove

    } // end lock

  }

  @OnError
  public void error(@SuppressWarnings("unused") final Session session,
                    final Throwable t) {
    LOGGER.error("Caught websocket error", t);
  }

  /**
   * Message sent on the WebSocket.
   */
  public static final class BracketMessage implements Serializable {

    /**
     * If true, then {@link #bracketUpdate} must be populated.
     */
    public boolean isBracketUpdate = false;

    public BracketUpdate bracketUpdate;

    /**
     * If true then {@link #allBracketInfo} must be populated.
     */
    public boolean isDisplayUpdate = false;

    public List<BracketInfo> allBracketInfo = new LinkedList<>();

  }

  private static final class BracketInfoTypeInformation extends TypeReference<Collection<BracketInfo>> {
    public static final BracketInfoTypeInformation INSTANCE = new BracketInfoTypeInformation();
  }

}