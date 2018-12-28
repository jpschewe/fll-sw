/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.db.Queries;
import fll.util.LogUtils;

/**
 * Code for /index.jsp.
 */
public class MainIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Populate the page context with information for the jsp.
   * pageContext:
   * <ul>
   * <li>tournamentTitle - display name of the tournament</li>
   * <li>urls - URLs to access the server - collection of string</li>
   * <li>baseSslUrl - base URL for SSL connections, string, does not end with with
   * slash</li>
   * </ul>
   * 
   * @param request the request object
   * @param application used for application variables
   * @param pageContext populated with the specified variables
   */
  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournamentId = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);
      pageContext.setAttribute("tournamentTitle", tournament.getDescription());

      pageContext.setAttribute("urls", WebUtils.getAllUrls(request, application));

      final String baseSslUrl = String.format("https://%s:%d%s", request.getServerName(), WebUtils.SSL_PORT,
                                              request.getContextPath());
      pageContext.setAttribute("baseSslUrl", baseSslUrl);
    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

  }

}
