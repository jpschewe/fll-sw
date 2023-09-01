/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sent from the client to tell the server it's still alive.
 * 
 * @see DisplayHandler#updateLastSeen(String)
 */
/* package */ final class PingMessage extends UuidMessage {

  /**
   * @param uuid {@link #getUuid()}
   */
  /* package */ PingMessage(@JsonProperty("uuid") final String uuid) {
    super(Message.MessageType.ASSIGN_UUID, uuid);
  }

}
