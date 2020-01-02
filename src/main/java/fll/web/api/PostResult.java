/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.util.Optional;

/**
 * Generic result of a post.
 */
/* package */ class PostResult {

  /* package */ PostResult(final boolean success,
                           final Optional<String> message) {
    mSuccess = success;
    mMessage = message;
  }

  private final boolean mSuccess;

  /**
   * @return true if the post was successful
   */
  public boolean getSuccess() {
    return mSuccess;
  }

  private final Optional<String> mMessage;

  /**
   * @return a message to report to the user
   */
  public Optional<String> getMessage() {
    return mMessage;
  }

}
