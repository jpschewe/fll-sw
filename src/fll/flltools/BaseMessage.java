/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The base class for mhub messages.
 * 
 */
public abstract class BaseMessage {

  /**
   * @param type see {@link #getType()}
   */
  public BaseMessage(@Nonnull @JsonProperty("type") final MhubMessageType type) {
    this.type = type;
  }

  private final MhubMessageType type;

  /**
   * @return the type of message, typically "publish" or "subscribe"
   */
  public MhubMessageType getType() {
    return type;
  }

}
