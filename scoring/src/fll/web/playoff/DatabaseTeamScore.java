/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;

/**
 * TeamScore implementation for a score in the database.
 * 
 * @author jpschewe
 * @version $Revision$
 */
/* package */class DatabaseTeamScore extends TeamScore {

  private static final Logger LOG = Logger.getLogger(DatabaseTeamScore.class);

  /**
   * Create a database team score object. This method also updates the team
   * score totals.
   * 
   * @param connection the connection to get the data from
   * @param document the challenge document
   * @param team the team the score is for
   * @param runNumber the run the score is for
   * @throws SQLException if an error occurs updating the team score totals
   * @throws ParseException if an error occurs updating the team score totals
   * @see Queries#updateScoreTotals(Document, Connection);
   */
  public DatabaseTeamScore(final Connection connection, final Document document, final Team team, final int runNumber) throws SQLException, ParseException {
    super(team, runNumber);
    
    // make sure scores are up to date
    Queries.updateScoreTotals(document, connection);

    _connection = connection;
    _tournament = Queries.getCurrentTournament(_connection);
  }

  /**
   * @see fll.web.playoff.TeamScore#getEnumScore(java.lang.String)
   */
  public String getEnumScore(final String goalName) {
    if (null == _result) {
      throw new RuntimeException("No score is present");
    } else {
      try {
        return _result.getString(goalName);
      } catch (final SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#getIntScore(java.lang.String)
   */
  public int getIntScore(final String goalName) {
    if (null == _result) {
      throw new RuntimeException("No score is present");
    } else {
      try {
        return _result.getInt(goalName);
      } catch (final SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#getTotalScore()
   */
  public int getTotalScore() {
    try {
      return Playoff.getPerformanceScore(_connection, _tournament, getTeam(), getRunNumber());
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#isNoShow()
   */
  public boolean isNoShow() {
    try {
      return Playoff.isNoShow(_connection, _tournament, getTeam(), getRunNumber());
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#scoreExists()
   */
  public boolean scoreExists() {
    try {
      return Playoff.performanceScoreExists(_connection, getTeam(), getRunNumber());
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  public void cleanup() {
    Utilities.closeResultSet(_result);
    _result = null;
  }
  
  /**
   * Cleanup resources.
   */
  @Override
  public void finalize() {
    cleanup();
  }

  private final Connection _connection;

  private ResultSet _result = null;

  private final String _tournament;
}
