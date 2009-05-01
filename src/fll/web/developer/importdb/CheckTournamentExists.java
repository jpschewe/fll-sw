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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.web.Init;
import fll.web.SessionAttributes;

/**
 * @author jpschewe
 * @version $Revision$
 */
public class CheckTournamentExists extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(CheckTournamentExists.class);

  /**
   * @param request
   * @param response
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final StringBuilder message = new StringBuilder();
    final HttpSession session = request.getSession();

    Connection connection = null;
    try {
      Init.initialize(request, response);

      if (null != request.getAttribute("tournament")) {
        final String selectedTournament = (String) request.getAttribute("tournament");
        session.setAttribute("selectedTournament", selectedTournament);

        // Check if the tournament exists
        final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);
        connection = datasource.getConnection();
        final List<String> existingTournaments = Queries.getTournamentNames(connection);

        if (!existingTournaments.contains(selectedTournament)) {
          session.setAttribute("redirect_url", "promptCreateTournament.jsp");
        }

      } else {
        message.append("<p class='error'>Unknown form state, expected form fields not seen: "
            + request + "</p>");
      }
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOG.error(sqle);
    } finally {
      SQLFunctions.closeConnection(connection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL((String) session.getAttribute("redirect_url")));
  }

}
