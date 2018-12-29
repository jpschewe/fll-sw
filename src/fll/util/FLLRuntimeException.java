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
   * 
   */
  public FLLRuntimeException() {
    super();
  }

  /**
   * @param message
   */
  public FLLRuntimeException(final String message) {
    super(message);
  }

  /**
   * @param cause
   */
  public FLLRuntimeException(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message
   * @param cause
   */
  public FLLRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
