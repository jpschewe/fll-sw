/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

/**
 * Represents an internal error.
 */
public class FLLInternalException extends RuntimeException {

  /**
   * 
   */
  public FLLInternalException() {
    // TODO Auto-generated constructor stub
  }

  /**
   * @param message
   */
  public FLLInternalException(final String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param cause
   */
  public FLLInternalException(final Throwable cause) {
    super(cause);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param message
   * @param cause
   */
  public FLLInternalException(final String message, final Throwable cause) {
    super(message, cause);
    // TODO Auto-generated constructor stub
  }

}
