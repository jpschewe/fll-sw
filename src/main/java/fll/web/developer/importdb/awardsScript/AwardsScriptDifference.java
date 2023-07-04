/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import java.sql.Connection;
import java.sql.SQLException;

import fll.Tournament;

/**
 * Base class for differences in the awards script between two databases.
 */
public abstract class AwardsScriptDifference {

  /**
   * @return human readable description of the difference. This value can contain
   *         HTML tags.
   */
  public abstract String getDescription();

  /**
   * Resolve the difference using the specified action.
   * 
   * @param sourceConnection source database
   * @param sourceTournament source tournament
   * @param destConnection destination database
   * @param destTournament destination tournament
   * @param action the action to take on the difference
   * @throws SQLException on a database error
   */
  public abstract void resolveDifference(Connection sourceConnection,
                                         Tournament sourceTournament,
                                         Connection destConnection,
                                         Tournament destTournament,
                                         AwardsScriptDifferenceAction action)
      throws SQLException;
}
