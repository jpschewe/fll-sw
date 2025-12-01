/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import fll.Tournament;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Set the current tournament.
 */
@WebServlet("/admin/SetCurrentTournament")
public class SetCurrentTournament extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      final String currentTournamentParam = request.getParameter("currentTournament");
      if (null != currentTournamentParam
          && !"".equals(currentTournamentParam)) {
        final int newTournamentID = Integer.parseInt(currentTournamentParam);
        if (!Tournament.doesTournamentExist(connection, newTournamentID)) {
          message.append(String.format("<p class='error'>Tournament with id %d is unknown</p>", newTournamentID));
        } else {
          final Tournament newTournament = Tournament.findTournamentByID(connection, newTournamentID);
          ApplicationAttributes.getTournamentData(application).setCurrentTournament(newTournament);
          message.append(String.format("<p id='success'><i>Tournament changed to %s</i></p>", newTournament.getName()));
        }
      } else {
        message.append("<p class='error'>You must specify the new current tournament, ignoring request</p>");
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

    session.setAttribute("message", message.toString());

    final String referrer = request.getHeader("Referer");
    response.sendRedirect(response.encodeRedirectURL(StringUtils.isEmpty(referrer) ? "index.jsp" : referrer));

  }
}
