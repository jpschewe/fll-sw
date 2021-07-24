/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;

/**
 * Code for /index.jsp.
 */
public final class MainIndex {
  private MainIndex() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Populate the page context with information for the jsp.
   * pageContext:
   * <ul>
   * <li>tournament - current {@link Tournament} object</li>
   * <li>urls - URLs to access the server - collection of string</li>
   * </ul>
   *
   * @param request the request object
   * @param application used for application variables
   * @param pageContext populated with the specified variables
   */
  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext pageContext) {
    LOGGER.debug("Visiting index from {}", request.getHeader("referer"));

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournamentId = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);
      pageContext.setAttribute("tournament", tournament);

      pageContext.setAttribute("urls", WebUtils.getAllUrls(request, application));
    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

  }

}
