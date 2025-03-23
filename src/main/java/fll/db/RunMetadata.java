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

  public enum RunType {
    /**
     * This is a practice round.
     */
    PRACTICE,
    /**
     * This is a regular match play round that counts towards the awards.
     */
    REGULAR_MATCH_PLAY,
    /**
     * Performance run that is for head to head.
     */
    HEAD_TO_HEAD,
    /**
     * Other performance run.
     */
    OTHER;
  }

  /**
   * @param runNumber {@link #getRunNumber()}
   * @param displayName {@link #getDisplayName()}
   * @param scoreboardDisplay {@link #isScoreboardDisplay()}
   * @param runType {@link #getRunType()}
   */
  public RunMetadata(final int runNumber,
                     final String displayName,
                     final boolean scoreboardDisplay,
                     final RunType runType) {
    this.runNumber = runNumber;
    this.displayName = displayName;
    this.scoreboardDisplay = scoreboardDisplay;
    this.runType = runType;
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
   * @return if true, display on the scoreboard
   */
  public boolean isScoreboardDisplay() {
    return scoreboardDisplay;
  }

  private boolean scoreboardDisplay;

  /**
   * @return {@link RunType#REGULAR_MATCH_PLAY}
   */
  public boolean isRegularMatchPlay() {
    return RunType.REGULAR_MATCH_PLAY.equals(getRunType());
  }

  /**
   * @return {@link RunType#PRACTICE}
   */
  public boolean isPractice() {
    return RunType.PRACTICE.equals(getRunType());
  }

  /**
   * @return {@link RunType#HEAD_TO_HEAD}
   */
  public boolean isHeadToHead() {
    return RunType.HEAD_TO_HEAD.equals(getRunType());
  }

  private final RunType runType;

  /**
   * @return type of run
   */
  public RunType getRunType() {
    return runType;
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
    try (PreparedStatement prep = connection.prepareStatement("SELECT display_name, scoreboard_display, run_type" //
        + " FROM run_metadata" //
        + " WHERE tournament_id = ? and run_number = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String displayName = castNonNull(rs.getString(1));
          final boolean scoreboardDisplay = rs.getBoolean(2);
          final String runTypeStr = castNonNull(rs.getString(3));
          final RunType runType = RunType.valueOf(runTypeStr);
          final RunMetadata runMetadata = new RunMetadata(runNumber, displayName, scoreboardDisplay, runType);
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
    return new RunMetadata(runNumber, String.format(TournamentSchedule.PERF_HEADER_FORMAT, runNumber), true,
                           runningHeadToHead ? RunType.HEAD_TO_HEAD : RunType.OTHER);
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
        + " USING (VALUES (?, ?, ?, ?, ?)) AS source(tournament_id, run_number, display_name, scoreboard_display, run_type)" //
        + "   ON run_metadata.tournament_id = source.tournament_id AND run_metadata.run_number = source.run_number" //
        + " WHEN MATCHED" //
        + "   THEN UPDATE SET run_metadata.display_name = source.display_name" //
        + "     ,run_metadata.scoreboard_display = source.scoreboard_display" //
        + "     ,run_metadata.run_type = source.run_type" //
        + " WHEN NOT MATCHED" //
        + "   THEN INSERT (tournament_id, run_number, display_name, scoreboard_display, run_type)" //
        + "     VALUES (source.tournament_id, source.run_number, source.display_name, source.scoreboard_display, source.run_type)" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, metadata.getRunNumber());
      prep.setString(3, metadata.getDisplayName());
      prep.setBoolean(4, metadata.isScoreboardDisplay());
      prep.setString(5, metadata.getRunType().name());
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
        PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM run_metadata WHERE tournament_id = ? AND run_type = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, RunType.REGULAR_MATCH_PLAY.name());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    return 0;
  }

}
