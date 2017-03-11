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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.itextpdf.text.Utilities;

import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Provide updates on the state of head to head brackets.
 */
@ServerEndpoint("/playoff/H2HUpdateWebSocket")
public class H2HUpdateWebSocket {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final Map<String, Set<Session>> SESSIONS = new HashMap<>();

  private static final Object SESSIONS_LOCK = new Object();

  private static void addSession(final Session session,
                                 final Collection<String> bracketNames) {
        synchronized (SESSIONS_LOCK) {
      for (final String bracketName : bracketNames) {
        if (!SESSIONS.containsKey(bracketName)) {
          SESSIONS.put(bracketName, new HashSet<Session>());
        }
        SESSIONS.get(bracketName).add(session);
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

      final BracketInfo bracketInfo = jsonMapper.readValue(reader, BracketInfo.class);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Bracket names: "
            + bracketInfo.bracketNames + " first: " + bracketInfo.firstRound + " last: " + bracketInfo.lastRound);
      }

      addSession(session, bracketInfo.bracketNames);

    } catch (final IOException e) {
      LOGGER.error("Error reading JSON data", e);
      throw new FLLRuntimeException("Error parsing string array", e);
    }

//    // just echo for testing
//    try {
//      if (session.isOpen()) {
//        LOGGER.trace("Sending back text message: "
//            + msg);
//        session.getBasicRemote().sendText(msg);
//      } else {
//        LOGGER.error("Session is not open");
//      }
//    } catch (IOException e) {
//      LOGGER.error("Error sending text message, closing socket", e);
//      try {
//        session.close();
//      } catch (IOException e1) {
//        // Ignore
//      }
//    }

    LOGGER.trace("Bottom of message received");
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
                                   final Double score) {
    final BracketUpdate update = new BracketUpdate(bracketName, dbLine, playoffRound, teamNumber, teamName, score);

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

  private static final class BracketUpdate implements Serializable {
    public final String bracketName;

    public final int dbLine;

    public final int playoffRound;

    public final String teamName;

    public final Integer teamNumber;

    /**
     * Non-null string that reprpesents the score.
     */
    public final String score;

    public BracketUpdate(final String bracketName,
                         final int dbLine,
                         final int playoffRound,
                         final Integer teamNumber,
                         final String teamName,
                         final Double score) {
      this.bracketName = bracketName;
      this.dbLine = dbLine;
      this.playoffRound = playoffRound;
      this.teamNumber = teamNumber;
      this.teamName = teamName;
      //TODO #528 update this with the appropriate number formatter
      this.score = null == score ? "" : fll.Utilities.NUMBER_FORMAT_INSTANCE.format(score);
    }
  }

  private static final class BracketInfo implements Serializable {

    public int firstRound;

    public int lastRound;

    @JsonDeserialize(as = ArrayList.class, contentAs = String.class)
    public List<String> bracketNames;

  }

}
