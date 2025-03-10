/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.util.FLLInternalException;

/**
 * Cache tournament data for use as an application variable.
 */
public class TournamentData {

  /**
   * Initialize all caches to empty.
   * 
   * @param datasource {@link #getDataSource()}
   */
  public TournamentData(final DataSource datasource) {
    this.datasource = datasource;
    this.currentTournament = null;
  }

  private final DataSource datasource;

  /**
   * @return database datasource
   */
  public DataSource getDataSource() {
    return datasource;
  }

  private @Nullable Tournament currentTournament;

  /**
   * @return the current tournament
   */
  public Tournament getCurrentTournament() {
    synchronized (this) {
      if (null == currentTournament) {
        try (Connection connection = getDataSource().getConnection()) {
          currentTournament = Tournament.getCurrentTournament(connection);
          return currentTournament;
        } catch (final SQLException e) {
          throw new FLLInternalException("Unable to get current tournament", e);
        }
      } else {
        return currentTournament;
      }
    }
  }

  /**
   * Set the current tournament and cache it.
   * 
   * @param tournament the new tournament
   */
  public void setCurrentTournament(final Tournament tournament) {
    synchronized (this) {
      try (Connection connection = getDataSource().getConnection()) {
        Queries.setCurrentTournament(connection, tournament.getTournamentID());
        currentTournament = tournament;

        runMetadata.clear();
      } catch (final SQLException e) {
        throw new FLLInternalException("Unable to get current tournament", e);
      }
    }
  }

  private final Map<Integer, RunMetadata> runMetadata = new HashMap<>();

  /**
   * Get run metadata from cache or database.
   * 
   * @param runNumber run we are interested in
   * @return the metadata
   */
  public RunMetadata getRunMetadata(final int runNumber) {
    synchronized (this) {
      final Integer runNumberObj = Integer.valueOf(runNumber);

      if (runMetadata.containsKey(runNumberObj)) {
        return runMetadata.get(runNumberObj);
      } else {
        try (Connection connection = getDataSource().getConnection()) {
          final RunMetadata data = RunMetadata.getFromDatabase(connection, getCurrentTournament(), runNumber);
          runMetadata.put(runNumberObj, data);
          return data;
        } catch (final SQLException e) {
          throw new FLLInternalException("Unable to get run metadata from database", e);
        }
      }
    }
  }

  /**
   * @return the metadata for all runs that any team has completed, sorted by
   *         round
   */
  public List<RunMetadata> getAllRunMetadata() {
    final List<RunMetadata> allMetadata = new LinkedList<>();
    try (Connection connection = getDataSource().getConnection()) {
      final int maxPerfRounds = Queries.getMaxRunNumber(connection, getCurrentTournament());
      final int maxMetadataRound = RunMetadata.getMaxRunNumber(connection, getCurrentTournament());

      for (int round = 1; round <= Math.max(maxPerfRounds, maxMetadataRound); ++round) {
        final RunMetadata metadata = getRunMetadata(round);
        allMetadata.add(metadata);
      }
    } catch (final SQLException e) {
      throw new FLLInternalException("Error getting maximum number of performance rounds", e);
    }
    return allMetadata;
  }

  /**
   * Store run metadata and update the cache.
   * 
   * @param metadata the new meta data
   */
  public void storeRunMetadata(final RunMetadata metadata) {
    synchronized (this) {
      // clear cache on write
      this.runMetadata.put(metadata.getRunNumber(), metadata);

      try (Connection connection = getDataSource().getConnection()) {
        RunMetadata.storeToDatabase(connection, getCurrentTournament(), metadata);
      } catch (final SQLException e) {
        throw new FLLInternalException("Error storing run metadata to database", e);
      }
    }
  }

  public void deleteRunMetadata(final int runNumber) {
    synchronized (this) {
      this.runMetadata.remove(runNumber);

      try (Connection connection = getDataSource().getConnection()) {
        RunMetadata.deleteFromDatabase(connection, getCurrentTournament(), runNumber);
      } catch (final SQLException e) {
        throw new FLLInternalException("Error delete run metadata", e);
      }

    }
  }
}
