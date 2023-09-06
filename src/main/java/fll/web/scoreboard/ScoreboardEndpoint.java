/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.GetHttpSessionConfigurator;
import jakarta.servlet.http.HttpSession;
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
      throw new FLLInternalException("Received websocket message before receiving the session from start");
    }

    LOGGER.trace("Received message: {}", text);

    try {
      final JsonNode raw = mapper.readTree(text);
      final Message message = Message.parseMessage(mapper, raw);

      switch (message.getType()) {
      case REGISTER:
        final RegisterMessage registerMessage = (RegisterMessage) message;
        this.uuid = ScoreboardUpdates.addClient(registerMessage.getDisplayUuid(), session, getHttpSession(session));
        break;
      case UPDATE:
        LOGGER.warn("Received UPDATE message from client, ignoring");
        break;
      case DELETE:
        LOGGER.warn("Received DELETE message from client, ignoring");
        break;
      case RELOAD:
        LOGGER.warn("Received RELOAD message from client, ignoring");
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
      ScoreboardUpdates.removeClient(uuid);
    }
  }

  @OnError
  public void onError(final Throwable t) throws Throwable {
    LOGGER.warn("Display socket error", t);
  }

  private static HttpSession getHttpSession(final Session session) {
    final HttpSession httpSession = (HttpSession) session.getUserProperties()
                                                         .get(GetHttpSessionConfigurator.HTTP_SESSION_KEY);

    if (null == httpSession) {
      throw new FLLRuntimeException("Unable to find httpSession in the userProperties");
    }

    return httpSession;
  }

}
