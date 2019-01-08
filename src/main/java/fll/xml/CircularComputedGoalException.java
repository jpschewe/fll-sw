/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.util.FLLRuntimeException;

/**
 * Thrown when the dependency tree of a computed goal has a loop.
 */
public class CircularComputedGoalException extends FLLRuntimeException {

  /**
   * @param message
   */
  public CircularComputedGoalException(final String message) {
    super(message);
  }

}
