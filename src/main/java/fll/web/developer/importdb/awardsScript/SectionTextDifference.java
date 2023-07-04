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
import fll.db.AwardsScript.Section;
import fll.util.FLLInternalException;

/**
 * Difference in the text for a {@link Section} between 2 databases.
 */
public final class SectionTextDifference extends AwardsScriptDifference {

  /**
   * @param section see {@link #getSection()}
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  public SectionTextDifference(final Section section,
                               final String sourceValue,
                               final String destValue) {
    this.section = section;
    this.sourceValue = sourceValue;
    this.destValue = destValue;
  }

  @Override
  public String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append(String.format("<div>The text for section %s is different between the source database and the destination database.</div>",
                                     section));
    description.append(String.format("<div>Source: %s</div>", sourceValue));
    description.append(String.format("<div>Destination:%s </div>", destValue));
    return description.toString();
  }

  private final Section section;

  /**
   * @return the section that has a different value
   */
  public Section getSection() {
    return section;
  }

  private final String sourceValue;

  /**
   * @return macro value in the source database
   */
  public String getSourceValue() {
    return sourceValue;
  }

  private final String destValue;

  /**
   * @return macro value in the destination database
   */
  public String getDestValue() {
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
      AwardsScript.updateSectionTextForTournament(sourceConnection, sourceTournament, getSection(), getDestValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT:
      AwardsScript.updateSectionTextForTournament(destConnection, destTournament, getSection(), getSourceValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT_LEVEL:
      AwardsScript.updateSectionTextForTournamentLevel(destConnection, destTournament.getLevel(), getSection(),
                                                       getSourceValue());
      break;
    case KEEP_SOURCE_AS_SEASON:
      AwardsScript.updateSectionTextForSeason(destConnection, getSection(), getSourceValue());
      break;
    default:
      throw new FLLInternalException(String.format("Unknown enum value for %s: %s", action.getClass().getName(),
                                                   action.getName()));
    }
  }
}
