/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.util.FLLInternalException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Registration point for displays.
 */
@ServerEndpoint("/display/DisplayEndpoint")
public class DisplayEndpoint {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private @MonotonicNonNull Session session;

  private @MonotonicNonNull String uuid;

  private final ObjectMapper mapper;

  public DisplayEndpoint() {
    mapper = Utilities.createJsonMapper();
  }

  @OnOpen
  public void start(final Session session) {
    this.session = session;
  }

  @OnMessage
  public void onMessage(final String text) {
    if (null == this.session) {
      throw new FLLInternalException("Received websocket message before receiving the session from start");
    }

    LOGGER.trace("Received message: {}", text);

    try {
      final JsonNode raw = mapper.readTree(text);
      final Message message = Message.parseMessage(mapper, raw);

      switch (message.getType()) {
      case REGISTER_DISPLAY:
        final RegisterDisplayMessage registerMessage = (RegisterDisplayMessage) message;
        this.uuid = DisplayHandler.registerDisplay(registerMessage.getUuid(), registerMessage.getName(), session);
        break;
      case PING:
        final PingMessage pingMessage = (PingMessage) message;
        DisplayHandler.updateLastSeen(pingMessage.getUuid());
        break;
      case ASSIGN_UUID:
        LOGGER.warn("Received ASSIGN_UUID message from client, ignoring");
        break;
      case DISPLAY_URL:
        LOGGER.warn("Received DISPLAY_URL message from client, ignoring");
        break;
      default:
        LOGGER.error("Received unknown message type from client: {}", message.getType());
      }
    } catch (final JsonProcessingException e) {
      throw new FLLInternalException(String.format("Error reading json data from string: %s", text), e);
    }
  }

  @OnClose
  public void end() {
    if (null != uuid) {
      DisplayHandler.removeDisplay(uuid);
    }
  }

  @OnError
  public void onError(final Throwable t) throws Throwable {
    LOGGER.warn("Display socket error", t);
  }

}
