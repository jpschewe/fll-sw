/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;

/**
 * Metadata for performance runs.
 */
public class RunMetadata {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param runNumber {@link #getRunNumber()}
   * @param displayName {@link #getDisplayName()}
   * @param regularMatchPlay {@link #isRegularMatchPlay()}
   * @param scoreboardDisplay {@link #isScoreboardDisplay()}
   * @param headToHead {@link #isHeadToHead()}
   */
  public RunMetadata(final int runNumber,
                     final String displayName,
                     final boolean regularMatchPlay,
                     final boolean scoreboardDisplay,
                     final boolean headToHead) {
    this.runNumber = runNumber;
    this.displayName = displayName;
    this.regularMatchPlay = regularMatchPlay;
    this.scoreboardDisplay = scoreboardDisplay;
    this.headToHead = headToHead;
  }

  /**
   * @return user-visible name of the run
   */
  public String getDisplayName() {
    return displayName;
  }

  private String displayName;

  /**
   * @return internal performance run number
   */
  public int getRunNumber() {
    return runNumber;
  }

  private int runNumber;

  /**
   * @return if true, score counts towards standard awards and advancement
   */
  public boolean isRegularMatchPlay() {
    return regularMatchPlay;
  }

  private boolean regularMatchPlay;

  /**
   * @return if true, display on the scoreboard
   */
  public boolean isScoreboardDisplay() {
    return scoreboardDisplay;
  }

  private boolean scoreboardDisplay;

  private final boolean headToHead;

  /**
   * @return true if this is a run in a head to head bracket
   */
  public boolean isHeadToHead() {
    return headToHead;
  }

  @Override
  public int hashCode() {
    return runNumber;
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (this.getClass().equals(o.getClass())) {
      return runNumber == ((RunMetadata) o).runNumber;
    } else {
      return false;
    }
  }

  /**
   * Load from the database. If not found, compute a value with
   * {@link #isRegularMatchPlay()} set to false and {@link #isScoreboardDisplay()}
   * set to true.
   * 
   * @param connection database
   * @param tournament tournament
   * @param runNumber internal performance run number
   * @return metadata for the run
   */
  public static RunMetadata getFromDatabase(final Connection connection,
                                            final Tournament tournament,
                                            final int runNumber) {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT display_name, regular_match_play, scoreboard_display, head_to_head" //
            + " FROM run_metadata" //
            + " WHERE tournament_id = ? and run_number = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String displayName = castNonNull(rs.getString(1));
          final boolean regularMatchPlay = rs.getBoolean(2);
          final boolean scoreboardDisplay = rs.getBoolean(3);
          final boolean head2Head = rs.getBoolean(4);
          final RunMetadata runMetadata = new RunMetadata(runNumber, displayName, regularMatchPlay, scoreboardDisplay,
                                                          head2Head);
          return runMetadata;
        }
      }
    } catch (final SQLException e) {
      LOGGER.error("Error getting run metadata from database, synthesizing best guess data", e);
    }

    boolean runningHeadToHead;
    try {
      runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection, tournament.getTournamentID());
    } catch (final SQLException e) {
      LOGGER.error("Error checking if tournament is running head to head, defaulting to false", e);
      runningHeadToHead = false;
    }

    // TODO what to do if not found or a database error
    // Synthesize it to be "Run #", regular=false,
    // scoreboard=true
    return new RunMetadata(runNumber, String.format(TournamentSchedule.PERF_HEADER_FORMAT, runNumber), false, true,
                           runningHeadToHead);
  }

  /**
   * @param connection database
   * @param tournament tournament
   * @param metadata what to save
   * @throws SQLException on a database error
   */
  public static void storeToDatabase(final Connection connection,
                                     final Tournament tournament,
                                     final RunMetadata metadata)
      throws SQLException {
    // update or insert based on tournament and run number
    try (PreparedStatement prep = connection.prepareStatement("MERGE INTO run_metadata" //
        + " USING (VALUES (?, ?, ?, ?, ?, ?)) AS source(tournament_id, run_number, display_name, regular_match_play, scoreboard_display, head_to_head)" //
        + "   ON run_metadata.tournament_id = source.tournament_id AND run_metadata.run_number = source.run_number" //
        + " WHEN MATCHED" //
        + "   THEN UPDATE SET run_metadata.display_name = source.display_name" //
        + "     ,run_metadata.regular_match_play = source.regular_match_play" //
        + "     ,run_metadata.scoreboard_display = source.scoreboard_display" //
        + "     ,run_metadata.head_to_head = source.head_to_head" //
        + " WHEN NOT MATCHED" //
        + "   THEN INSERT (tournament_id, run_number, display_name, regular_match_play, scoreboard_display, head_to_head)" //
        + "     VALUES (source.tournament_id, source.run_number, source.display_name, source.regular_match_play, source.scoreboard_display, source.head_to_head)" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, metadata.getRunNumber());
      prep.setString(3, metadata.getDisplayName());
      prep.setBoolean(4, metadata.isRegularMatchPlay());
      prep.setBoolean(5, metadata.isScoreboardDisplay());
      prep.setBoolean(6, metadata.isHeadToHead());
      prep.executeUpdate();
    }
  }

