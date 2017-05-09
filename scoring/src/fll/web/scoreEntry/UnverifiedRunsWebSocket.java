/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Notify the select team page when it should reload because there are more
 * unverified runs.
 */
@ServerEndpoint(value = "/scoreEntry/UnverifiedRunsWebSocket")
public class UnverifiedRunsWebSocket {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final Set<Session> ALL_SESSIONS = new HashSet<>();

  private static final Object SESSIONS_LOCK = new Object();

  @OnOpen
  public void onOpen(final Session session) {
    synchronized (SESSIONS_LOCK) {
      ALL_SESSIONS.add(session);
    }
  }

  /**
   * Notify all clients that they should update.
   */
  public static void notifyToUpdate() {
    synchronized (SESSIONS_LOCK) {
      final Set<Session> toRemove = new HashSet<>();

      final String messageText = "update"; // message text doesn't matter
      for (final Session session : ALL_SESSIONS) {
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
        internalRemoveSession(session);
      }
    }
  }

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
}
