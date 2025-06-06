/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import fll.Tournament;
import fll.db.GlobalParameters;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Gather parameter information for edit_global_parameters.jsp.
 */
public final class GatherParameterInformation {

  private GatherParameterInformation() {
  }

  /**
   * @param application get application variables
   * @param pageContext set variables for the page
   * @throws SQLException on a database error
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext)
      throws SQLException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      pageContext.setAttribute("tournaments", tournaments);

      pageContext.setAttribute("performanceAdvancementPercentage_default",
                               TournamentParameters.getDefaultPerformanceAdvancementPercentage(connection));
      final Map<Integer, Integer> performanceAdvancementPercentage = new HashMap<>();
      for (final Tournament tournament : tournaments) {
        if (TournamentParameters.tournamentParameterValueExists(connection, tournament.getTournamentID(),
                                                                TournamentParameters.PERFORMANCE_ADVANCEMENT_PERCENTAGE)) {
          performanceAdvancementPercentage.put(tournament.getTournamentID(),
                                               TournamentParameters.getPerformanceAdvancementPercentage(connection,
                                                                                                        tournament.getTournamentID()));
        }
      }
      pageContext.setAttribute("performanceAdvancementPercentage", performanceAdvancementPercentage);

      pageContext.setAttribute("gDivisionFlipRate",
                               GlobalParameters.getIntGlobalParameter(connection, GlobalParameters.DIVISION_FLIP_RATE));

      pageContext.setAttribute("gAllTeamsScrollRate", GlobalParameters.getAllTeamScrollRate(connection));
      pageContext.setAttribute("gHeadToHeadSecondsPerRow", GlobalParameters.getHeadToHeadScrollRate(connection));
    }
  }
}
