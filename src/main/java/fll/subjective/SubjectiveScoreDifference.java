/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

/**
 * Tells of a difference between two score documents. The type should only be
 * {@link String} or {@link Double}.
 */
public abstract class SubjectiveScoreDifference {

  private final String category;

  public final String getCategory() {
    return category;
  }

  private final String subcategory;

  public final String getSubcategory() {
    return subcategory;
  }

  private final int teamNumber;

  public final int getTeamNumber() {
    return teamNumber;
  }

  private final String judge;

  public final String getJudge() {
    return judge;
  }

  /**
   * The value in the master document. Subclasses should override this with a
   * specific type.
   */
  public abstract Object getMasterValue();

  /**
   * The value in the compare document. Subclasses should override this with a
   * specific type.
   */
  public abstract Object getCompareValue();

  public SubjectiveScoreDifference(final String category, final String subcategory, final int teamNumber, final String judge) {
    this.category = category;
    this.subcategory = subcategory;
    this.teamNumber = teamNumber;
    this.judge = judge;
  }

}
