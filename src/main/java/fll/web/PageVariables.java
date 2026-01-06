/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import fll.TournamentTeam;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
import fll.util.FLLRuntimeException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Populate page context variables that are used on multiple pages.
 */
public final class PageVariables {

  private PageVariables() {
  }

  /**
   * Set the variable {@code completedRunMetadata} to the {@link List} of
   * {@link RunMetadata} in order of run number for the runs that have seen scores
   * in the current tournament.
   * 
   * @param application get application variables
   * @param pageContext set page variables
   */
  public static void populateCompletedRunData(final ServletContext application,
                                              final PageContext pageContext) {

    final List<RunMetadata> editMetadata = new LinkedList<>();

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final RunMetadataFactory runMetadataFactory = tournamentData.getRunMetadataFactory();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int maxRunNumber = Queries.getMaxRunNumberForTournament(connection, tournamentData.getCurrentTournament());
      if (maxRunNumber > 0) {
        for (int runNumber = 1; runNumber <= maxRunNumber; ++runNumber) {
          final RunMetadata runMetadata = runMetadataFactory.getRunMetadata(runNumber);
          editMetadata.add(runMetadata);
        }
      }
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }

    pageContext.setAttribute("completedRunMetadata", editMetadata);
  }

  /**
   * Set the variable {@code tournamentTeams} to the {@link java.util.Collection}
   * of
   * {@link fll.TournamentTeam} objects for the current tournament.
   * 
   * @param application get application variables
   * @param pageContext set page variables
   */
  public static void populateTournamentTeams(final ServletContext application,
                                             final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      pageContext.setAttribute("tournamentTeams", Queries.getTournamentTeams(connection).values());
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

}
