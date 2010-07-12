/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Servlet to create a tournament.
 * 
 * @author jpschewe
 */
public class CreateTournament extends BaseFLLServlet {

  private static final Logger LOG = Logger.getLogger(CreateTournament.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    try {
      final String answer = (String) request.getAttribute("submit");
      if ("Yes".equals(answer)) {
        createSelectedTournament(session);
      } else {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      }
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

  /**
   * Create the tournament that is in the session variable
   * <code>selectedTournament</code>.
   * 
   * @param session the session
   * @throws SQLException
   */
  private static void createSelectedTournament(final HttpSession session) throws SQLException {
    final StringBuilder message = new StringBuilder();

    Connection sourceConnection = null;
    Connection destConnection = null;
    try {
      final String tournament = SessionAttributes.getNonNullAttribute(session, "selectedTournament", String.class);
      final DataSource sourceDataSource = SessionAttributes.getNonNullAttribute(session, "dbimport", DataSource.class);
      sourceConnection = sourceDataSource.getConnection();

      final DataSource destDataSource = SessionAttributes.getDataSource(session);
      destConnection = destDataSource.getConnection();

      createTournament(sourceConnection, destConnection, tournament, message, session);

      session.setAttribute("message", message.toString());
    } finally {
      SQLFunctions.close(sourceConnection);
      SQLFunctions.close(destConnection);
    }
  }

  /**
   * Create a tournament from the source in the dest. This recurses on
   * nextTournament if needed.
   * 
   * @throws SQLException
   */
  private static void createTournament(final Connection sourceConnection,
                                       final Connection destConnection,
                                       final String tournamentName,
                                       final StringBuilder message,
                                       final HttpSession session) throws SQLException {
    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournamentName);
    Tournament.createTournament(destConnection, sourceTournament.getName(), sourceTournament.getLocation());
    message.append("<p>Created tournament "
        + sourceTournament.getName() + "</p>");

    if (null != sourceTournament.getNextTournament()) {
      final Tournament sourceNextTournament = sourceTournament.getNextTournament();
      final Tournament destNextTournament = Tournament.findTournamentByName(destConnection, sourceNextTournament.getName());
      if (null == destNextTournament) {
        createTournament(sourceConnection, destConnection, sourceNextTournament.getName(), message, session);
        Tournament.setNextTournament(destConnection, tournamentName, sourceNextTournament.getName());

        session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTournamentExists");
      } else {
        message.append("<p class='error'>Cannot find tournament "
            + tournamentName + " in imported database!</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      }
    }
  }

}
