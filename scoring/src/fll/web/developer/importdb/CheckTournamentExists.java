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
 * Servlet to check if the tournament exists in the dest database.
 * 
 * @author jpschewe
 * @web.servlet name="CheckTournamentExists"
 * @web.servlet-mapping url-pattern="/developer/importdb/CheckTournamentExists"
 */
public class CheckTournamentExists extends BaseFLLServlet {

  private static final Logger LOG = Logger.getLogger(CheckTournamentExists.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    Connection connection = null;
    try {
      // support finding the selected tournament in the session as well
      final String selectedTournamentParam = request.getParameter("tournament");
      final String selectedTournament;
      if(null == selectedTournamentParam) {
        selectedTournament = SessionAttributes.getAttribute(session, "selectedTournament", String.class);
      } else {
        selectedTournament = selectedTournamentParam;
      }
      
      if (null != selectedTournament) {
        session.setAttribute("selectedTournament", selectedTournament);

        // Check if the tournament exists
        final DataSource datasource = SessionAttributes.getDataSource(session);
        connection = datasource.getConnection();
        final Tournament tournament = Tournament.findTournamentByName(connection, selectedTournament);
        if (null == tournament) {
          session.setAttribute(SessionAttributes.REDIRECT_URL, "promptCreateTournament.jsp");
        } else {
          session.setAttribute(SessionAttributes.REDIRECT_URL, "FindMissingTeams");
        }

      } else {
        message.append("<p class='error'>Can't find the 'tournament' parameter</p>");
      }
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.closeConnection(connection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

}
