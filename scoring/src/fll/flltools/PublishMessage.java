/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for all publish messages
 */
public abstract class PublishMessage extends BaseMessage {
  
  /**
   * The type is set to "publish".
   * 
   * @param topic passed to parent
   * @param node passed to parent
   * @param seq passed to parent
   * @see BaseMessage#BaseMessage(String, String, String, int)
   */
  public PublishMessage(@Nonnull @JsonProperty("topic") final String topic,
                        @Nonnull @JsonProperty("node") final String node,
                        @JsonProperty("seq") final int seq) {
    super(topic, TYPE_PUBLISH, node, seq);
  }

}
