/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import fll.util.FLLRuntimeException;

/**
 * Thrown when a tournament cannnot be found.
 */
public class UnknownTournamentException extends FLLRuntimeException {

  /**
   * @param tournamentName name of the tournament that cannot be found
   */
  public UnknownTournamentException(final String tournamentName) {
    super("Cannot find tournament with name '"
        + tournamentName
        + "'");
  }

  /**
   * @param tournamentId id of the tournament that cannot be found
   */
  public UnknownTournamentException(final int tournamentId) {
    super("Cannot find tournament with id "
        + tournamentId);
  }

}
