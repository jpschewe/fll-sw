/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An error from the server.
 */
@JsonIgnoreProperties(value = { "type" }, allowGetters = true)
public class ErrorResponse extends BaseMessage {

  /**
   * Use null for the {@link #getSeq()}.
   * 
   * @param message passed to {@link #ErrorResponse(String, Integer)}
   */
  public ErrorResponse(@JsonProperty("message") final String message) {
    this(message, null);
  }

  /**
   * @param message see {@link #getMessage()}
   * @param seq see {@link #getSeq()}
   */
  public ErrorResponse(@JsonProperty("message") final String message,
                       @JsonProperty("seq") final Integer seq) {
    super(MhubMessageType.ERROR_RESPONSE);
    this.seq = seq;
    this.message = message;
  }

  private final Integer seq;

  /**
   * @return the sequence number, may be null
   */
  public Integer getSeq() {
    return seq;
  }

  private final String message;

  /**
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

}
