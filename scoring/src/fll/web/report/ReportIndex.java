/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.report.finalist.FinalistSchedule;

/**
 * Populate the page context for the report index page.
 */
public class ReportIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param application application context
   * @param session session context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final int tournament = Queries.getCurrentTournament(connection);

      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = "
          + Queries.getCurrentTournament(connection));
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
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);
    }
  }

}
