/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.autosched;

import fll.util.FLLRuntimeException;

/**
 * Used for inconsistent scheduler parameters.
 */
public class InconsistentSchedParams extends FLLRuntimeException {

  public InconsistentSchedParams() {
    super();
  }

  public InconsistentSchedParams(final String message) {
    super(message);
  }

  public InconsistentSchedParams(final Throwable cause) {
    super(cause);
  }

  public InconsistentSchedParams(final String message,
                                 final Throwable cause) {
    super(message, cause);
  }

}
