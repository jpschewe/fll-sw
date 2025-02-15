/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fll.Team;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Gather information for
 * checkSeedingRoundsResult.jsp.
 */
public final class CheckSeedingRounds {

  private CheckSeedingRounds() {
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckSeedingRounds.class);

  /**
   * @param application for the datasource
   * @param page to store data
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection);

      final List<Team> less = Queries.getTeamsNeedingSeedingRuns(connection, tournamentTeams, true);
      page.setAttribute("teamsNeedingSeedingRounds", less);

    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database", e);
      throw new RuntimeException(e);
    }

  }

}
