/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

/**
 * Sent when a score is deleted.
 */
final class DeleteMessage extends Message {
  /* package */ DeleteMessage() {
    super(Message.MessageType.DELETE);
  }

}
