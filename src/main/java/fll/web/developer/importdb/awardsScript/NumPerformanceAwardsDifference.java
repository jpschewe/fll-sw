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

/**
 * Difference in the number of performance awards between 2 databases.
 */
public final class NumPerformanceAwardsDifference extends AwardsScriptDifference {

  /**
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  public NumPerformanceAwardsDifference(final int sourceValue,
                                        final int destValue) {
    this.sourceValue = sourceValue;
    this.destValue = destValue;
  }

  @Override
  public String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append("<div>The number of performance awards is different between the source database and the destination database.</div>");
    description.append(String.format("<div>Source: %s</div>", sourceValue));
    description.append(String.format("<div>Destination:%s </div>", destValue));
    return description.toString();
  }

  private final int sourceValue;

  /**
   * @return num performance awards in the source database
   */
  public int getSourceValue() {
    return sourceValue;
  }

  private final int destValue;

  /**
   * @return num performance awards in the destination database
   */
  public int getDestValue() {
    return destValue;
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
      AwardsScript.updateNumPerformanceAwardsForTournament(sourceConnection, sourceTournament, getDestValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT:
      AwardsScript.updateNumPerformanceAwardsForTournament(destConnection, destTournament, getSourceValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT_LEVEL:
      AwardsScript.updateNumPerformanceAwardsForTournamentLevel(destConnection, destTournament.getLevel(),
                                                                getSourceValue());
      break;
    case KEEP_SOURCE_AS_SEASON:
      AwardsScript.updateNumPerformanceAwardsForSeason(destConnection, getSourceValue());
      break;
    default:
      throw new FLLInternalException(String.format("Unknown enum value for %s: %s", action.getClass().getName(),
                                                   action.getName()));
    }
  }
}
