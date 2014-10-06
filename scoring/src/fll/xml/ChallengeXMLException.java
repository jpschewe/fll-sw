/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.util.FLLRuntimeException;

/**
 * Thrown when there is an XML error parsing a challenge description.
 */
public class ChallengeXMLException extends FLLRuntimeException {

  public ChallengeXMLException() {
  }

  public ChallengeXMLException(final String message) {
    super(message);
  }

  public ChallengeXMLException(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message
   * @param cause
   */
  public ChallengeXMLException(final String message,
                                 final Throwable cause) {
    super(message, cause);
  }

}
