/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

/**
 * Types of scores.
 */
public enum ScoreType {
  INTEGER, FLOAT;

  /**
   * @return XML representation
   */
  public String toXmlString() {
    // case for XML and enum value are the same
    return toString();
  }

}
