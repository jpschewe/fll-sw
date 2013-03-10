/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.db.Queries;

/**
 * The schedule for finalist judging.
 */
public class FinalistSchedule {

  public FinalistSchedule(final int tournament,
                          final Collection<FinalistDBRow> schedule) {
    this.mTournament = tournament;
    this.mSchedule = schedule;
  }

  private final Collection<FinalistDBRow> mSchedule;

  private final int mTournament;

  public int getTournament() {
    return mTournament;
  }

  /**
   * Store the schedule to the database. Remove any finalist schedule existing
   * for the tournament.
   * 
   * @param connection
   * @throws SQLException
   */
  public void store(final Connection connection) throws SQLException {
    PreparedStatement deletePrep = null;
    PreparedStatement insertPrep = null;
    try {
      deletePrep = connection.prepareStatement("DELETE FROM finalist_schedule WHERE tournament = ?");
      deletePrep.setInt(1, getTournament());
      deletePrep.executeUpdate();

      insertPrep = connection.prepareStatement("INSERT INTO finalist_schedule (tournament, category, judge_time, team_number) VALUES(?, ?, ?, ?)");
      insertPrep.setInt(1, getTournament());
      for (final FinalistDBRow row : mSchedule) {
        insertPrep.setString(2, row.getCategoryName());
        insertPrep.setTime(3, Queries.dateToTime(row.getTime()));
        insertPrep.setInt(4, row.getTeamNumber());
        insertPrep.executeUpdate();
      }

    } catch (final SQLException e) {
      SQLFunctions.close(deletePrep);
      SQLFunctions.close(insertPrep);
    }
  }

}
