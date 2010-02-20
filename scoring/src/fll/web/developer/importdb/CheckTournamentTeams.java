/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.ImportDB;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Servlet to check that all of the teams are in the right tournament.
 * 
 * @author jpschewe
 * @web.servlet name="CheckTournamentTeams"
 * @web.servlet-mapping url-pattern="/developer/importdb/CheckTournamentTeams"
 */
public class CheckTournamentTeams extends BaseFLLServlet {

  private static final Logger LOG = Logger.getLogger(CheckTournamentTeams.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    Connection sourceConnection = null;
    Connection destConnection = null;
    try {
      final String tournament = SessionAttributes.getNonNullAttribute(session, "selectedTournament", String.class);
      final DataSource sourceDataSource = SessionAttributes.getNonNullAttribute(session, "dbimport", DataSource.class);
      sourceConnection = sourceDataSource.getConnection();
      
      final DataSource destDataSource = SessionAttributes.getDataSource(session);
      destConnection = destDataSource.getConnection();

      final List<TournamentDifference> tournamentDifferences = ImportDB.computeMissingFromTournamentTeams(sourceConnection, destConnection, tournament);
      session.setAttribute("tournamentDifferences", tournamentDifferences);
      
      if(tournamentDifferences.isEmpty()) {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "ExecuteImport");
      } else {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "resolveTournamentDifferences.jsp");
      }

    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.closeConnection(sourceConnection);
      SQLFunctions.closeConnection(destConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

}
