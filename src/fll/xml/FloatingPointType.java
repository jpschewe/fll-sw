/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

public enum FloatingPointType {

  DECIMAL, ROUND, TRUNCATE;

  /**
   * Defaults to TRUNCATE if string doesn't match any known constant.
   */
  public static FloatingPointType fromString(final String str) {
    if (DECIMAL.toString().equalsIgnoreCase(str)) {
      return DECIMAL;
    } else if (ROUND.toString().equalsIgnoreCase(str)) {
      return ROUND;
    } else {
      return TRUNCATE;
    }
  }
  
  public String toXmlString() {
    // the XML always uses the lower case version
    return toString().toLowerCase();
  }

}
