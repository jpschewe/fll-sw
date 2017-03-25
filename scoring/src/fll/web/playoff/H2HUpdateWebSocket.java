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
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
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
import fll.web.GetHttpSessionConfigurator;

/**
 * Provide updates on the state of head to head brackets.
 */
@ServerEndpoint(value = "/playoff/H2HUpdateWebSocket", configurator = GetHttpSessionConfigurator.class)
public class H2HUpdateWebSocket {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final Map<String, Set<Session>> SESSIONS = new HashMap<>();

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

          final StringWriter writer = new StringWriter();
          try {
            jsonMapper.writeValue(writer, update);
          } catch (final IOException e) {
            throw new FLLInternalException("Error writing JSON for brackets", e);
          }
          final String message = writer.toString();
          session.getBasicRemote().sendText(message);
        } // foreach update
      }
    }
  }

  public static void sendMessage(final String bracketName,
                                 final String message) {
    synchronized (SESSIONS_LOCK) {
      if (!SESSIONS.containsKey(bracketName)) {
        return;
      }

      final Set<Session> toRemove = new HashSet<>();

      final Set<Session> sessions = SESSIONS.get(bracketName);
      for (final Session session : sessions) {
        if (session.isOpen()) {
          try {
            session.getBasicRemote().sendText(message);
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
      } // foreach session to remove

    } // end lock
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
      // this happens when BracketInfo is subclassed and the subclass is passed in
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

    // // just echo for testing
    // try {
    // if (session.isOpen()) {
    // LOGGER.trace("Sending back text message: "
    // + msg);
    // session.getBasicRemote().sendText(msg);
    // } else {
    // LOGGER.error("Session is not open");
    // }
    // } catch (IOException e) {
    // LOGGER.error("Error sending text message, closing socket", e);
    // try {
    // session.close();
    // } catch (IOException e1) {
    // // Ignore
    // }
    // }
  }

  /**
   * Update the head to head display.
   * 
   * @param bracketName the bracket being updated
   * @param dbLine the line in the database
   * @param playoffRound the playoff round
   * @param teamNumber the team that is at the specified row and playoff round,
   *          may be null
   * @param teamName team name, may be null
   * @param score the score for the team, may be null
   */
  public static void updateBracket(final String bracketName,
                                   final int dbLine,
                                   final int playoffRound,
                                   final Integer teamNumber,
                                   final String teamName,
                                   final Double score,
                                   final boolean verified) {
    final BracketUpdate update = new BracketUpdate(bracketName, dbLine, playoffRound, teamNumber, teamName, score,
                                                   verified);

    synchronized (SESSIONS_LOCK) {
      if (!SESSIONS.containsKey(bracketName)) {
        return;
      }

      final Set<Session> toRemove = new HashSet<>();

      final ObjectMapper jsonMapper = new ObjectMapper();

      final StringWriter writer = new StringWriter();

      try {
        jsonMapper.writeValue(writer, update);
      } catch (final IOException e) {
        throw new FLLInternalException("Error writing JSON for brackets", e);
      }
      final String message = writer.toString();

      final Set<Session> sessions = SESSIONS.get(bracketName);
      for (final Session session : sessions) {
        if (session.isOpen()) {
          try {
            session.getBasicRemote().sendText(message);
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
      } // foreach session to remove

    } // end lock

  }

  public static final class BracketUpdate implements Serializable {
    public String bracketName;

    public int dbLine;

    public int playoffRound;

    public String teamName;

    public Integer teamNumber;

    /**
     * Non-null string that represents the score.
     */
    public String score;

    /**
     * True if the information has been verified.
     */
    public boolean verified;

    public BracketUpdate() {
    }

    public BracketUpdate(final String bracketName,
                         final int dbLine,
                         final int playoffRound,
                         final Integer teamNumber,
                         final String teamName,
                         final Double score,
                         final boolean verified) {
      this.bracketName = bracketName;
      this.dbLine = dbLine;
      this.playoffRound = playoffRound;
      this.teamNumber = teamNumber;
      this.teamName = teamName;
      // TODO #528 update this with the appropriate number formatter
      this.score = null == score ? "" : fll.Utilities.NUMBER_FORMAT_INSTANCE.format(score);
      this.verified = verified;
    }
  }

  private static final class BracketInfoTypeInformation extends TypeReference<Collection<BracketInfo>> {
    public static final BracketInfoTypeInformation INSTANCE = new BracketInfoTypeInformation();
  }

}
