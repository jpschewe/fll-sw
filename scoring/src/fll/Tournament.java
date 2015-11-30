/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import fll.db.GenerateDB;
import fll.util.LogUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * The representation of a tournament. If someone changes the database, this
 * object does not notice the changes. It's a snapshot in time from when the
 * object was created.
 */
public final class Tournament implements Serializable {

  private static final Logger LOGGER = LogUtils.getLogger();

  private Tournament(@JsonProperty("tournamentID") final int tournamentID,
                     @JsonProperty("name") final String name,
                     @JsonProperty("location") final String location) {
    this.tournamentID = tournamentID;
    this.name = name;
    this.location = location;
  }

  private final int tournamentID;

  public int getTournamentID() {
    return tournamentID;
  }

  private final String name;

  public String getName() {
    return name;
  }

  private final String location;

  public String getLocation() {
    return location;
  }

  /**
   * Create a tournament without a next tournament.
   */
  public static void createTournament(final Connection connection,
                                      final String tournamentName,
                                      final String location) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location) VALUES (?, ?)");
      prep.setString(1, tournamentName);
      prep.setString(2, location);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }
  
  /**
   * Get a list of tournaments in the DB ordered by Name. This excludes the
   * internal tournament.
   * 
   * @return list of tournament tournaments
   */
  public static List<Tournament> getTournaments(final Connection connection) throws SQLException {
    final List<Tournament> retval = new LinkedList<Tournament>();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT tournament_id, Name, Location FROM Tournaments WHERE tournament_id <> ? ORDER BY Name");
      prep.setInt(1, GenerateDB.INTERNAL_TOURNAMENT_ID);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int tournamentID = rs.getInt(1);
        final String name = rs.getString(2);
        final String location = rs.getString(3);

        final Tournament tournament = new Tournament(tournamentID, name, location);
        retval.add(tournament);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return retval;
  }

  /**
   * Create a {@link Tournament} for the tournament with the specified name.
   * 
   * @param connection the database connection
   * @param name the name of a tournament to find
   * @return the Tournament, or null if it cannot be found
   * @throws SQLException
   */
  public static Tournament findTournamentByName(final Connection connection,
                                                final String name) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT tournament_id, Location FROM Tournaments WHERE Name = ?");
      prep.setString(1, name);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int id = rs.getInt(1);
        final String location = rs.getString(2);
        return new Tournament(id, name, location);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Create a {@link Tournament} for the tournament with the specified id.
   * 
   * @param connection the database connection
   * @param tournamentID the ID to find
   * @return the Tournament, or null if it cannot be found
   * @throws SQLException
   */
  public static Tournament findTournamentByID(final Connection connection,
                                              final int tournamentID) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Name, Location FROM Tournaments WHERE tournament_id = ?");
      prep.setInt(1, tournamentID);
      rs = prep.executeQuery();
      if (rs.next()) {
        final String name = rs.getString(1);
        final String location = rs.getString(2);
        return new Tournament(tournamentID, name, location);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  public boolean equals(final Object o) {
    if (o instanceof Tournament) {
      final Tournament other = (Tournament) o;
      return other.getTournamentID() == getTournamentID();
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getTournamentID();
  }

  @Override
  public String toString() {
    return getName()
        + "(" + getTournamentID() + ") - " + getLocation();
  }

  /**
   * Check if this tournament needs the score summarization code to run.
   * 
   * @param tournamentID the tournament to check
   * @return true if an update is needed
   * @throws SQLException
   */
  public boolean checkTournamentNeedsSummaryUpdate(final Connection connection) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT performance_seeding_modified, subjective_modified, summary_computed" //
          + " FROM Tournaments" //
          + " WHERE tournament_id = ?");
      prep.setInt(1, getTournamentID());
      rs = prep.executeQuery();
      if (rs.next()) {
        Timestamp performanceSeedingModified = rs.getTimestamp(1);
        if (rs.wasNull()) {
          performanceSeedingModified = null;
        }
        Timestamp subjectiveModified = rs.getTimestamp(2);
        if (rs.wasNull()) {
          subjectiveModified = null;
        }
        Timestamp summaryComputed = rs.getTimestamp(3);
        if (rs.wasNull()) {
          summaryComputed = null;
        }

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("performance: "
              + performanceSeedingModified);
          LOGGER.trace("subjective: "
              + subjectiveModified);
          LOGGER.trace("summary: "
              + summaryComputed);
        }

        if (null == summaryComputed) {
          // never computed
          return true;
        } else if (null == performanceSeedingModified
            && null == subjectiveModified) {
          // computed and nothing has changed
          return false;
        } else if (null == performanceSeedingModified) {
          // subjective may have changed and have computed
          return summaryComputed.before(subjectiveModified);
        } else if (null == subjectiveModified) {
          return summaryComputed.before(performanceSeedingModified);
        } else {
          // nothing is null
          return summaryComputed.before(subjectiveModified)
              || summaryComputed.before(performanceSeedingModified);
        }

      } else {
        return true;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Note that the performance seeding rounds have been modified for this
   * tournament.
   * 
   * @throws SQLException
   */
  public void recordPerformanceSeedingModified(final Connection connection) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Tournaments" //
          + " SET performance_seeding_modified = CURRENT_TIMESTAMP" //
          + " WHERE tournament_id = ?");
      prep.setInt(1, getTournamentID());
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Note that the subjective scores have been modified for this
   * tournament.
   * 
   * @throws SQLException
   */
  public void recordSubjectiveModified(final Connection connection) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Tournaments" //
          + " SET subjective_modified = CURRENT_TIMESTAMP" //
          + " WHERE tournament_id = ?");
      prep.setInt(1, getTournamentID());
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Note that the summarized scores have been computed for this tournament.
   * 
   * @throws SQLException
   */
  public void recordScoreSummariesUpdated(final Connection connection) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Tournaments" //
          + " SET summary_computed = CURRENT_TIMESTAMP" //
          + " WHERE tournament_id = ?");
      prep.setInt(1, getTournamentID());
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

}
