/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sent from the client to register the display.
 */
final class RegisterMessage extends Message {

  /**
   * @param displayUuid {@link #getDisplayUuid()}
   */
  RegisterMessage(@JsonProperty("displayUuid") final String displayUuid) {
    super(Message.MessageType.REGISTER);
    this.displayUuid = displayUuid;
  }

  private final String displayUuid;

  public String getDisplayUuid() {
    return this.displayUuid;
  }
}
