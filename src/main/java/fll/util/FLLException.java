/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

/**
 * Base checked exception for FLL-SW.
 */
public class FLLException extends Exception {

  /**
   * @param message {@link #getMessage()}
   */
  public FLLException(final String message) {
    super(message);
  }

  /**
   * @param cause {@link #getCause()}
   */
  public FLLException(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message {@link #getMessage()}
   * @param cause {@link #getCause()}
   */
  public FLLException(final String message,
                      final Throwable cause) {
    super(message, cause);
  }

}
