/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.util.FLLRuntimeException;

/**
 * Thrown when something cannot be found in a {@link GoalScope} or {@link VariableScope}.
 */
public class ScopeException extends FLLRuntimeException {

  public ScopeException(final String message) {
    super(message);
  }
}
