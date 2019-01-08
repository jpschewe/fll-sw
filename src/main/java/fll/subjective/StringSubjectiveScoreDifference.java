/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

/**
 * A difference in string values.
 */
public class StringSubjectiveScoreDifference extends SubjectiveScoreDifference {

  public StringSubjectiveScoreDifference(final String category,
                                         final String subcategory,
                                         final int teamNumber,
                                         final String judge,
                                         final String masterValue,
                                         final String compareValue) {
    super(category, subcategory, teamNumber, judge);
    this.masterValue = masterValue;
    this.compareValue = compareValue;
  }

  private final String masterValue;

  /**
   * The value from the master document.
   * 
   * @return The value or null if not found.
   */
  public final String getMasterValue() {
    return masterValue;

  }

  private final String compareValue;

  /**
   * The value from the compare document.
   * 
   * @return The value or null if not found.
   */
  public final String getCompareValue() {
    return compareValue;
  }

}
