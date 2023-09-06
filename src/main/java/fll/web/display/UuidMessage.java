/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Common code for messages with a UUID.
 */
/* package */ abstract class UuidMessage extends Message {

  /**
   * @param type {@link #getType()}
   * @param uuid {@link #getUuid()}
   */
  /* package */ UuidMessage(@JsonProperty("type") final Message.MessageType type,
                            @JsonProperty("uuid") final String uuid) {
    super(type);
    this.uuid = uuid;
  }

  private final String uuid;

  /**
   * @return UUID from the client, may be empty
   */
  public String getUuid() {
    return uuid;
  }

}
