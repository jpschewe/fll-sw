/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The base class for mhub messages.
 */
public abstract class BaseMessage {

  /**
   * Creates message with sequence number 0.
   * 
   * @param topic see {@link #getTopic()}
   * @param type see {@link #getType()}
   * @param node see {@link #getNode()}
   */
  public BaseMessage(@Nonnull @JsonProperty("topic") final String topic,
                     @Nonnull @JsonProperty("type") final String type,
                     @Nonnull @JsonProperty("node") final String node) {
    this(topic, type, node, 0);
  }

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
   * Comes back from the server for an error processing a message.
   */
  public static final String TYPE_ERROR = "error";
  
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

  private int seq;

  /**
   * @return sequence number for the message
   */
  public int getSeq() {
    return seq;
  }

  /**
   * Change the sequence number. Most messages will be constructed with a
   * sequence number of 0 and {@link MhubMessageHandler} will set the sequence
   * number before sending.
   * 
   * @param seq the new sequence number
   */
  public void setSeq(final int seq) {
    this.seq = seq;
  }

  private final Map<String, String> headers = new HashMap<>();

  /**
   * Replace the headers with the new value.
   * 
   * @param v the new headers to set
   * @see #getHeaders()
   */
  public void setHeaders(final Map<String, String> v) {
    headers.clear();
    headers.putAll(v);
  }

  /**
   * Add a header.
   * 
   * @param key the key to add
   * @param value the value to add
   */
  public void addHeader(final String key,
                        final String value) {
    headers.put(key, value);
  }

  /**
   * @return headers for the messsage, defaults to empty
   */
  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(headers);
  }

}
