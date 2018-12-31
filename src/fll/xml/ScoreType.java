/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

/**
 * @author jpschewe
 */
public enum ScoreType {
  INTEGER, FLOAT;


  public String toXmlString() {
    // case for XML and enum value are the same
    return toString();
  }

}
