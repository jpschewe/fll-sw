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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.WebUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Gather information for editing or adding a team and put it in the page
 * context.
 */
public class GatherTeamData {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext page) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of GatherTeamData.populateContext");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      // store map of tournaments in session
      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      page.setAttribute("tournaments", tournaments);

      final Map<Integer, Collection<String>> tournamentEventDivisions = new HashMap<>();
      final Map<Integer, Collection<String>> tournamentJudgingStations = new HashMap<>();
      final Map<Integer, Boolean> playoffsInitialized = new HashMap<>();
      for (final Tournament tournament : tournaments) {

        final Collection<String> allEventDivisions = Queries.getAwardGroups(connection,
                                                                               tournament.getTournamentID());
        if (allEventDivisions.isEmpty()) {
          // special case for empty, always allow 1
          allEventDivisions.add("1");
        }
        tournamentEventDivisions.put(tournament.getTournamentID(), allEventDivisions);

        final Collection<String> allJudgingStations = Queries.getJudgingStations(connection,
                                                                                 tournament.getTournamentID());
        if (allJudgingStations.isEmpty()) {
          // special case for empty, always allow 1
          allJudgingStations.add("1");
        }
        tournamentJudgingStations.put(tournament.getTournamentID(), allJudgingStations);

        playoffsInitialized.put(tournament.getTournamentID(),
                                Queries.isPlayoffDataInitialized(connection, tournament.getTournamentID()));
      }
      page.setAttribute("tournamentEventDivisions", tournamentEventDivisions);
      page.setAttribute("tournamentJudgingStations", tournamentJudgingStations);
      page.setAttribute("playoffsInitialized", playoffsInitialized);

      final String teamNumberStr = request.getParameter("teamNumber");

      if (null == teamNumberStr) {
        // put blanks in for all values
        page.setAttribute("addTeam", true);
        page.setAttribute("teamNumber", null);
        page.setAttribute("teamName", null);
        page.setAttribute("teamNameEscaped", null);
        page.setAttribute("organization", null);
        page.setAttribute("organizationEscaped", null);
        page.setAttribute("division", null);
        page.setAttribute("teamInTouranemnt", Collections.emptyMap());
        page.setAttribute("inPlayoffs", false);
        page.setAttribute("currentEventDivisions", Collections.emptyMap());
        page.setAttribute("currentJudgingStations", Collections.emptyMap());
      } else {
        page.setAttribute("addTeam", false);

        // check parsing the team number to be sure that we fail right away
        final int teamNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();

        // track current division and judging station for team so that it can be
        // selected
        final Map<Integer, String> currentEventDivisions = new HashMap<>();
        final Map<Integer, String> currentJudgingStations = new HashMap<>();
        for (final Tournament tournament : tournaments) {
          final String eventDivision = Queries.getEventDivision(connection, teamNumber, tournament.getTournamentID());
          currentEventDivisions.put(tournament.getTournamentID(), eventDivision);

          final String judgingStation = Queries.getJudgingGroup(connection, teamNumber, tournament.getTournamentID());
          currentJudgingStations.put(tournament.getTournamentID(), judgingStation);
        }
        page.setAttribute("currentEventDivisions", currentEventDivisions);
        page.setAttribute("currentJudgingStations", currentJudgingStations);

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
            page.setAttribute("inPlayoffs", rs.getInt(1) > 0);
          }
        } finally {
          SQLFunctions.close(rs);
          SQLFunctions.close(prep);
        }

        // get the team information and put it in the session
        page.setAttribute("teamNumber", teamNumber);
        final Team team = Team.getTeamFromDatabase(connection, teamNumber);
        page.setAttribute("teamName", team.getTeamName());
        page.setAttribute("teamNameEscaped", WebUtils.escapeForHtmlFormValue(team.getTeamName()));
        page.setAttribute("organization", team.getOrganization());
        page.setAttribute("organizationEscaped", WebUtils.escapeForHtmlFormValue(team.getOrganization()));
        final Map<Integer, Boolean> teamInTournament = new HashMap<>();
        for (final Integer tid : Queries.getAllTournamentsForTeam(connection, teamNumber)) {
          teamInTournament.put(tid, true);
        }
        page.setAttribute("teamInTournament", teamInTournament);
      }
    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new RuntimeException("Error parsing team number, this is an internal error", pe);
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Bottom of GatherTeamData.populateContext");
    }
  }
}
