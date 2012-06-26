/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

/**
 * If the winner should be a high score or a low score.
 * 
 * @author jpschewe
 */
public enum WinnerType {
  HIGH("MAX", "DESC"), LOW("MIN", "ASC");
  
  private final String mMinMaxString;
  private final String mSortString;
  
  private WinnerType(final String minMaxString, final String sortString) {
    mMinMaxString = minMaxString;
    mSortString = sortString;
  }
  /**
   * 
   * @return string for the SQL function that gets the right score (MAX or MIN).
   */
  public String getMinMaxString() {
    return mMinMaxString;
  }
  
  /**
   * 
   * @return SQL ASC or DESC
   */
  public String getSortString() {
    return mSortString;
  }
}
