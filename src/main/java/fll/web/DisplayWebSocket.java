/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Handle updating the big screen display pages.
 */
@ServerEndpoint(value = "/DisplayWebSocket", configurator = GetHttpSessionConfigurator.class)
public class DisplayWebSocket {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * session -> display name
   */
  private static final Map<Session, String> ALL_SESSIONS = new HashMap<>();

  private static final Object SESSIONS_LOCK = new Object();

  @OnOpen
  public void onOpen(final Session session) {
    final HttpSession httpSession = (HttpSession) session.getUserProperties()
                                                         .get(GetHttpSessionConfigurator.HTTP_SESSION_KEY);
    final String displayName = SessionAttributes.getDisplayName(httpSession);

    synchronized (SESSIONS_LOCK) {
      ALL_SESSIONS.put(session, displayName);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Adding session "
            + session + " display: " + displayName);
      }
    }
  }

  /**
   * Notify all clients that they should update.
   */
  public static void notifyToUpdate(final ServletContext httpApplication) {
    synchronized (SESSIONS_LOCK) {
      final Set<Session> toRemove = new HashSet<>();

      final String messageText = "update"; // message text doesn't matter
      for (final Map.Entry<Session, String> entry : ALL_SESSIONS.entrySet()) {
        final Session session = entry.getKey();
        final String displayName = entry.getValue();

        final DisplayInfo displayInfo = DisplayInfo.getNamedDisplay(httpApplication, displayName);
        if (session.isOpen()) {

          try {
            session.getBasicRemote().sendText(messageText);
          } catch (final IOException ioe) {
            LOGGER.error("Got error sending message to session ("
                + session.getId() + "), dropping session", ioe);
            toRemove.add(session);
          }

          // if this is a named display, update the time last seen
          if (null != displayInfo) {
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Updating last seen time for display: "
                  + displayName + " display: " + displayInfo.getName());
            }

            displayInfo.updateLastSeen(httpApplication);
          } // non-null DisplayInfo
        } else {
          LOGGER.info("Removing closed session: " + session.getId());
          
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