  /**
   * @param connection database
   * @param tournament tournament
   * @return maximum run number that metadata has been stored for
   * @throws SQLException on a database error
   */
  public static int getMaxRunNumber(final Connection connection,
                                    final Tournament tournament)
      throws SQLException {
    try (
        PreparedStatement findMax = connection.prepareStatement("SELECT MAX(run_number) FROM run_metadata WHERE tournament_id = ?")) {
      findMax.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = findMax.executeQuery()) {
        if (rs.next()) {
          final int maxKnown = rs.getInt(1);
          return maxKnown;
        } else {
          return 0;
        }
      }
    }
  }

  /**
   * @param connection database
   * @param tournament tournament
   * @param runNumber run number to check
   * @return true if the metadata for this run can potentially be deleted
   * @throws SQLException on a database error
   */
  public static boolean canDelete(final Connection connection,
                                  final Tournament tournament,
                                  final int runNumber)
      throws SQLException {

    // check that it isn't in the schedule
    try (
        PreparedStatement checkSchedule = connection.prepareStatement("SELECT MAX(c) FROM (SELECT COUNT(*) AS c FROM sched_perf_rounds WHERE tournament = ? GROUP BY team_number)")) {
      checkSchedule.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = checkSchedule.executeQuery()) {
        if (rs.next()) {
          final int maxSchedRound = rs.getInt(1);
          if (runNumber <= maxSchedRound) {
            LOGGER.warn("Cannot delete the metadata for a run defined in the schedule. Run: {} Schedule max: {}",
                        runNumber, maxSchedRound);
            return false;
          }
        }
      }
    }

    // check that it doesn't have performance scores
    try (
        PreparedStatement checkPerf = connection.prepareStatement("SELECT COUNT(*) FROM performance where tournament = ? AND runnumber = ?")) {
      checkPerf.setInt(1, tournament.getTournamentID());
      checkPerf.setInt(2, runNumber);
      try (ResultSet rs = checkPerf.executeQuery()) {
        if (rs.next()) {
          final int count = rs.getInt(1);
          if (count > 0) {
            LOGGER.warn("Cannot delete metadata for runs that already have scores. Run: {}", runNumber);
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * Delete metadata from the database as long as it's allowed.
   * Cannot delete if it's in the schedule, has performance scores or isn't the
   * maximum known metadata.
   * 
   * @param connection database
   * @param tournament tournament
   * @param runNumber run to delete metadata for
   * @throws SQLException on a database error
   * @throws FLLRuntimeException if the run cannot be deleted
   */
  public static void deleteFromDatabase(final Connection connection,
                                        final Tournament tournament,
                                        final int runNumber)
      throws SQLException, FLLRuntimeException {
    final boolean autoCommit = connection.getAutoCommit();

    try {
      connection.setAutoCommit(false);

      // check that this is the maximum run we know about
      final int maxKnown = getMaxRunNumber(connection, tournament);
      if (runNumber < maxKnown) {
        throw new FLLRuntimeException(String.format("Can only delete metadata for the maximum run known. Run: %d max run: %d",
                                                    runNumber, maxKnown));
      }

      if (!canDelete(connection, tournament, runNumber)) {
        throw new FLLRuntimeException(String.format("The metadata for run %d cannot be deleted and maintain consistency of the database",
                                                    runNumber));
      }

      try (
          PreparedStatement delete = connection.prepareStatement("DELETE FROM run_metadata WHERE tournament_id = ? AND run_number = ?")) {
        delete.setInt(1, tournament.getTournamentID());
        delete.setInt(2, runNumber);
        delete.executeUpdate();
      }

      connection.commit();
    } catch (final SQLException e) {
      connection.rollback();
      throw e;
    } finally {
      connection.setAutoCommit(autoCommit);
    }
  }

  /**
   * @param connection database
   * @param tournament tournament
   * @return number of regular match play rounds in the specified tournament
   * @throws SQLException on a database error
   */
  public static int getNumRegularMatchPlayRounds(final Connection connection,
                                                 final Tournament tournament)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM run_metadata WHERE tournament_id = ? AND regular_match_play IS TRUE")) {
      prep.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    return 0;
  }

}
