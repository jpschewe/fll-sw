/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Gather information for editing or adding a team and put it in the session.
 * 
 * @web.servlet name="GatherTeamData"
 * @web.servlet-mapping url-pattern="/admin/GatherTeamData"
 */
public class GatherTeamData extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of GatherTeamData.doPost");
    }

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);

    try {
      final Connection connection = datasource.getConnection();

      // store map of tournaments in session
      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      session.setAttribute("tournaments", tournaments);

      session.setAttribute("divisions", Queries.getDivisions(connection));

      if ("1".equals(request.getParameter("addTeam"))) {
        // put blanks in for all values
        session.setAttribute("addTeam", true);
        session.setAttribute("teamNumber", null);
        session.setAttribute("teamName", null);
        session.setAttribute("organization", null);
        session.setAttribute("region", null);
        session.setAttribute("division", null);
        session.setAttribute("teamPrevTournament", null);
        session.setAttribute("teamCurrentTournament", null);

        session.setAttribute("playoffsInitialized",
                             Queries.isPlayoffDataInitialized(connection, Queries.getCurrentTournament(connection)));
      } else {
        session.setAttribute("addTeam", false);

        // check parsing the team number to be sure that we fail right away
        final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("teamNumber")).intValue();

        // check if team is listed in any playoff data
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
          prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
              + " WHERE Team = ?");
          prep.setInt(1, teamNumber);
          rs = prep.executeQuery();
          if (!rs.next()) {
            throw new RuntimeException("Query to obtain count of PlayoffData entries returned no data");
          } else {
            session.setAttribute("inPlayoffs", rs.getInt(1) > 0);
          }
        } finally {
          SQLFunctions.close(rs);
          SQLFunctions.close(prep);
        }

        // get the team information and put it in the session
        session.setAttribute("teamNumber", teamNumber);
        final Team team = Team.getTeamFromDatabase(connection, teamNumber);
        session.setAttribute("teamName", team.getTeamName());
        session.setAttribute("organization", team.getOrganization());
        session.setAttribute("region", team.getRegion());
        session.setAttribute("division", team.getDivision());
        final int teamCurrentTournamentID = Queries.getTeamCurrentTournament(connection, teamNumber);
        final Tournament teamCurrentTournament = Tournament.findTournamentByID(connection, teamCurrentTournamentID);
        final Integer teamPrevTournamentID = Queries.getTeamPrevTournament(connection, teamNumber,
                                                                           teamCurrentTournamentID);
        session.setAttribute("teamPrevTournament", teamPrevTournamentID);
        session.setAttribute("teamCurrentTournament", teamCurrentTournament);

        final String currentEventDivision = Queries.getEventDivision(connection, teamNumber, teamCurrentTournamentID);
        if(LOGGER.isTraceEnabled()) {
          LOGGER.trace("Checking if playoffs are initialized for tournament: " + teamCurrentTournamentID + " division: " + currentEventDivision);
        }
        session.setAttribute("playoffsInitialized", Queries.isPlayoffDataInitialized(connection,
                                                                                     teamCurrentTournamentID,
                                                                                     currentEventDivision));
      }
    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new RuntimeException("Error parsing team number, this is an internal error", pe);
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Bottom of GatherTeamData.doPost");
    }

    if (message.length() > 0) {
      session.setAttribute("message", message.toString());
    }

    response.sendRedirect(response.encodeRedirectURL("editTeam.jsp"));
  }
}
