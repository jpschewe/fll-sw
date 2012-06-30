/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Check if the team being added/edited needs judging station set. Assumes that
 * the team number and judging station are set in the session.
 */
@WebServlet("/admin/CheckJudgingStationNeeded")
public class CheckJudgingStationNeeded extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Key for storing the current judging station for a team. Value is a
   * {@link String}.
   */
  public static final String JUDGING_STATION = "judging_station";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = SessionAttributes.getDataSource(session);

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int teamNumber = SessionAttributes.getNonNullAttribute(session, GatherTeamData.TEAM_NUMBER, Integer.class);

      final int teamCurrentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);

      final String judgingStation = Queries.getJudgingStation(connection, teamNumber, teamCurrentTournament);
      session.setAttribute(JUDGING_STATION, judgingStation);

      @SuppressWarnings("unchecked")
      final Collection<String> allJudgingStations = (Collection<String>) SessionAttributes.getNonNullAttribute(session,
                                                                                                               CommitTeam.ALL_JUDGING_STATIONS,
                                                                                                               Collection.class);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("team: "
            + teamNumber + " judgingStation: " + judgingStation + " all: " + allJudgingStations);
      }

      if (!allJudgingStations.isEmpty()
          && !allJudgingStations.contains(judgingStation)) {
        response.sendRedirect(response.encodeRedirectURL("chooseJudgingStation.jsp"));
      } else {
        session.setAttribute(JUDGING_STATION, judgingStation);
        response.sendRedirect(response.encodeRedirectURL("SaveTeamData"));
      }
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
