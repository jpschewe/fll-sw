/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fll.Team;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.playoff.PlayoffIndex;
import fll.web.playoff.PlayoffSessionData;

/**
 * Check seeding round information for teams. Redirects to
 * checkSeedingRoundsResult.jsp.
 */
@WebServlet("/playoff/CheckSeedingRounds")
public class CheckSeedingRounds extends BaseFLLServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckSeedingRounds.class);

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final StringBuilder message = new StringBuilder();

      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final Map<Integer, TournamentTeam> tournamentTeams = data.getTournamentTeams();

      final List<Team> less = Queries.getTeamsNeedingSeedingRuns(connection, tournamentTeams, true);
      data.setTeamsNeedingSeedingRounds(less);

      SessionAttributes.appendToMessage(session, message.toString());
      response.sendRedirect(response.encodeRedirectURL("checkSeedingRoundsResult.jsp"));

    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database", e);
      throw new RuntimeException(e);
    }

  }

}
