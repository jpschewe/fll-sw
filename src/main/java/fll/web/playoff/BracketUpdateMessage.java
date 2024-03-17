/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent to the client to state that the information displayed in the
 * brackets is changing.
 */
final class BracketUpdateMessage extends Message {

  BracketUpdateMessage(@JsonProperty("bracketUpdate") final BracketUpdate bracketUpdate) {
    super(Message.MessageType.BRACKET_UPDATE);
    this.bracketUpdate = bracketUpdate;
  }

  public BracketUpdate getBracketUpdate() {
    return bracketUpdate;
  }

  private final BracketUpdate bracketUpdate;

}