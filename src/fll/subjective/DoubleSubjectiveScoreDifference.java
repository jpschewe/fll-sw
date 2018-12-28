/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

/**
 * A difference with double values. 
 */
public class DoubleSubjectiveScoreDifference extends SubjectiveScoreDifference {

  public DoubleSubjectiveScoreDifference(final String category,
                                   final String subcategory,
                                   final int teamNumber,
                                   final String judge,
                                   final Double masterValue,
                                   final Double compareValue) {
    super(category, subcategory, teamNumber, judge);
    this.masterValue = masterValue;
    this.compareValue = compareValue;
}
  
  private final Double masterValue;

  /**
   * The value from the master document. 
   * @return The value or null if not found.
   */
  public final Double getMasterValue() {
    return masterValue;

  }

  private final Double compareValue;

  /**
   * The value from the compare document. 
   * @return The value or null if not found.
   */
  public final Double getCompareValue() {
    return compareValue;
  }
  
}
