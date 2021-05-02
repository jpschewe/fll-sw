/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handle updating the big screen display pages.
 */
@ServerEndpoint(value = "/DisplayWebSocket", configurator = GetHttpSessionConfigurator.class)
public class DisplayWebSocket {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * session -> display name
   */
  private static final Map<Session, @Nullable String> ALL_SESSIONS = new HashMap<>();

  private static final Object SESSIONS_LOCK = new Object();

  /**
   * @param session the session for the newly opened websocket
   */
  @OnOpen
  public void onOpen(final Session session) {
    final HttpSession httpSession = (HttpSession) session.getUserProperties()
                                                         .get(GetHttpSessionConfigurator.HTTP_SESSION_KEY);
    if (null == httpSession) {
      LOGGER.error("Could not find HttpSession in user properties. Websocket session is not added to ALL_SESSIONS.");
      return;
    }

    final String displayName = SessionAttributes.getDisplayName(httpSession);

    synchronized (SESSIONS_LOCK) {
      ALL_SESSIONS.put(session, displayName);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Adding session "
            + session
            + " display: "
            + displayName);
      }
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
   * Notify all clients that they should update.
   *
   * @param httpApplication the application context to get the displays from
   */
  public static void notifyToUpdate(final ServletContext httpApplication) {
    synchronized (SESSIONS_LOCK) {
      final Set<Session> toRemove = new HashSet<>();

      final String messageText = "update"; // message text doesn't matter
      for (final Map.Entry<Session, @Nullable String> entry : ALL_SESSIONS.entrySet()) {
        final Session session = entry.getKey();
        final String displayName = entry.getValue();

        final DisplayInfo displayInfo = DisplayInfo.getNamedDisplay(httpApplication, displayName);
        if (session.isOpen()) {

          try {
            session.getBasicRemote().sendText(messageText);
          } catch (final IOException ioe) {
            LOGGER.error("Got error sending message to session ("
                + session.getId()
                + "), dropping session", ioe);
            toRemove.add(session);
          }

          // if this is a named display, update the time last seen
          if (null != displayInfo) {
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Updating last seen time for display: "
                  + displayName
                  + " display: "
                  + displayInfo.getName());
            }

            displayInfo.updateLastSeen(httpApplication);
          } // non-null DisplayInfo
        } else {
          LOGGER.info("Removing closed session: "
              + session.getId());

          toRemove.add(session);
        }
      } // foreach session

      // cleanup dead sessions
      for (final Session session : toRemove) {
        internalRemoveSession(session);
      }
    }
  }

  /**
   * @param session the session that threw an error
   * @param t the exception
   */
  @OnError
  public void error(final Session session,
                    final Throwable t) {
    synchronized (SESSIONS_LOCK) {
      if (t instanceof EOFException) {
        LOGGER.warn("Got end of file from websocket, assuming that the webserver shutdown", t);
      } else {
        LOGGER.error("Caught websocket error, closing session", t);
      }

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
