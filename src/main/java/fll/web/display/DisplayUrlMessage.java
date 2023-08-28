/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

/**
 * Sent from server to client to specify the url to display.
 */
/* package */ final class DisplayUrlMessage extends Message {

  /**
   * @param url {@link #getUrl()}
   */
  /* package */ DisplayUrlMessage(final String url) {
    super(Message.MessageType.DISPLAY_URL);
    this.url = url;
  }

  private final String url;

  /**
   * @return the URL that the client is to display
   */
  public String getUrl() {
    return url;
  }

}
