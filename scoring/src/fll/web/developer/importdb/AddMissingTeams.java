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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Team;
import fll.Tournament;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Add teams after promptCreateMissingTeams.jsp.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/AddMissingTeams")
public class AddMissingTeams extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    Connection sourceConnection = null;
    Connection destConnection = null;
    try {
      final String tournamentName = SessionAttributes.getNonNullAttribute(session, "selectedTournament", String.class);
      final DataSource sourceDataSource = SessionAttributes.getNonNullAttribute(session, "dbimport", DataSource.class);
      sourceConnection = sourceDataSource.getConnection();

      final DataSource destDataSource = ApplicationAttributes.getDataSource();
      destConnection = destDataSource.getConnection();

      final Tournament tournament = Tournament.findTournamentByName(destConnection, tournamentName);
      final int tournamentID = tournament.getTournamentID();

      @SuppressWarnings(value = "unchecked")
      final List<Team> missingTeams = SessionAttributes.getNonNullAttribute(session, "missingTeams", List.class);
      for (final Team team : missingTeams) {
        final String dup = Queries.addTeam(destConnection, team.getTeamNumber(), team.getTeamName(),
                                           team.getOrganization(), team.getDivision(), tournamentID);
        if (null != dup) {
          throw new FLLRuntimeException(
                                        String.format("Internal error, team with number %d should not exist in the destination database, found match with team with name: %s",
                                                      team.getTeamNumber(), dup));
        }
      }

      session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTeamInfo");

    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.close(sourceConnection);
      SQLFunctions.close(destConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }
}
