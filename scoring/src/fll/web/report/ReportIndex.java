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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.report.finalist.FinalistSchedule;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Populate the page context for the report index page.
 */
public class ReportIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param application application context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {

    // clear out some variables
    session.removeAttribute(PromptSummarizeScores.SUMMARY_REDIRECT_KEY);
    session.removeAttribute(PromptSummarizeScores.SUMMARY_CHECKED_KEY);

    Connection connection = null;
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final int tournament = Queries.getCurrentTournament(connection);

      prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?");
      prep.setInt(1, Queries.getCurrentTournament(connection));
      rs = prep.executeQuery();
      final int maxRunNumber;
      if (rs.next()) {
        maxRunNumber = rs.getInt(1);
      } else {
        maxRunNumber = 1;
      }
      pageContext.setAttribute("maxRunNumber", maxRunNumber);

      pageContext.setAttribute("tournamentTeams", Queries.getTournamentTeams(connection).values());

      final Collection<String> finalistDivisions = FinalistSchedule.getAllDivisions(connection, tournament);
      pageContext.setAttribute("finalistDivisions", finalistDivisions);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }
  }

}
