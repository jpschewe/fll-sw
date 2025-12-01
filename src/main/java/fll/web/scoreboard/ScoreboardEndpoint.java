/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.EOFException;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.web.GetHttpSessionConfigurator;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Registration point for scoreboard dynamic updates.
 */
@ServerEndpoint(value = "/scoreboard/ScoreboardEndpoint", configurator = GetHttpSessionConfigurator.class)
public class ScoreboardEndpoint {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private @MonotonicNonNull Session session;

  private @MonotonicNonNull String uuid;

  private final ObjectMapper mapper;

  public ScoreboardEndpoint() {
    mapper = Utilities.createJsonMapper();
  }

  @OnOpen
  public void start(final Session session) {
    this.session = session;
  }

  @OnMessage
  public void onMessage(final String text) {
    if (null == this.session) {
      LOGGER.error("Received websocket message before receiving the session from start, ignoring");
      return;
    }

    LOGGER.trace("{}: Received message: {}", session, text);

    try {
      final JsonNode raw = mapper.readTree(text);
      final Message message = Message.parseMessage(mapper, raw);

      switch (message.getType()) {
      case REGISTER:
        final RegisterMessage registerMessage = (RegisterMessage) message;
        this.uuid = ScoreboardUpdates.addClient(registerMessage.getDisplayUuid(), session,
                                                GetHttpSessionConfigurator.getHttpSession(session));
        break;
      case UPDATE:
        LOGGER.warn("{}: Received UPDATE message from client, ignoring", uuid);
        break;
      case DELETE:
        LOGGER.warn("{}: Received DELETE message from client, ignoring", uuid);
        break;
      case RELOAD:
        LOGGER.warn("{}: Received RELOAD message from client, ignoring", uuid);
        break;
      default:
        LOGGER.error("{}: Received unknown message type from client: {}", uuid, message.getType());
      }
    } catch (final JsonProcessingException e) {
      LOGGER.error("Error reading json data from string: {}, ignoring", text, e);
    }
  }

  @OnClose
  public void end() {
    if (null != uuid) {
      ScoreboardUpdates.removeClient(uuid);
    }
  }

  @OnError
  public void onError(final Throwable t) {
    if (t instanceof EOFException) {
      LOGGER.debug("{}: Socket closed.", uuid);
    } else {
      LOGGER.error("{}: Display socket error", uuid, t);
    }
  }

}
