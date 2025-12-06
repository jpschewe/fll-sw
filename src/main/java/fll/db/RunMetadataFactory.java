/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import fll.Tournament;
import fll.util.FLLInternalException;

/**
 * Create {@link RunMetadata} objects for a tournament. This information is
 * cached.
 */
public class RunMetadataFactory {

  /**
   * Initialize the cache to empty.
   * 
   * @param datasource {@link #getDataSource()}
   * @param tournament {@link #getTournament()}
   */
  public RunMetadataFactory(final DataSource datasource,
                            final Tournament tournament) {
    this.datasource = datasource;
    this.rournament = tournament;
  }

  private final DataSource datasource;

  /**
   * @return database datasource
   */
  public DataSource getDataSource() {
    return datasource;
  }

  private final Tournament rournament;

  /**
   * @return the tournament for this cache
   */
  public Tournament getTournament() {
    return rournament;
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
          final RunMetadata data = RunMetadata.getFromDatabase(connection, getTournament(), runNumber);
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
      final int maxPerfRounds = Queries.getMaxRunNumberForTournament(connection, getTournament());
      final int maxMetadataRound = RunMetadata.getMaxRunNumber(connection, getTournament());

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
   * @return regular match play run metadata
   * @see #getAllRunMetadata()
   */
  public List<RunMetadata> getRegularMatchPlayRunMetadata() {
    return getAllRunMetadata().stream() //
                              .filter(RunMetadata::isRegularMatchPlay) //
                              .toList();
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
        RunMetadata.storeToDatabase(connection, getTournament(), metadata);
      } catch (final SQLException e) {
        throw new FLLInternalException("Error storing run metadata to database", e);
      }
    }
  }

  /**
   * Delete run metadata and update the cache.
   * 
   * @param runNumber the run number to delete the metadata for
   */
  public void deleteRunMetadata(final int runNumber) {
    synchronized (this) {
      this.runMetadata.remove(runNumber);

      try (Connection connection = getDataSource().getConnection()) {
        RunMetadata.deleteFromDatabase(connection, getTournament(), runNumber);
      } catch (final SQLException e) {
        throw new FLLInternalException("Error delete run metadata", e);
      }

    }
  }
}
