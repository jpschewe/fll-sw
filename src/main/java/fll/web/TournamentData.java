/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
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
        try (Connection connection = datasource.getConnection()) {
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
      try (Connection connection = datasource.getConnection()) {
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

    final Integer runNumberObj = Integer.valueOf(runNumber);

    if (runMetadata.containsKey(runNumberObj)) {
      return runMetadata.get(runNumberObj);
    } else {
      try (Connection connection = datasource.getConnection()) {
        final RunMetadata data = RunMetadata.getFromDatabase(connection, getCurrentTournament(), runNumber);
        runMetadata.put(runNumberObj, data);
        return data;
      } catch (final SQLException e) {
        throw new FLLInternalException("Unable to get run metadata from database", e);
      }
    }

  }
}
