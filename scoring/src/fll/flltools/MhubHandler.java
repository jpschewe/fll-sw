/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

/**
 * Handle all communication with mhub.
 */
public class MhubHandler implements ServletContextListener {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static MhubHandler instance = null;

  /**
   * There is a single instance of {@link MhubHandler} for each servlet
   * container. This should always be non-null, but can be null when the servlet
   * container is starting up or shutting down.
   * 
   * @return the singleton instance, may be null
   */
  public static MhubHandler getInstance() {
    return instance;
  }

  private ExecutorService executor = null;

  private boolean running = false;

  // private WebSocketClient webSocket = null;

  @Override
  public void contextDestroyed(@Nonnull final ServletContextEvent event) {
    if (null != instance) {
      throw new IllegalStateException("Cannot have 2 instances of singleton MhubHandler");
    }

    if (null != executor) {
      throw new IllegalStateException("Handler is already executing, cannot start again");
    }

    instance = this;
    executor = Executors.newSingleThreadExecutor();
    executor.submit(() -> execute(event.getServletContext()));
  }

  @Override
  public void contextInitialized(@Nonnull final ServletContextEvent ignored) {
    running = false;
    executor.shutdown();
    executor = null;
    instance = null;
  }

  private void execute(final ServletContext application) {
    running = true;
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      while (running) {

        // FIXME need to do something here to create the web socket and
        // send/receive messages. Maybe send/receive is handled with other
        // methods and callbacks?

        // final String hostname = MhubParameters.getHostname(connection);
        // if (null != hostname) {
        // // create websocket if not already created
        // if(null == webSocket) {
        // createWebSocket(hostname, MhubParameters.getPort(connection));
        // }
        // }
      } // while running
    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database, mhub handler terminated", e);
    }
  }

  /**
   * Send a message to mhub.
   * 
   * @param msg the message to send
   * @return if sending the message was sucessfull
   */
  public boolean sendMesasge(@Nonnull final BaseMessage msg) {
    // FIXME need to write to a StringStream and then send the string through
    // the web socket

    // final ObjectMapper mapper = new ObjectMapper();
    // mapper.writer(stream, msg);

    return true;
  }

//  /**
//   * Close the websocket.
//   */
//  private void closeWebSocket() {
//    webSocket.destroy();
//    webSocket = null;
//  }
//
//  /**
//   * @param hostname the host to connect to
//   * @param port the port to connect on
//   */
//  private void createWebSocket(@Nonnull final String hostname,
//                               final int port) {
//    webSocket = new WebSocketClient();
//    final URI uri = new URI("ws://"
//        + hostname
//        + ":"
//        + port
//        + "/");
//    webSocket.connect(websocket, uri);
//
//  }
//

}
