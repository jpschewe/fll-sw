/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.report.finalist.FinalistSchedule;

/**
 * Populate the page context for the report index page.
 */
public final class ReportIndex {

  private ReportIndex() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param session session variables
   * @param application application context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {

    // clear out some variables
    session.removeAttribute(PromptSummarizeScores.SUMMARY_REDIRECT_KEY);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?")) {
        prep.setInt(1, Queries.getCurrentTournament(connection));

        try (ResultSet rs = prep.executeQuery()) {
          final int maxRunNumber;
          if (rs.next()) {
            maxRunNumber = rs.getInt(1);
          } else {
            maxRunNumber = 1;
          }
          pageContext.setAttribute("maxRunNumber", maxRunNumber);
        } // result set
      } // prepared statement

      pageContext.setAttribute("tournamentTeams", Queries.getTournamentTeams(connection).values());

      final Collection<String> finalistDivisions = FinalistSchedule.getAllDivisions(connection, tournament);
      pageContext.setAttribute("finalistDivisions", finalistDivisions);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new FLLRuntimeException(e);
    }
  }

}
