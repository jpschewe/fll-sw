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
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.TournamentLevel.NoSuchTournamentLevelException;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.admin.Tournaments;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * The representation of a tournament. If someone changes the database, this
 * object does not notice the changes. It's a snapshot in time from when the
 * object was created.
 */
public final class Tournament implements Serializable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private Tournament(@JsonProperty("tournamentID") final int tournamentID,
                     @JsonProperty("name") final String name,
                     @JsonProperty("description") final @Nullable String description,
                     @JsonProperty("date") final @Nullable LocalDate date,
                     @JsonProperty("level") final TournamentLevel level) {
    this.tournamentID = tournamentID;
    this.name = name;
    this.description = description;
    this.date = date;
    this.level = level;
  }

  private final int tournamentID;

  /**
   * @return database ID for the tournament
   */
  public int getTournamentID() {
    return tournamentID;
  }

  private final String name;

  /**
   * @return the {@link #getDescription()} if not null, otherwise
   *         {@link #getName()}
   */
  @JsonIgnore
  public String getTitle() {
    if (null == description) {
      return name;
    } else {
      return description;
    }
  }

  /**
   * @return a short name for the tournament
   */
  public String getName() {
    return name;
  }

  private final @Nullable String description;

  /**
   * @return a longer description of the tournament
   */
  public @Nullable String getDescription() {
    return description;
  }

  private final @Nullable LocalDate date;

  /**
   * @return date of the tournament
   */
  public @Nullable LocalDate getDate() {
    return date;
  }

  /**
   * @return {@link #getDate()} formatted for HTML date input type
   * @see Tournaments#DATE_FORMATTER
   */
  @JsonIgnore
  public String getDateString() {
    return null == date ? "" : Tournaments.DATE_FORMATTER.format(date);
  }

  private final TournamentLevel level;

  /**
   * @return the level of the tournament
   */
  public TournamentLevel getLevel() {
    return level;
  }

  /**
   * Create a tournament.
   * 
   * @param connection database connection
   * @param tournamentName see {@link #getName()}
   * @param description see {@link #getDescription()}
   * @param date see {@link #getDate()}
   * @param level see {@link #getLevel()}
   * @throws SQLException on a database error
   */
  public static void createTournament(final Connection connection,
                                      final String tournamentName,
                                      final @Nullable String description,
                                      final @Nullable LocalDate date,
                                      final TournamentLevel level)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location, tournament_date, level_id) VALUES (?, ?, ?, ?)")) {
      prep.setString(1, tournamentName);
      prep.setString(2, description);
      if (null == date) {
        prep.setNull(3, Types.DATE);
      } else {
        prep.setDate(3, java.sql.Date.valueOf(date));
      }
      prep.setInt(4, level.getId());
      prep.executeUpdate();
    }
  }

  /**
   * Get a list of tournaments in the DB ordered by date then location. This
   * excludes the
   * internal tournament.
   * 
   * @param connection database connection
   * @return list of tournament tournaments, excludes the internal tournament
   * @throws SQLException on a database error
   */
  public static List<Tournament> getTournaments(final Connection connection) throws SQLException {
    final List<Tournament> retval = new LinkedList<Tournament>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT tournament_id, Name, Location, tournament_date, level_id FROM Tournaments WHERE tournament_id <> ? ORDER BY tournament_date, location ")) {
      prep.setInt(1, GenerateDB.INTERNAL_TOURNAMENT_ID);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int tournamentID = rs.getInt(1);
          final String name = castNonNull(rs.getString(2));
          final String location = rs.getString(3);
          final java.sql.Date d = rs.getDate(4);
          final LocalDate date = null == d ? null : d.toLocalDate();

          final int levelId = rs.getInt(5);
          try {
            final TournamentLevel level = TournamentLevel.getById(connection, levelId);

            final Tournament tournament = new Tournament(tournamentID, name, location, date, level);
            retval.add(tournament);
          } catch (final NoSuchTournamentLevelException e) {
            throw new FLLInternalException("Error looking up level for tournament '"
                + name
                + "'", e);
          }
        }
      }
    }
    return retval;
  }

  /**
   * @param connection the database connection
   * @return the current tournament
   * @throws SQLException on a database error
   */
  public static Tournament getCurrentTournament(final Connection connection) throws SQLException {
    final int currentTournamentID = Queries.getCurrentTournament(connection);
    final Tournament currentTournament = Tournament.findTournamentByID(connection, currentTournamentID);
    return currentTournament;
  }

  /**
   * Create a {@link Tournament} for the tournament with the specified name.
   * 
   * @param connection the database connection
   * @param name the name of a tournament to find
   * @return the Tournament
   * @throws SQLException on a database error
   * @throws UnknownTournamentException if the tournament cannot be found
   */
  public static Tournament findTournamentByName(final Connection connection,
                                                final String name)
      throws SQLException, UnknownTournamentException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT tournament_id, Location, tournament_date, level_id FROM Tournaments WHERE Name = ?")) {
      prep.setString(1, name);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int id = rs.getInt(1);
          final String location = rs.getString(2);
          final java.sql.Date d = rs.getDate(3);
          final LocalDate date = null == d ? null : d.toLocalDate();
          final int levelId = rs.getInt(4);

          try {
            final TournamentLevel level = TournamentLevel.getById(connection, levelId);

            return new Tournament(id, name, location, date, level);
          } catch (final NoSuchTournamentLevelException e) {
            throw new FLLInternalException("Error looking up level for tournament '"
                + name
                + "'", e);
          }
        } else {
          throw new UnknownTournamentException(name);
        }
      }
    }
  }

  /**
   * @param connection database connection
   * @param id tournament id
   * @return if a tournament with the specified {@code id} exists
   * @throws SQLException on a database error
   */
  public static boolean doesTournamentExist(final Connection connection,
                                            final int id)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT Name FROM Tournaments WHERE tournament_id = ?")) {
      prep.setInt(1, id);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * @param connection database connection
   * @param name tournament name
   * @return if a tournament with the specified {@code name} exists
   * @throws SQLException on a database error
   */
  public static boolean doesTournamentExist(final Connection connection,
                                            final String name)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT Name FROM Tournaments WHERE Name = ?")) {
      prep.setString(1, name);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * Create a {@link Tournament} for the tournament with the specified id.
   * 
   * @param connection the database connection
   * @param tournamentID the ID to find
   * @return the Tournament
   * @throws SQLException on a database error
   * @throws UnknownTournamentException when the tournament is not found
   */
  public static Tournament findTournamentByID(final Connection connection,
                                              final int tournamentID)
      throws SQLException, UnknownTournamentException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT Name, Location, tournament_date, level_id FROM Tournaments WHERE tournament_id = ?")) {
      prep.setInt(1, tournamentID);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final String location = rs.getString(2);
          final java.sql.Date d = rs.getDate(3);
          final LocalDate date = null == d ? null : d.toLocalDate();
          final int levelId = rs.getInt(4);
          try {
            final TournamentLevel level = TournamentLevel.getById(connection, levelId);
            return new Tournament(tournamentID, name, location, date, level);
          } catch (final NoSuchTournamentLevelException e) {
            throw new FLLInternalException("Error looking up level for tournament '"
                + name
                + "'", e);
          }

        } else {
          throw new UnknownTournamentException(tournamentID);
        }
      }
    }
  }

  @Override
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (o.getClass().equals(this.getClass())) {
      final Tournament other = (Tournament) o;
      return other.getTournamentID() == getTournamentID();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getTournamentID();
  }

  @Override
  public String toString() {
    return getName()
        + " ("
        + getTournamentID()
        + ") - "
        + getDescription();
  }

  /**
   * Check if this tournament needs the score summarization code to run.
   * 
   * @param connection database connection
   * @return true if an update is needed
   * @throws SQLException on a database error
   */
  public boolean checkTournamentNeedsSummaryUpdate(final Connection connection) throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT performance_seeding_modified, subjective_modified, summary_computed" //
            + " FROM Tournaments" //
            + " WHERE tournament_id = ?")) {
      prep.setInt(1, getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
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
            // subjective may have changed
            return null == subjectiveModified
                || summaryComputed.before(subjectiveModified);
          } else if (null == subjectiveModified) {
            // performance may have changed
            return summaryComputed.before(performanceSeedingModified);
          } else {
            // nothing is null
            return summaryComputed.before(subjectiveModified)
                || summaryComputed.before(performanceSeedingModified);
          }
        } else {
          return true;
        }
      }
    }
  }

  /**
   * Note that the performance seeding rounds have been modified for this
   * tournament.
   * 
   * @param connection database connection
   * @throws SQLException on a database error
   */
  public void recordPerformanceSeedingModified(final Connection connection) throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("UPDATE Tournaments" //
        + " SET performance_seeding_modified = CURRENT_TIMESTAMP" //
        + " WHERE tournament_id = ?")) {
      prep.setInt(1, getTournamentID());
      prep.executeUpdate();
    }
  }

  /**
   * Note that the subjective scores have been modified for this
   * tournament.
   * 
   * @param connection database connection
   * @throws SQLException on a database error
   */
  public void recordSubjectiveModified(final Connection connection) throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("UPDATE Tournaments" //
        + " SET subjective_modified = CURRENT_TIMESTAMP" //
        + " WHERE tournament_id = ?")) {
      prep.setInt(1, getTournamentID());
      prep.executeUpdate();
    }
  }

  /**
   * Note that the summarized scores have been computed for this tournament.
   * 
   * @param connection database connection
   * @throws SQLException on a database error
   */
  public void recordScoreSummariesUpdated(final Connection connection) throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("UPDATE Tournaments" //
        + " SET summary_computed = CURRENT_TIMESTAMP" //
        + " WHERE tournament_id = ?")) {
      prep.setInt(1, getTournamentID());
      prep.executeUpdate();
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
                                final String table)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM "
        + table
        + " WHERE tournament = ?")) {
      prep.setInt(1, getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int count = rs.getInt(1);
          if (count > 0) {
            return true;
          }
        }
      }
      // no scores found
      return false;
    }
  }

  /**
   * Check if this tournament contains scores.
   * 
   * @param connection the database connection
   * @param description the challenge description to get the subjective
   *          categories
   * @return true if there are any performance or subjective scores in this
   *         tournament
   * @throws SQLException on a database error
   */
  public boolean containsScores(final Connection connection,
                                final ChallengeDescription description)
      throws SQLException {
    if (scoresInTable(connection, "performance")) {
      return true;
    }

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      if (scoresInTable(connection, category.getName())) {
        return true;
      }
    }

    // no scores found
    return false;
  }

  /**
   * Update the information for a tournament.
   * 
   * @param tournamentID which tournament to modify
   * @param name see {@link #getName()}
   * @param description see {@link #getDescription()}
   * @param date see {@link #getDate()}
   * @param level see {@link #getLevel()}
   * @param connection database connection
   * @throws SQLException on a database error
   */
  public static void updateTournament(final Connection connection,
                                      final int tournamentID,
                                      final String name,
                                      final @Nullable String description,
                                      final @Nullable LocalDate date,
                                      final TournamentLevel level)
      throws SQLException {
    try (PreparedStatement updatePrep = connection.prepareStatement("UPDATE Tournaments SET" //
        + " Name = ?" //
        + ", Location = ?" //
        + ", tournament_date = ?" //
        + ", level_id = ?" //
        + " WHERE tournament_id = ?")) {
      updatePrep.setString(1, name);
      if (null != description) {
        updatePrep.setString(2, description);
      } else {
        updatePrep.setNull(2, Types.VARCHAR);
      }
      if (null == date) {
        updatePrep.setNull(3, Types.DATE);
      } else {
        updatePrep.setDate(3, java.sql.Date.valueOf(date));
      }
      updatePrep.setInt(4, level.getId());
      updatePrep.setInt(5, tournamentID);
      updatePrep.executeUpdate();
    }
  }

  /**
   * Delete a tournament.
   * This will delete a tournament if there are no scores associated with it.
   * 
   * @param connection database connection
   * @param tournamentID tournament to delete
   * @throws SQLException If there is a database error such as scores existing.
   */
  public static void deleteTournament(final Connection connection,
                                      final int tournamentID)
      throws SQLException {
    try (PreparedStatement deleteTables = connection.prepareStatement("DELETE FROM TableNames WHERE tournament = ?")) {
      deleteTables.setInt(1, tournamentID);
      deleteTables.executeUpdate();
    }

    try (PreparedStatement deleteJudges = connection.prepareStatement("DELETE FROM judges WHERE tournament = ?")) {
      deleteJudges.setInt(1, tournamentID);
      deleteJudges.executeUpdate();
    }

    try (
        PreparedStatement deleteSchedulePerf = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE tournament = ?")) {
      deleteSchedulePerf.setInt(1, tournamentID);
      deleteSchedulePerf.executeUpdate();
    }

    try (
        PreparedStatement deleteScheduleSubj = connection.prepareStatement("DELETE FROM sched_subjective WHERE tournament = ?")) {
      deleteScheduleSubj.setInt(1, tournamentID);
      deleteScheduleSubj.executeUpdate();
    }

    try (PreparedStatement deleteSchedule = connection.prepareStatement("DELETE FROM schedule WHERE tournament = ?")) {
      deleteSchedule.setInt(1, tournamentID);
      deleteSchedule.executeUpdate();
    }

    try (
        PreparedStatement deleteTournamentParameters = connection.prepareStatement("DELETE FROM tournament_parameters WHERE tournament = ?")) {
      deleteTournamentParameters.setInt(1, tournamentID);
      deleteTournamentParameters.executeUpdate();
    }

    try (
        PreparedStatement deleteTournamentTeams = connection.prepareStatement("DELETE FROM TournamentTeams WHERE tournament = ?")) {
      deleteTournamentTeams.setInt(1, tournamentID);
      deleteTournamentTeams.executeUpdate();
    }

    try (
        PreparedStatement deleteTournament = connection.prepareStatement("DELETE FROM Tournaments WHERE tournament_id = ?")) {
      deleteTournament.setInt(1, tournamentID);
      deleteTournament.executeUpdate();
    }
  }

}
