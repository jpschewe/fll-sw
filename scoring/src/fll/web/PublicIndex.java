/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.report.finalist.FinalistSchedule;

/**
 * Populate the page context for the public index page.
 */
public class PublicIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param application application context
   * @param session session context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final String tournamentName = Queries.getCurrentTournamentName(connection);
      pageContext.setAttribute("tournamentName", tournamentName);

      final int tournament = Queries.getCurrentTournament(connection);
      final Collection<String> finalistDivisions = FinalistSchedule.getAllDivisions(connection, tournament);
      pageContext.setAttribute("finalistDivisions", finalistDivisions);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }

  }
}
