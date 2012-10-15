/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreboard;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.StringUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

@WebServlet("/scoreboard/Last8")
public class Last8 extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Entering doPost");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final Formatter formatter = new Formatter(response.getWriter());
    final String showOrgStr = request.getParameter("showOrganization");
    final boolean showOrg = null == showOrgStr ? true : Boolean.parseBoolean(showOrgStr);

    PreparedStatement prep = null;
    ResultSet rs = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);
      final int maxScoreboardRound = Queries.getMaxScoreboardPerformanceRound(connection, currentTournament);

      formatter.format("<html>");
      formatter.format("<head>");
      formatter.format("<link rel='stylesheet' type='text/css' href='score_style.css' />");
      formatter.format("<meta http-equiv='refresh' content='30' />");
      formatter.format("</head>");

      formatter.format("<body>");
      formatter.format("<center>");
      formatter.format("<table border='1' cellpadding='0' cellspacing='0' width='98%%'>");
      formatter.format("<tr>");
      int numColumns = 5;
      if (!showOrg) {
        --numColumns;
      }
      formatter.format("<th colspan='%d'>Most Recent Performance Scores</th>", numColumns);
      formatter.format("</tr>");

      // scores here
      prep = connection.prepareStatement("SELECT Teams.TeamNumber"
          + ", Teams.Organization" //
          + ", Teams.TeamName" //
          + ", current_tournament_teams.event_division" //
          + ", verified_performance.Bye" //
          + ", verified_performance.NoShow" //
          + ", verified_performance.ComputedTotal" //
          + " FROM Teams,verified_performance,current_tournament_teams"//
          + " WHERE verified_performance.Tournament = ?" //
          + "  AND Teams.TeamNumber = verified_performance.TeamNumber" //
          + "  AND Teams.TeamNumber = current_tournament_teams.TeamNumber" //
          + "  AND verified_performance.Bye = False" //
          + "  AND verified_performance.RunNumber <= ?"
          + " ORDER BY verified_performance.TimeStamp DESC, Teams.TeamNumber ASC LIMIT 8");
      prep.setInt(1, currentTournament);
      prep.setInt(2, maxScoreboardRound);
      rs = prep.executeQuery();

      while (rs.next()) {
        formatter.format("<tr>");
        formatter.format("<td class='left' width='10%%'><b>%d</b></td>", rs.getInt("TeamNumber"));
        String teamName = rs.getString("TeamName");
        teamName = null == teamName ? "&nbsp;" : StringUtils.trimString(teamName, Team.MAX_TEAM_NAME_LEN);
        formatter.format("<td class='left' width='28%%'><b>%s</b></td>", teamName);
        if (showOrg) {
          String organization = rs.getString("Organization");
          organization = null == organization ? "&nbsp;" : StringUtils.trimString(organization, Top10.MAX_ORG_NAME);
          formatter.format("<td class='left'><b>%s</b></td>", organization);
        }
        formatter.format("<td class='right' width='5%%'><b>%s</b></td>", rs.getString("event_division"));

        formatter.format("<td class='right' width='8%%'><b>");
        if (rs.getBoolean("NoShow")) {
          formatter.format("No Show");
        } else if (rs.getBoolean("Bye")) {
          formatter.format("Bye");
        } else {
          formatter.format("%s", Utilities.NUMBER_FORMAT_INSTANCE.format(rs.getDouble("ComputedTotal")));
        }
        formatter.format("</b></td>");
        formatter.format("</tr>");

      }// end while next

      formatter.format("</table>");
      formatter.format("</body>");
      formatter.format("</html>");
    } catch (final SQLException e) {
      throw new RuntimeException("Error talking to the database", e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Exiting doPost");
    }
  }
}
