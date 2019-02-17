/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Acknowledge a {@link PublishCommand}.
 */
@JsonIgnoreProperties(value={ "type" }, allowGetters=true)
public class PubAckResponse extends BaseMessage {

  /**
   * @param seq see {@link #getSeq()}
   */
  public PubAckResponse(@JsonProperty("seq") final int seq) {
    super(MhubMessageType.PUB_ACK_RESPONSE);
    this.seq = seq;
  }

  private final int seq;

  /**
   * @return the sequence number that is being acknowldged
   */
  public int getSeq() {
    return seq;
  }

}
