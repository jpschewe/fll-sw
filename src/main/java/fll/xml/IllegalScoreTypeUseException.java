/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Thrown when a score type is used improperly such as "raw" used on enumerated
 * or computed goals.
 */
public class IllegalScoreTypeUseException extends ChallengeValidationException {

  /**
   * @param message {@link #getMessage()}
   */
  public IllegalScoreTypeUseException(final String message) {
    super(message);
  }

}
