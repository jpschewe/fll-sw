/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;

/**
 * Gather parameter information for edit_tournament_parameters.jsp.
 */
public class GatherTournamentParameterInformation {

  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext)
      throws SQLException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      pageContext.setAttribute("tournament", tournament);

      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());
      pageContext.setAttribute("numSeedingRounds", numSeedingRounds);

      if (TournamentParameters.getRunningHeadToHead(connection, tournament.getTournamentID())) {
        pageContext.setAttribute("runningHeadToHeadChecked", "checked");
      } else {
        pageContext.setAttribute("runningHeadToHeadChecked", "");
      }
      final int maxPerformanceRoundEntered = Queries.maxPerformanceRunNumberCompleted(connection);
      pageContext.setAttribute("runningHeadToHeadDisabled", maxPerformanceRoundEntered > numSeedingRounds);

      pageContext.setAttribute("numSeedingSoundsDisabled", maxPerformanceRoundEntered > 0);

      pageContext.setAttribute("performanceAdvancementPercentage",
                               TournamentParameters.getPerformanceAdvancementPercentage(connection,
                                                                                        tournament.getTournamentID()));

    }
  }
}
