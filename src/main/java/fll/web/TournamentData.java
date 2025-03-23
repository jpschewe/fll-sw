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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import fll.Tournament;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
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
    this.runMetadataFactory = null;
  }

  private final DataSource datasource;

  private @MonotonicNonNull RunMetadataFactory runMetadataFactory;

  /**
   * @return {@link RunMetadataFactory} for the current tournament
   */
  public RunMetadataFactory getRunMetadataFactory() {
    synchronized (this) {
      if (null == runMetadataFactory) {
        runMetadataFactory = new RunMetadataFactory(getDataSource(), getCurrentTournament());
      }
      return runMetadataFactory;
    }
  }

  /**
   * @return database datasource
   */
  public DataSource getDataSource() {
    return datasource;
  }

  private @MonotonicNonNull Tournament currentTournament;

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

}
