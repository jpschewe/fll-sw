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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
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
                     @JsonProperty("description") final String description) {
    this.tournamentID = tournamentID;
    this.name = name;
    this.description = description;
  }

  private final int tournamentID;

  public int getTournamentID() {
    return tournamentID;
  }

  private final String name;

  /**
   * 
   * @return a short name for the tournament
   */
  public String getName() {
    return name;
  }

  private final String description;

  /**
   * 
   * @return a longer description of the tournament
   */
  public String getDescription() {
    return description;
  }

  /**
   * Create a tournament without a next tournament.
   */
  public static void createTournament(final Connection connection,
                                      final String tournamentName,
                                      final String description) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location) VALUES (?, ?)");
      prep.setString(1, tournamentName);
      prep.setString(2, description);
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
        + " (" + getTournamentID() + ") - " + getDescription();
  }

  /**
   * Check if this tournament needs the score summarization code to run.
   * 
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

  /**
   * Check if there are scores in the specified table for this tournament.
   * 
   * @param connection database connection
   * @param table table name to check
   * @return true if there are scores for this tournament
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic based upon tables in the database")
  private boolean scoresInTable(final Connection connection,
                                final String table) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT COUNT(*) FROM "
          + table + " WHERE tournament = ?");
      prep.setInt(1, getTournamentID());
      rs = prep.executeQuery();
      if (rs.next()) {
        final int count = rs.getInt(1);
        if (count > 0) {
          return true;
        }
      }

      // no scores found
      return false;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check if this tournament contains scores.
   * 
   * @param connection
   * @param description the challenge description to get the subjective
   *          categories
   * @return true if there are any performance or subjective scores in this
   *         tournament
   * @throws SQLException
   */
  public boolean containsScores(final Connection connection,
                                final ChallengeDescription description) throws SQLException {
    if (scoresInTable(connection, "performance")) {
      return true;
    }

    for (final ScoreCategory category : description.getSubjectiveCategories()) {
      if (scoresInTable(connection, category.getName())) {
        return true;
      }
    }

    // no scores found
    return false;
  }

  /**
   * Delete a tournament.
   * This will delete a tournament if there are no scores associated with it.
   * 
   * @param connection
   * @param tournamentID tournament to delete
   * @throws SQLException If there is a database error such as scores existing.
   */
  public static void deleteTournament(final Connection connection,
                                      final int tournamentID) throws SQLException {
    PreparedStatement deleteTournament = null;
    PreparedStatement deleteTables = null;
    PreparedStatement deleteJudges = null;
    PreparedStatement deleteSchedule = null;
    PreparedStatement deleteSchedulePerf = null;
    PreparedStatement deleteScheduleSubj = null;
    PreparedStatement deleteTournamentParameters = null;
    PreparedStatement deleteTournamentTeams = null;
    try {
      deleteTables = connection.prepareStatement("DELETE FROM TableNames WHERE tournament = ?");
      deleteTables.setInt(1, tournamentID);
      deleteTables.executeUpdate();

      deleteJudges = connection.prepareStatement("DELETE FROM judges WHERE tournament = ?");
      deleteJudges.setInt(1, tournamentID);
      deleteJudges.executeUpdate();

      deleteSchedulePerf = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE tournament = ?");
      deleteSchedulePerf.setInt(1, tournamentID);
      deleteSchedulePerf.executeUpdate();

      deleteScheduleSubj = connection.prepareStatement("DELETE FROM sched_subjective WHERE tournament = ?");
      deleteScheduleSubj.setInt(1, tournamentID);
      deleteScheduleSubj.executeUpdate();

      deleteSchedule = connection.prepareStatement("DELETE FROM schedule WHERE tournament = ?");
      deleteSchedule.setInt(1, tournamentID);
      deleteSchedule.executeUpdate();

      deleteTournamentParameters = connection.prepareStatement("DELETE FROM tournament_parameters WHERE tournament = ?");
      deleteTournamentParameters.setInt(1, tournamentID);
      deleteTournamentParameters.executeUpdate();

      deleteTournamentTeams = connection.prepareStatement("DELETE FROM TournamentTeams WHERE tournament = ?");
      deleteTournamentTeams.setInt(1, tournamentID);
      deleteTournamentTeams.executeUpdate();

      deleteTournament = connection.prepareStatement("DELETE FROM Tournaments WHERE tournament_id = ?");
      deleteTournament.setInt(1, tournamentID);
      deleteTournament.executeUpdate();
    } finally {
      SQLFunctions.close(deleteTournament);
      SQLFunctions.close(deleteTables);
      SQLFunctions.close(deleteJudges);
      SQLFunctions.close(deleteSchedule);
      SQLFunctions.close(deleteSchedulePerf);
      SQLFunctions.close(deleteScheduleSubj);
      SQLFunctions.close(deleteTournamentParameters);
      SQLFunctions.close(deleteTournamentTeams);

    }
  }

}
