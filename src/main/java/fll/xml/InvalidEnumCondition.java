/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Thrown when an enum condition in a challenge description computed goal is
 * invalid.
 * This typically occurs when a non-enumerated goal is referenced inside an enum
 * condition.
 */
public class InvalidEnumCondition extends ChallengeValidationException {

  /**
   * Default constructor.
   */
  public InvalidEnumCondition() {
  }

  /**
   * @param message {@link #getMessage()}
   */
  public InvalidEnumCondition(final String message) {
    super(message);
  }

  /**
   * @param cause {@link #getCause()}
   */
  public InvalidEnumCondition(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message {@link #getMessage()}
   * @param cause {@link #getCause()}
   */
  public InvalidEnumCondition(final String message,
                              final Throwable cause) {
    super(message, cause);
  }

}
