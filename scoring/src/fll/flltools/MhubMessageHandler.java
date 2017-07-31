/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.flltools.displaySystem.DisplaySystemHandler;
import fll.util.FLLInternalException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

/**
 * Handle all communication with mhub. This class watches for changes in the
 * mhub global parameters and starts and stops the web socket as needed
 * 
 * @see MhubParameters
 */
@ClientEndpoint
@ThreadSafe
public class MhubMessageHandler extends Thread {

  private final Object lock = new Object();

  private static final Logger LOGGER = LogUtils.getLogger();

  private boolean running = false;

  private DisplaySystemHandler displayHandler = null;

  private final ServletContext application;

  /**
   * Constructs the message handler, but doesn't start it.
   * 
   * @param application used to get various parameters
   */
  public MhubMessageHandler(@Nonnull final ServletContext application) {
    this.application = application;
  }

  /**
   * Stop the thread. If called from another thread, wait for this thread to
   * stop.
   */
  public void shutdown() {
    running = false;
    this.interrupt(); // stop any sleep

    closeWebSocket(); // will shutdown display handler as well

    if (Thread.currentThread() != this) {
      try {
        this.join();
      } catch (final InterruptedException e) {
        LOGGER.warn("Interrupted waiting for shutdown, exiting", e);
      }
    }
  }

  private static final Duration DB_CHECK_INTERVAL = Duration.ofMinutes(1);

  /**
   * Watch for the mhub parameters to change and create websocket as necessary.
   */
  @Override
  public void run() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Starting checking for db parameter changes");
    }

    running = true;
    while (running) {

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      if (null != datasource) {
        try (Connection connection = datasource.getConnection()) {
          if (Utilities.testDatabaseInitialized(connection)) {
            createWebSocket(MhubParameters.getHostname(connection), MhubParameters.getPort(connection));
          } // have valid database
        } catch (final SQLException e) {
          LOGGER.error("Error talking to the database, will try again later", e);
        }

      } // non-null datasource

      try {
        Thread.sleep(DB_CHECK_INTERVAL.toMillis());
      } catch (final InterruptedException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Interrupted sleeping, will exit if running is false", e);
        }
      }

    } // while running
  }

  /**
   * Send a message to mhub.
   * 
   * @param msg the message to send
   * @return if sending the message was successful
   */
  public boolean sendMesasge(@Nonnull final BaseMessage msg) {
    synchronized (lock) {
      if (sessionList.isEmpty()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Session list is empty, cannot send message");
        }
        return false;
      }

      final List<Session> toRemove = new LinkedList<>();

      final StringWriter writer = new StringWriter();
      final ObjectMapper mapper = new ObjectMapper();
      try {
        mapper.writeValue(writer, msg);
      } catch (final IOException e) {
        LOGGER.error("Error converting message to JSON, skipping transmission", e);
        return false;
      }

      final String messageText = writer.getBuffer().toString();

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Sending message "
            + messageText);
      }

      sessionList.forEach(session -> {
        if (session.isOpen()) {
          try {
            session.getBasicRemote().sendText(messageText);
          } catch (final IOException ioe) {
            LOGGER.error("Got error sending message to session ("
                + session.getId()
                + "), dropping session", ioe);
            toRemove.add(session);
          }

        } else {
          toRemove.add(session);
        }
      }); // foreach session

      // cleanup dead sessions
      toRemove.forEach(session -> internalRemoveSession(session));

      return true;
    }
  }

  private final List<Session> sessionList = new LinkedList<>();

  /**
   * Close the websocket and shutdown the display handler thread.
   */
  private void closeWebSocket() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Closing web sockets");
    }

    synchronized (lock) {
      sessionList.forEach(session -> {
        try {
          session.close();
        } catch (final IOException e) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got error closing session, ignoring", e);
          }
        }
      });
      sessionList.clear();
      currentUri = null;

      if (null != displayHandler) {
        displayHandler.shutdown();
        displayHandler = null;
      }
    }
  }

  private URI currentUri = null;

  /**
   * Create the websocket if the parameters are different.
   * 
   * @param hostname the host to connect to
   * @param port the port to connect on
   */
  private void createWebSocket(@Nonnull final String hostname,
                               final int port) {
    synchronized (lock) {
      try {
        final URI newUri = new URI("ws://"
            + hostname
            + ":"
            + port);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Checking if need to create websocket current: "
              + currentUri
              + " new: "
              + newUri);
        }

        if (!Objects.equals(newUri, currentUri)) {
          LOGGER.trace("not equal");

          closeWebSocket();

          currentUri = newUri;

          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Connecting websocket to mhub at "
                + currentUri);
          }

          final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
          container.connectToServer(this, currentUri);

          displayHandler = new DisplaySystemHandler(this, application);
          displayHandler.start();
        } else {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("URIs are the same, no connection required");
          }
        }
      } catch (final URISyntaxException e) {
        LOGGER.error("Bad websocket URI", e);
        throw new FLLInternalException("Bad websocket URI", e);
      } catch (IOException | DeploymentException e) {
        LOGGER.error("Unable to connect to mhub", e);
        currentUri = null;
      }
    }
  }

  @OnError
  public void error(final Session session,
                    final Throwable t) {
    synchronized (lock) {
      LOGGER.error("Caught websocket error, closing session", t);

      internalRemoveSession(session);
    }
  }

  /**
   * Close the session and remove it from the global list without locking.
   */
  private void internalRemoveSession(final Session session) {
    try {
      session.close();
    } catch (final IOException ioe) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got error closing session, ignoring", ioe);
      }
    }

    sessionList.remove(session);
  }

  @OnOpen
  public void onOpen(Session session) throws IOException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Websocket opened: "
          + session.getId());
    }

    synchronized (lock) {
      if (running) {
        sessionList.add(session);
      } else {
        LOGGER.warn("Got session open with running set to false, ignoring");
      }
    }
  }

  @OnClose
  public void onClose(final Session session) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Websocket closed: "
          + session.getId());
    }

    synchronized (lock) {
      internalRemoveSession(session);
    }
  }

}
