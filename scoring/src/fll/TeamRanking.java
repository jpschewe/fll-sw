/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The ranking a a team has in all categories.
 */
public class TeamRanking {

  public TeamRanking(final int teamNumber) {
    mTeamNumber = teamNumber;
  }

  private final int mTeamNumber;

  public int getTeamNumber() {
    return mTeamNumber;
  }

  private final Map<String, CategoryRank> mRankings = new HashMap<String, CategoryRank>();

  public CategoryRank getRankForCategory(final String category) {
    return mRankings.get(category);
  }

  public void setRankForCategory(final String category,
                                 final CategoryRank rank) {
    mRankings.put(category, rank);
  }

  /**
   * Get the list of categories that this team has rankings for.
   * 
   * @return sorted by category name
   */
  public List<String> getCategories() {
    final List<String> retval = new LinkedList<String>(mRankings.keySet());
    Collections.sort(retval);
    return retval;
  }

}
