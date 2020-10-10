/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

/**
 * Base Runtime Exception for FLL.
 */
public class FLLRuntimeException extends RuntimeException {

  /**
   * Base constructor.
   */
  public FLLRuntimeException() {
    super();
  }

  /**
   * @param message {@link #getMessage()}
   */
  public FLLRuntimeException(final String message) {
    super(message);
  }

  /**
   * @param cause {@link #getCause()}
   */
  public FLLRuntimeException(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message {@link #getMessage()}
   * @param cause {@link #getCause()}
   */
  public FLLRuntimeException(final String message,
                             final Throwable cause) {
    super(message, cause);
  }

}
