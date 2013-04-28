/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;


/**
 * A rank that a team has in a category.
 */
public class CategoryRank {

  public CategoryRank(final String group,
                      final String category,
                      final int rank,
                      final int numTeams) {
    mGroup = group;
    mCategory = category;
    mRank = rank;
    mNumTeams = numTeams;
  }

  private final String mCategory;

  public String getCategory() {
    return mCategory;
  }

  private final int mRank;

  public int getRank() {
    return mRank;
  }

  private final int mNumTeams;

  /**
   * Number of teams in the ranking.
   */
  public int getNumTeams() {
    return mNumTeams;
  }

  private final String mGroup;

  /**
   * Category name used for the overall rank.
   */
  public static final String OVERALL_CATEGORY_NAME = "Overall";

  /**
   * Category name used for the performance rank.
   */
  public static final String PERFORMANCE_CATEGORY_NAME = "Performance";

  /**
   * Rank for teams that didn't show up for a category.
   */
  public static final int NO_SHOW_RANK = -1;

  /**
   * The group that this ranking applies to.
   * This is usually the judging station for the team, but
   * may be something else if there is a different grouping such as performance
   * where this may be the division.
   */
  public String getGroup() {
    return mGroup;
  }
}
