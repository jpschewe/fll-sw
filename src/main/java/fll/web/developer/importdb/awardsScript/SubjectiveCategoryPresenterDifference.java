/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import java.sql.Connection;
import java.sql.SQLException;

import fll.Tournament;
import fll.db.AwardsScript;
import fll.util.FLLInternalException;
import fll.xml.SubjectiveScoreCategory;

/**
 * Difference in the presenter for a subjective category between 2 databases.
 */
public class SubjectiveCategoryPresenterDifference extends CategoryPresenterDifference {

  /**
   * @param category see {@link #getCategory()}
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  public SubjectiveCategoryPresenterDifference(final SubjectiveScoreCategory category,
                                               final String sourceValue,
                                               final String destValue) {
    super(category, sourceValue, destValue);
    this.category = category;
  }

  private final SubjectiveScoreCategory category;

  @Override
  public SubjectiveScoreCategory getCategory() {
    return category;
  }

  @Override
  public void resolveDifference(final Connection sourceConnection,
                                final Tournament sourceTournament,
                                final Connection destConnection,
                                final Tournament destTournament,
                                final AwardsScriptDifferenceAction action)
      throws SQLException {
    switch (action) {
    case KEEP_DESTINATION:
      AwardsScript.updatePresenterForTournament(sourceConnection, sourceTournament, getCategory(), getDestValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT:
      AwardsScript.updatePresenterForTournament(destConnection, destTournament, getCategory(), getSourceValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT_LEVEL:
      AwardsScript.updatePresenterForTournamentLevel(destConnection, destTournament.getLevel(), getCategory(),
                                                     getSourceValue());
      break;
    case KEEP_SOURCE_AS_SEASON:
      AwardsScript.updatePresenterForSeason(destConnection, getCategory(), getSourceValue());
      break;
    default:
      throw new FLLInternalException(String.format("Unknown enum value for %s: %s", action.getClass().getName(),
                                                   action.getName()));
    }
  }

}
