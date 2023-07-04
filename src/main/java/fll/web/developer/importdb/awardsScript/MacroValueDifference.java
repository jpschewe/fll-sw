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
import fll.db.AwardsScript.Macro;
import fll.util.FLLInternalException;

/**
 * Difference in a {@link Macro} value 2 databases.
 */
public final class MacroValueDifference extends AwardsScriptDifference {

  /**
   * @param macro see {@link #getMacro()}
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  public MacroValueDifference(final Macro macro,
                              final String sourceValue,
                              final String destValue) {
    this.macro = macro;
    this.sourceValue = sourceValue;
    this.destValue = destValue;
  }

  @Override
  public String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append(String.format("<div>The value of macro %s is different between the source database and the destination database.</div>",
                                     macro));
    description.append(String.format("<div>Source: %s</div>", sourceValue));
    description.append(String.format("<div>Destination:%s </div>", destValue));
    return description.toString();
  }

  private final Macro macro;

  /**
   * @return the macro that has a different value
   */
  public Macro getMacro() {
    return macro;
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
      AwardsScript.updateMacroValueForTournament(sourceConnection, sourceTournament, getMacro(), getDestValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT:
      AwardsScript.updateMacroValueForTournament(destConnection, destTournament, getMacro(), getSourceValue());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT_LEVEL:
      AwardsScript.updateMacroValueForTournamentLevel(destConnection, destTournament.getLevel(), getMacro(),
                                                      getSourceValue());
      break;
    case KEEP_SOURCE_AS_SEASON:
      AwardsScript.updateMacroValueForSeason(destConnection, getMacro(), getSourceValue());
      break;
    default:
      throw new FLLInternalException(String.format("Unknown enum value for %s: %s", action.getClass().getName(),
                                                   action.getName()));
    }
  }
}
