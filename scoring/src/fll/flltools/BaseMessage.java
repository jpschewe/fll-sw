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
   * @param type see {@link #getType()}
   * @param node see {@link #getNode()}
   * @param seq see {@link #getSeq()}
   */
  public BaseMessage(@Nonnull @JsonProperty("topic") final String topic,
                     @Nonnull @JsonProperty("type") final String type,
                     @Nonnull @JsonProperty("node") final String node,
                     @JsonProperty("seq") final int seq) {
    this.topic = topic;
    this.type = type;
    this.node = node;
    this.seq = seq;
  }

  private final String topic;

  /**
   * @return the topic for the message
   */
  public String getTopic() {
    return topic;
  }

  public abstract Object getData();

  private final String type;

  /**
   * The type for publish messages
   */
  public static final String TYPE_PUBLISH = "publish";
  /**
   * The type for subscribe messages.
   */
  public static final String TYPE_SUBSCRIBE = "subscribe";
  /**
   * The type for generic messages, typically errors.
   */
  public static final String TYPE_MESSAGE = "message";
  /**
   * Comes back from the server in response to a publish message.
   */
  public static final String TYPE_PUBLISH_ACK = "puback";

  /**
   * @return the type of message, typically "publish" or "subscribe"
   */
  public String getType() {
    return type;
  }

  private final String node;

  /**
   * @return the node to send to or subscribe to. May be null.
   * @see #getType()
   */
  public String getNode() {
    return node;
  }

  private final int seq;

  /**
   * @return sequence number for the message
   */
  public int getSeq() {
    return seq;
  }

}
