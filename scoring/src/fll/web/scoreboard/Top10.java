/*
 * Copyright (c) 2008
 *      Jon Schewe.  All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * I'd appreciate comments/suggestions on the code jpschewe@mtu.net
 */
package fll.web.scoreboard;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;
import fll.util.FP;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

/**
 * @web.servlet name="Top10"
 * @web.servlet-mapping url-pattern="/scoreboard/Top10"
 */
public class Top10 extends BaseFLLServlet {

  private static final Logger LOGGER = Logger.getLogger(Top10.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Entering doPost");
    }

    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Formatter formatter = new Formatter(response.getWriter());
    final String showOrgStr = request.getParameter("showOrganization");
    final boolean showOrg = null == showOrgStr ? true : Boolean.parseBoolean(showOrgStr);

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      final Connection connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);

      final Integer divisionIndexObj = (Integer) session.getAttribute("divisionIndex");
      int divisionIndex;
      if (null == divisionIndexObj) {
        divisionIndex = 0;
      } else {
        divisionIndex = divisionIndexObj.intValue();
      }
      ++divisionIndex;
      final List<String> divisions = Queries.getEventDivisions(connection);
      if (divisionIndex >= divisions.size()) {
        divisionIndex = 0;
      }
      session.setAttribute("divisionIndex", Integer.valueOf(divisionIndex));

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
      formatter.format("<th colspan='%d' bgcolor='%s'>Top Ten Performance Scores: Division %s</th>", numColumns,
                       Queries.getColorForDivisionIndex(divisionIndex), divisions.get(divisionIndex));
      formatter.format("</tr>");

      final Document challengeDocument = (Document) application.getAttribute("challengeDocument");
      final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);

      prep = connection.prepareStatement("SELECT Teams.TeamName, Teams.Organization, Teams.TeamNumber, T2.MaxOfComputedScore FROM"
          + " (SELECT TeamNumber, " + winnerCriteria.getMinMaxString() + "(ComputedTotal) AS MaxOfComputedScore FROM verified_performance WHERE Tournament = ? "
          + " AND NoShow = False AND Bye = False GROUP BY TeamNumber) AS T2" + " JOIN Teams ON Teams.TeamNumber = T2.TeamNumber, current_tournament_teams"
          + " WHERE Teams.TeamNumber = current_tournament_teams.TeamNumber AND current_tournament_teams.event_division = ?"
          + " ORDER BY T2.MaxOfComputedScore " + winnerCriteria.getSortString() + " LIMIT 10");
      prep.setInt(1, currentTournament);
      prep.setString(2, divisions.get(divisionIndex));
      rs = prep.executeQuery();

      double prevScore = -1;
      int i = 1;
      int rank = 0;
      while (rs.next()) {
        final double score = rs.getDouble("MaxOfComputedScore");
        if (!FP.equals(score, prevScore, 1E-6)) {
          rank = i;
        }

        formatter.format("<tr>");
        formatter.format("<td class='center' width='7%%'>%d</td>", rank);
        formatter.format("<td class='right' width='10%%'>%d</td>", rs.getInt("TeamNumber"));
        String teamName = rs.getString("TeamName");
        teamName = null == teamName ? "&nbsp;" : teamName.substring(0, Math.min(10, teamName.length()));
        formatter.format("<td class='left' width='28%%'>%s</td>", teamName);
        if (showOrg) {
          String organization = rs.getString("Organization");
          organization = null == organization ? "&nbsp;" : organization.substring(0, Math.min(32, organization.length()));
          formatter.format("<td class='left'>%s</td>", organization);
        }
        formatter.format("<td class='right' width='8%%'>%s</td>", Utilities.NUMBER_FORMAT_INSTANCE.format(score));

        formatter.format("</tr>");

        prevScore = score;
        ++i;
      }// end while next

      formatter.format("</table>");
      formatter.format("</body>");
      formatter.format("</html>");
    } catch (final SQLException e) {
      throw new RuntimeException("Error talking to the database", e);
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Exiting doPost");
    }
  }
}
