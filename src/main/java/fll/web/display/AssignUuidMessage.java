/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sent to the client to specify the UUID.
 */
/* package */ final class AssignUuidMessage extends UuidMessage {

  /**
   * @param uuid {@link #getUuid()}
   */
  /* package */ AssignUuidMessage(@JsonProperty("uuid") final String uuid) {
    super(Message.MessageType.ASSIGN_UUID, uuid);
  }

}
