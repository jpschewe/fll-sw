/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fll.Tournament;
import fll.db.AwardsScript;
import fll.util.FLLInternalException;
import fll.web.report.awards.AwardCategory;

/**
 * Difference in the award order between 2 databases.
 */
public final class AwardOrderDifference extends AwardsScriptDifference {

  /**
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  public AwardOrderDifference(final List<AwardCategory> sourceValue,
                              final List<AwardCategory> destValue) {
    this.sourceValue = Collections.unmodifiableList(new ArrayList<>(sourceValue));
    this.destValue = Collections.unmodifiableList(new ArrayList<>(destValue));
  }

  @Override
  public String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append("<div>The order of awards is different between the source database and the destination database.</div>");
    description.append("<div>Source:<ul>");
    for (final AwardCategory svalue : sourceValue) {
      description.append("<li>");
      description.append(svalue.getTitle());
      description.append("</li>");
    }
    description.append("</ul></div>");
    description.append("<div>Destination:<ul>");
    for (final AwardCategory dvalue : destValue) {
      description.append("<li>");
      description.append(dvalue.getTitle());
      description.append("</li>");
    }
    description.append("</ul></div>");
    return description.toString();
  }

  private final List<AwardCategory> sourceValue;

  /**
   * @return award in the source database (unmodifiable)
   */
  public List<AwardCategory> getSourceValue() {
    return sourceValue;
  }

  private final List<AwardCategory> destValue;

  /**
   * @return award in the destination database (unmodifiable)
   */
  public List<AwardCategory> getDestValue() {
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
      AwardsScript.updateAwardOrderForTournament(sourceConnection, sourceTournament, getDestValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT:
      AwardsScript.updateAwardOrderForTournament(destConnection, destTournament, getSourceValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT_LEVEL:
      AwardsScript.updateAwardOrderForTournamentLevel(destConnection, destTournament.getLevel(), getSourceValue());
      break;
    case KEEP_SOURCE_AS_SEASON:
      AwardsScript.updateAwardOrderForSeason(destConnection, getSourceValue());
      break;
    default:
      throw new FLLInternalException(String.format("Unknown enum value for %s: %s", action.getClass().getName(),
                                                   action.getName()));
    }
  }
}
