/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Team;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Gather data for advancement of teams.
 */
public class GatherAdvancementData extends BaseFLLServlet {

  private static final Logger LOGGER = Logger.getLogger(GatherAdvancementData.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of GatherAdvancementData.doPost");
    }

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);

    final String[] teamsToAdvance = request.getParameterValues("advance");

    try {
      final Connection connection = datasource.getConnection();

      final Collection<Integer> teamNumbers;
      if (null != teamsToAdvance) {
        teamNumbers = new HashSet<Integer>();
        for (final String teamNumStr : teamsToAdvance) {
          final int teamNum = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumStr).intValue();
          teamNumbers.add(teamNum);
        }
      } else {
        teamNumbers = Queries.getAllTeamNumbers(connection);
      }

      final Map<Integer, String> currentTournament = new HashMap<Integer, String>();
      final Map<Integer, String> nextTournament = new HashMap<Integer, String>();
      final List<Team> advancingTeams = new LinkedList<Team>();
      for (final int teamNum : teamNumbers) {
        final Team team = Team.getTeamFromDatabase(connection, teamNum);
        if (!GenerateDB.INTERNAL_REGION.equals(team.getRegion())) {
          final int current = Queries.getTeamCurrentTournament(connection, teamNum);
          final String currentName = Queries.getTournamentName(connection, current);

          final Integer next = Queries.getNextTournament(connection, current);
          if (null == next) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Cannot advance team "
                  + teamNum + " as the current tournament " + currentName + " does not have a next tournament");
            }
          } else {
            advancingTeams.add(team);
            currentTournament.put(teamNum, currentName);
            final String nextName = Queries.getTournamentName(connection, next);
            nextTournament.put(teamNum, nextName);
          }
        }

      }

      Collections.sort(advancingTeams, Team.TEAM_NUMBER_COMPARATOR);
      session.setAttribute("advancingTeams", advancingTeams);
      session.setAttribute("currentTournament", currentTournament);
      session.setAttribute("nextTournament", nextTournament);

    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new RuntimeException("Error parsing team number, this is an internal error", pe);
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Bottom of GatherAdvancementData.doPost");
    }

    if (message.length() > 0) {
      session.setAttribute("message", message.toString());
    }

    if (null == teamsToAdvance) {
      response.sendRedirect(response.encodeRedirectURL("selectTeamsToAdvance.jsp"));
    } else {
      response.sendRedirect(response.encodeRedirectURL("verifyAdvancingTeams.jsp"));
    }
  }

}
