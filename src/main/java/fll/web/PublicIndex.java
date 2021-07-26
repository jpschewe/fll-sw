/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.web.report.finalist.FinalistSchedule;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Populate the page context for the public index page.
 */
public final class PublicIndex {

  private PublicIndex() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param application application context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final String tournamentName = Tournament.getCurrentTournament(connection).getName();
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
