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
import javax.servlet.annotation.WebServlet;
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
 * Gather data for advancement of teams.
 */
@WebServlet("/admin/GatherAdvancementData")
public class GatherAdvancementData extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of GatherAdvancementData.doPost");
    }

    final DataSource datasource = SessionAttributes.getDataSource(session);

    final String[] teamsToAdvance = request.getParameterValues("advance");

    Connection connection = null;
    try {
      connection = datasource.getConnection();

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

      processAdvancementData(response, session, null == teamsToAdvance, connection, teamNumbers);

    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new RuntimeException("Error parsing team number, this is an internal error", pe);
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

  /**
   * Process the form data.
   * 
   * @param sendToSelect if true, send to selectTeamsToAdvance.jsp, else send to
   *          verifyAdvancingTeams.jsp
   */
  /* package */static void processAdvancementData(final HttpServletResponse response,
                                                  final HttpSession session,
                                                  final boolean sendToSelect,
                                                  final Connection connection,
                                                  final Collection<Integer> teamNumbers) throws SQLException,
      IOException {
    final StringBuilder message = new StringBuilder();
    final Map<Integer, String> currentTournament = new HashMap<Integer, String>();
    final Map<Integer, String> nextTournament = new HashMap<Integer, String>();
    final List<Team> advancingTeams = new LinkedList<Team>();
    for (final int teamNum : teamNumbers) {
      final Team team = Team.getTeamFromDatabase(connection, teamNum);
      if (!team.isInternal()) {
        final int currentID = Queries.getTeamCurrentTournament(connection, teamNum);
        final Tournament current = Tournament.findTournamentByID(connection, currentID);

        if (null == current.getNextTournament()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cannot advance team "
                + teamNum + " as the current tournament " + current.getName() + " does not have a next tournament");
          }
        } else {
          advancingTeams.add(team);
          currentTournament.put(teamNum, current.getName());
          nextTournament.put(teamNum, current.getNextTournament().getName());
        }
      }

    }

    Collections.sort(advancingTeams, Team.TEAM_NUMBER_COMPARATOR);
    session.setAttribute("advancingTeams", advancingTeams);
    session.setAttribute("currentTournament", currentTournament);
    session.setAttribute("nextTournament", nextTournament);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Bottom of GatherAdvancementData.doPost");
    }

    if (message.length() > 0) {
      session.setAttribute("message", message.toString());
    }

    if (sendToSelect) {
      response.sendRedirect(response.encodeRedirectURL("selectTeamsToAdvance.jsp"));
    } else {
      response.sendRedirect(response.encodeRedirectURL("verifyAdvancingTeams.jsp"));
    }
  }

}
