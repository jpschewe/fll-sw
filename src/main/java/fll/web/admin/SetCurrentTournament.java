/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Set the current tournament.
 */
@WebServlet("/admin/SetCurrentTournament")
public class SetCurrentTournament extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @SuppressFBWarnings(value = "HRS_REQUEST_PARAMETER_TO_HTTP_HEADER", justification = "Passing referrer along as the redirect")
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final String currentTournamentParam = request.getParameter("currentTournament");
      if (null != currentTournamentParam
          && !"".equals(currentTournamentParam)) {
        final int newTournamentID = Integer.parseInt(currentTournamentParam);
        final Tournament newTournament = Tournament.findTournamentByID(connection, newTournamentID);
        if (null == newTournament) {
          message.append(String.format("<p class='error'>Tournament with id %d is unknown</p>", newTournamentID));
        } else {
          Queries.setCurrentTournament(connection, newTournamentID);
          message.append(String.format("<p id='success'><i>Tournament changed to %s</i></p>", newTournament.getName()));
        }
      } else {
        message.append("<p class='error'>You must specify the new current tournament, ignoring request</p>");
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute("message", message.toString());

    final String referrer = request.getHeader("referer");
    response.sendRedirect(response.encodeRedirectURL(referrer == null ? "index.jsp" : referrer));

  }
}
