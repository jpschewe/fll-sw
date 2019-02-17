/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.util.FLLRuntimeException;

/**
 * Thrown when an invalid initial value is found in a challenge description.
 */
public class InvalidInitialValue extends FLLRuntimeException {

  public InvalidInitialValue() {
  }

  public InvalidInitialValue(final String message) {
    super(message);
  }

  public InvalidInitialValue(final Throwable cause) {
    super(cause);
  }

  public InvalidInitialValue(final String message,
                             final Throwable cause) {
    super(message, cause);
  }

}
