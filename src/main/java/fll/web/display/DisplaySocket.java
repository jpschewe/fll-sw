/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import java.io.IOException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import jakarta.websocket.Session;

/**
 * Abstraction of a web socket.
 */
public class DisplaySocket {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final ObjectMapper jsonWriter = Utilities.createJsonMapper();

  private final @Nullable Session session;

  /**
   * Creates a socket that does nothing.
   */
  public DisplaySocket() {
    this.session = null;
  }

  /**
   * @param session where to send data.
   */
  public DisplaySocket(final Session session) {
    this.session = session;
  }

  /**
   * Send a message as JSON.
   * 
   * @param message the message to send
   * @throws IOException when there is an error sending or ending as JSON
   */
  public void sendMessage(final Message message) throws IOException {
    if (null != session) {
      final String msg = jsonWriter.writeValueAsString(message);
      session.getBasicRemote().sendText(msg);
    }
  }

  /**
   * Close the socket.
   */
  public void close() {
    if (null != session) {
      try {
        session.close();
      } catch (final IOException e) {
        LOGGER.debug("Error closing web socket, ignoring", e);
      }
    }
  }

}
