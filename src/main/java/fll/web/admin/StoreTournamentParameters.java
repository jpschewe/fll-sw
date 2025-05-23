/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Submit action for edit_tournament_parameters.jsp.
 */
@WebServlet("/admin/StoreTournamentParameters")
public class StoreTournamentParameters extends BaseFLLServlet {

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
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final boolean runningHeadToHead = "on".equals(request.getParameter("running_head_to_head"));
      TournamentParameters.setRunningHeadToHead(connection, tournament.getTournamentID(), runningHeadToHead);

      final int performanceAdvancementPercentage = WebUtils.getIntRequestParameter(request,
                                                                                   "performance_advancement_percentage");
      TournamentParameters.setPerformanceAdvancementPercentage(connection, tournament.getTournamentID(),
                                                               performanceAdvancementPercentage);

      final @Nullable String pitSignTopText = request.getParameter("pit_sign_top_text");
      TournamentParameters.setPitSignTopText(connection, tournament.getTournamentID(),
                                             null == pitSignTopText ? "" : pitSignTopText);

      final @Nullable String pitSignBottomText = request.getParameter("pit_sign_bottom_text");
      TournamentParameters.setPitSignBottomText(connection, tournament.getTournamentID(),
                                                null == pitSignBottomText ? "" : pitSignBottomText);

      if (message.length() == 0) {
        message.append("<p id='success'>Tournament parameters saved</p>");
      }
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving tournament parameters to the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());
    response.sendRedirect(response.encodeRedirectURL("edit_tournament_parameters.jsp"));
  }

}
