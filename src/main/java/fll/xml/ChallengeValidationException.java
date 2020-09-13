/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.util.FLLRuntimeException;

/**
 * Thrown in response to an error doign additional validation of the challenge.
 */
public class ChallengeValidationException extends FLLRuntimeException {

  /**
   * Default constructor.
   */
  public ChallengeValidationException() {
    super();
  }

  /**
   * @param message {@link #getMessage()}
   */
  public ChallengeValidationException(final String message) {
    super(message);
  }

  /**
   * @param cause {@link #getCause()}
   */
  public ChallengeValidationException(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message {@link #getMessage()}
   * @param cause {@link #getCause()}
   */
  public ChallengeValidationException(final String message,
                                      final Throwable cause) {
    super(message, cause);
  }

}
