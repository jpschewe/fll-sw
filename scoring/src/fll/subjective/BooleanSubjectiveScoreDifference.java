/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

/**
 * A difference in boolean values.
 */
public class BooleanSubjectiveScoreDifference extends SubjectiveScoreDifference {

  public BooleanSubjectiveScoreDifference(final String category,
                                   final String subcategory,
                                   final int teamNumber,
                                   final String judge,
                                   final Boolean masterValue,
                                   final Boolean compareValue) {
    super(category, subcategory, teamNumber, judge);
    this.masterValue = masterValue;
    this.compareValue = compareValue;
}
  
  private final Boolean masterValue;

  /**
   * The value from the master document. 
   * @return The value or null if not found.
   */
  public final Boolean getMasterValue() {
    return masterValue;

  }

  private final Boolean compareValue;

  /**
   * The value from the compare document. 
   * @return The value or null if not found.
   */
  public final Boolean getCompareValue() {
    return compareValue;
  }
  
}
