/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem.list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Send an array to the list module to display.
 */
public class SetArray extends BaseListMessage {

  public SetArray() {
    super("default", "setArray");
  }

  /**
   * @return {@link SetArray#getPayload()}
   */
  @Override
  public Object getData() {
    return getPayload();
  }

  private Payload payload = null;

  /**
   * @param payload the payload for the message
   */
  @JsonIgnore
  public void setPayload(final Payload payload) {
    this.payload = payload;
  }

  /**
   * @return the payload for the message
   */
  @JsonIgnore
  public Payload getPayload() {
    return payload;
  }

  /**
   * The data field for the {@link SetArray} message.
   * 
   * @see SetArray#getData()
   */
  public static final class Payload {
    public Payload(final String header, final List<List<String>> data) {
      this.header = header;
      this.data = data;
    }

    private final String header;

    /**
     * @return the header for the list
     */
    public String getHeader() {
      return header;
    }
    
    private final List<List<String>> data;
    /**
     * @return the data to be displayed, 2-dimensional array
     */
    public List<List<String>> getData() {
      return data;
    }
    
  }

}
