/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types of messages based on
 * https://github.com/poelstra/mhub/blob/master/src/protocol.ts
 */
public enum MhubMessageType {
  SUBSCRIBE_COMMAND("subscribe"), PUBLISH_COMMAND("publish"), PING_COMMAND("ping"), LOGIN_COMMAND(
      "login"), MESSAGE_RESPONSE("message"), SUB_ACK_RESPONSE("suback"), PUB_ACK_RESPONSE(
          "puback"), PING_ACK_RESPONSE("pingack"), LOGIN_ACK_RESPONSE("loginack"), ERROR_RESPONSE("error");

  private MhubMessageType(final String type) {
    this.type = type;
  }

  private final String type;

  /**
   * @return type JSON string for the type
   */
  @JsonValue
  public String getType() {
    return type;
  }
}
