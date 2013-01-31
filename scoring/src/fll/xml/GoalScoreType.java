/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

public enum GoalScoreType {

  RAW, COMPUTED;

  /**
   * Defaults to "RAW" if nothing matches.
   */
  public static GoalScoreType fromString(final String str) {
    if ("computed".equalsIgnoreCase(str)) {
      return COMPUTED;
    } else {
      return RAW;
    }
  }

}
