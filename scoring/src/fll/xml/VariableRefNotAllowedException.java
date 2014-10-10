/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.util.FLLRuntimeException;

/**
 * Thrown when a variable reference is found where it's not expected.
 */
public class VariableRefNotAllowedException extends FLLRuntimeException {

  public VariableRefNotAllowedException() {
  }

  public VariableRefNotAllowedException(final String message) {
    super(message);
  }

  public VariableRefNotAllowedException(final Throwable cause) {
    super(cause);
  }

  public VariableRefNotAllowedException(final String message,
                                        final Throwable cause) {
    super(message, cause);
  }

}
