/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fll.Tournament;

/**
 * Utilities for working with delaying the display of performance scores.
 */
public final class DelayedPerformance {

  private DelayedPerformance() {
  }

  /**
   * @param connection database connection
   * @param tournament the tournament to deal with
   * @return the maximum run number to display
   * @throws SQLException on a database error
   */
  public static int getMaxRunNumberToDisplay(final Connection connection,
                                             final Tournament tournament)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT run_number" //
        + " FROM delayed_performance" //
        + " WHERE tournament_id = ?" //
        + " AND delayed_until > CURRENT_TIMESTAMP" //
        + " ORDER BY run_number ASC" //
        + " LIMIT 1" //
    )) {
      prep.setInt(1, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int runNumber = rs.getInt("run_number");
          // display all run numbers before this one
          return runNumber
              - 1;
        } else {
          // display all runs
          return Integer.MAX_VALUE;
        }
      } // result set

    } // prepared statement

  }

}
