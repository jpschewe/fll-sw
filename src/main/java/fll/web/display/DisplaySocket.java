/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import java.io.IOException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.web.WebUtils;
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
   * @return true if the message was sent, false if not.
   * @throws JsonProcessingException if the message cannot be written as JSON
   */
  public boolean sendMessage(final Message message) throws JsonProcessingException {
    if (null != session) {
      final String msg = jsonWriter.writeValueAsString(message);

      return WebUtils.sendWebsocketTextMessage(session, msg);
    } else {
      // no session, consider this a successful send
      return true;
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
