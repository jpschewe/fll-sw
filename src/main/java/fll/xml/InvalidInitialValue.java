/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Thrown when an invalid initial value is found in a challenge description.
 */
public class InvalidInitialValue extends ChallengeValidationException {

  /**
   * Default constructor.
   */
  public InvalidInitialValue() {
  }

  /**
   * @param message {@link #getMessage()}
   */
  public InvalidInitialValue(final String message) {
    super(message);
  }

  /**
   * @param cause {@link #getCause()}
   */
  public InvalidInitialValue(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message {@link #getMessage()}
   * @param cause {@link #getCause()}
   */
  public InvalidInitialValue(final String message,
                             final Throwable cause) {
    super(message, cause);
  }

}
