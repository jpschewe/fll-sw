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
public final class MacroValueDifference extends StringDifference {

  /**
   * @param macro see {@link #getMacro()}
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  public MacroValueDifference(final Macro macro,
                              final String sourceValue,
                              final String destValue) {
    super(sourceValue, destValue);
    this.macro = macro;
  }

  protected String getTextDescription() {
    return String.format("value of macro %s", getMacro());
  }

  private final Macro macro;

  /**
   * @return the macro that has a different value
   */
  public Macro getMacro() {
    return macro;
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
