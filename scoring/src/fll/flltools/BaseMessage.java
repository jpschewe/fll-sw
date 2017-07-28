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
 */
public abstract class BaseMessage {

  /**
   * @param topic see {@link #getTopic()}
   */
  public BaseMessage(@Nonnull @JsonProperty("topic") final String topic) {
    this.topic = topic;

  }

  private final String topic;

  /**
   * @return the topic for the message
   */
  public String getTopic() {
    return topic;
  }

  public abstract Object getData();

}
