/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.util.FLLInternalException;

/**
 * A message sent to a web client.
 */
/* package */ abstract class Message {

  /**
   * Types of messages that can be sent to a display client.
   */
  /* package */ enum MessageType {
    REGISTER_DISPLAY, ASSIGN_UUID, DISPLAY_URL, PING;
  }

  /**
   * @param type {@link #getType()}
   */
  /* package */ Message(@JsonProperty("type") final MessageType type) {
    this.type = type;
  }

  private final MessageType type;

  /**
   * @return type of the message
   */
  public MessageType getType() {
    return type;
  }

  /**
   * Convert raw JSON data into a message.
   * 
   * @param mapper JSON mapper to use
   * @param raw the raw JSON node
   * @return a parsed message
   */
  public static Message parseMessage(final ObjectMapper mapper,
                                     final JsonNode raw) {
    final String typeStr = raw.get("type").textValue();
    final Message.MessageType type = Message.MessageType.valueOf(typeStr);
    final Class<? extends Message> messageClass;
    switch (type) {
    case REGISTER_DISPLAY:
      messageClass = RegisterDisplayMessage.class;
      break;
    case ASSIGN_UUID:
      messageClass = AssignUuidMessage.class;
      break;
    case DISPLAY_URL:
      messageClass = DisplayUrlMessage.class;
      break;
    case PING:
      messageClass = PingMessage.class;
      break;
    default:
      throw new FLLInternalException(String.format("Unknown message type: %s", type));
    }

    final Message msg = mapper.convertValue(raw, messageClass);
    return msg;
  }

}
