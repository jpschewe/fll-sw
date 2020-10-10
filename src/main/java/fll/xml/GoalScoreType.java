/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Used to specify which value to use from a goal.
 */
public enum GoalScoreType {

  RAW, COMPUTED;

  /**
   * @param str the string to convert
   * @return Defaults to {@link #RAW} if nothing matches.
   */
  public static GoalScoreType fromString(final String str) {
    if (COMPUTED.toString().equalsIgnoreCase(str)) {
      return COMPUTED;
    } else {
      return RAW;
    }
  }

  /**
   * @return the string to write to XML
   */
  public String toXmlString() {
    // XML is all lowercase
    return toString().toLowerCase();
  }

}
