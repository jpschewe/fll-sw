/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent to the client to state that the displayed brackets are changing.
 */
final class DisplayUpdateMessage extends Message {

  /**
   * @param allBracketInfo {@link #getAllBracketInfo()}
   */
  DisplayUpdateMessage(@JsonProperty("allBracketInfo") final List<BracketInfo> allBracketInfo) {
    super(Message.MessageType.DISPLAY_UPDATE);
    this.allBracketInfo = allBracketInfo;
  }

  /**
   * @return which brackets should be displayed
   */
  public List<BracketInfo> getAllBracketInfo() {
    return allBracketInfo;
  }

  private final List<BracketInfo> allBracketInfo;

}