/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.util.FLLInternalException;
import jakarta.servlet.jsp.PageContext;

/**
 * A message between the server and a playoff client.
 */
/* package */ abstract class Message {

  /**
   * Types of messages that can be sent to a display client.
   */
  /* package */ enum MessageType {
    REGISTER, BRACKET_UPDATE, DISPLAY_UPDATE;
  }

  /**
   * Set variables in the page context for the message types.
   */
  static void setPageVariables(final PageContext pageContext) {
    pageContext.setAttribute("REGISTER_MESSAGE_TYPE", Message.MessageType.REGISTER.toString());
    pageContext.setAttribute("BRACKET_UPDATE_MESSAGE_TYPE", Message.MessageType.BRACKET_UPDATE.toString());
    pageContext.setAttribute("DISPLAY_UPDATE_MESSAGE_TYPE", Message.MessageType.DISPLAY_UPDATE.toString());
  }

  /**
   * @param type {@link #getType()}
   */
  Message(@JsonProperty("type") final MessageType type) {
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
    case REGISTER:
      messageClass = RegisterMessage.class;
      break;
    case DISPLAY_UPDATE:
      messageClass = DisplayUpdateMessage.class;
      break;
    case BRACKET_UPDATE:
      messageClass = BracketUpdateMessage.class;
      break;
    default:
      throw new FLLInternalException(String.format("Unknown message type: %s", type));
    }

    final Message msg = mapper.convertValue(raw, messageClass);
    return msg;
  }

}
