/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import fll.util.FLLRuntimeException;

/**
 * Used for inconsistent scheduler parameters.
 */
public class InconsistentSchedParams extends FLLRuntimeException {

  /**
   * Base constructor.
   */
  public InconsistentSchedParams() {
    super();
  }

  /**
   * @param message {@link #getMessage()}
   */
  public InconsistentSchedParams(final String message) {
    super(message);
  }

  /**
   * @param cause {@link #getCause()}
   */
  public InconsistentSchedParams(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message {@link #getMessage()}
   * @param cause {@link #getCause()}
   */
  public InconsistentSchedParams(final String message,
                                 final Throwable cause) {
    super(message, cause);
  }

}
