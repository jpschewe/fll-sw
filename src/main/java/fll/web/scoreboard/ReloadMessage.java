/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

/**
 * Sent to tell the client to reload the page.
 */
final class ReloadMessage extends Message {

  ReloadMessage() {
    super(Message.MessageType.RELOAD);
  }

}
