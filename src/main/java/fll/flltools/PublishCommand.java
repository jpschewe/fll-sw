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

/**
 * Base class for all publish messages
 */
public abstract class PublishCommand extends BaseMessage implements SequenceNumberCommand {

  /**
   * The type is set to "publish".
   * The sequence number is 0, it should be set by the messaging infrastructure.
   * 
   * @param topic see {@link #getTopic()}
   * @param node see {@link #getNode()}
   */
  public PublishCommand(@Nonnull final String topic,
                        @Nonnull final String node) {
    super(MhubMessageType.PUBLISH_COMMAND);
    this.topic = topic;
    this.node = node;
    this.seq = 0;
  }

  private final String topic;

  /**
   * @return the topic for the message
   */
  public String getTopic() {
    return topic;
  }

  /**
   * Subclasses override this for their payload.
   * 
   * @return the payload of the message
   */
  public abstract Object getData();

  private final String node;

  /**
   * @return the node to send to. May be null.
   */
  public String getNode() {
    return node;
  }

  private int seq;

  /**
   * @return sequence number for the message
   */
  @Override
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
  @Override
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
