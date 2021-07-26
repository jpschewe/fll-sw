/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;
import fll.web.playoff.Playoff;

/**
 * Gather parameter information for edit_tournament_parameters.jsp.
 */
public final class GatherTournamentParameterInformation {

  private GatherTournamentParameterInformation() {
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
      final List<String> playoffBrackets = Playoff.getPlayoffBrackets(connection, tournament.getTournamentID());
      final boolean runningHeadToHeadDisabled = maxPerformanceRoundEntered > numSeedingRounds
          || !playoffBrackets.isEmpty();
      pageContext.setAttribute("runningHeadToHeadDisabled", runningHeadToHeadDisabled);

      pageContext.setAttribute("numSeedingSoundsDisabled", maxPerformanceRoundEntered > 0);

      pageContext.setAttribute("performanceAdvancementPercentage",
                               TournamentParameters.getPerformanceAdvancementPercentage(connection,
                                                                                        tournament.getTournamentID()));

    }
  }
}
