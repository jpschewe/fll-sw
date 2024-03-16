/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sent from the client to register the display.
 */
final class RegisterMessage extends Message {

  /**
   * @param displayUuid {@link #getDisplayUuid()}
   * @param bracketInfo {@link #getBracketInfo()}
   */
  RegisterMessage(@JsonProperty("displayUuid") final String displayUuid,
                  @JsonProperty("bracketInfo") Collection<BracketInfo> bracketInfo) {
    super(Message.MessageType.REGISTER);
    this.displayUuid = displayUuid;
    this.bracketInfo = bracketInfo;
  }

  private final String displayUuid;

  /**
   * @return UUID for the display
   */
  public String getDisplayUuid() {
    return this.displayUuid;
  }

  private final Collection<BracketInfo> bracketInfo;

  /**
   * @return information about the brackets on the display
   */
  public Collection<BracketInfo> getBracketInfo() {
    return bracketInfo;
  }
}
