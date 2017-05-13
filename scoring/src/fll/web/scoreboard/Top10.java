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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FP;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import fll.xml.WinnerType;

@WebServlet("/scoreboard/Top10")
public class Top10 extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Max number of characters in a team name to display.
   */
  public static final int MAX_TEAM_NAME = 12;

  /**
   * Max number of characters in an organization to display.
   */
  public static final int MAX_ORG_NAME = 20;

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Determine sort order based upon winner criteria")
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Entering doPost");
    }

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();
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
      final int maxScoreboardRound = TournamentParameters.getMaxScoreboardPerformanceRound(connection,
                                                                                           currentTournament);

      final Integer divisionIndexObj = SessionAttributes.getAttribute(session, "divisionIndex", Integer.class);
      int divisionIndex;
      if (null == divisionIndexObj) {
        divisionIndex = 0;
      } else {
        divisionIndex = divisionIndexObj.intValue();
      }
      ++divisionIndex;
      final List<String> divisions = Queries.getAwardGroups(connection);
      if (divisionIndex >= divisions.size()) {
        divisionIndex = 0;
      }
      session.setAttribute("divisionIndex", Integer.valueOf(divisionIndex));

      formatter.format("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">%n");
      formatter.format("<html>%n");
      formatter.format("<head>%n");
      formatter.format("<link rel='stylesheet' type='text/css' href='../style/fll-sw.css' />%n");
      formatter.format("<link rel='stylesheet' type='text/css' href='score_style.css' />%n");
      formatter.format("<meta http-equiv='refresh' content='%d' />%n",
                       GlobalParameters.getIntGlobalParameter(connection, GlobalParameters.DIVISION_FLIP_RATE));
      formatter.format("</head>%n");

      formatter.format("<body class='scoreboard'>%n");
      formatter.format("<table border='1' cellpadding='2' cellspacing='0' width='98%%'>%n");

      formatter.format("<colgroup>%n");
      formatter.format("<col width='30px' />%n");
      formatter.format("<col width='75px' />%n");
      formatter.format("<col />%n");
      if (showOrg) {
        formatter.format("<col />%n");
      }
      formatter.format("<col width='70px' />%n");
      formatter.format("</colgroup>%n");

      if (!divisions.isEmpty()) {
        formatter.format("<tr>%n");
        int numColumns = 5;
        if (!showOrg) {
          --numColumns;
        }
        formatter.format("<th colspan='%d' bgcolor='%s'>Top Performance Scores: %s</th>", numColumns,
                         Queries.getColorForIndex(divisionIndex), divisions.get(divisionIndex));
        formatter.format("</tr>%n");

        final WinnerType winnerCriteria = challengeDescription.getWinner();

        prep = connection.prepareStatement("SELECT Teams.TeamName, Teams.Organization, Teams.TeamNumber, T2.MaxOfComputedScore" //
            + " FROM (SELECT TeamNumber, " //
            + winnerCriteria.getMinMaxString() + "(ComputedTotal) AS MaxOfComputedScore" //
            + "  FROM verified_performance WHERE Tournament = ? " + "   AND NoShow = False" //
            + "   AND Bye = False" //
            + "   AND RunNumber <= ?" //
            + "  GROUP BY TeamNumber) AS T2"
            + " JOIN Teams ON Teams.TeamNumber = T2.TeamNumber, current_tournament_teams"
            + " WHERE Teams.TeamNumber = current_tournament_teams.TeamNumber" //
            + " AND current_tournament_teams.event_division = ?" + " ORDER BY T2.MaxOfComputedScore "
            + winnerCriteria.getSortString());
        prep.setInt(1, currentTournament);
        prep.setInt(2, maxScoreboardRound);
        prep.setString(3, divisions.get(divisionIndex));
        rs = prep.executeQuery();

        double prevScore = -1;
        int i = 1;
        int rank = 0;
        while (rs.next()) {
          final double score = rs.getDouble("MaxOfComputedScore");
          if (!FP.equals(score, prevScore, 1E-6)) {
            rank = i;
          }

          formatter.format("<tr>%n");
          formatter.format("<td class='center'>%d</td>%n", rank);
          formatter.format("<td class='right'>%d</td>%n", rs.getInt("TeamNumber"));
          String teamName = rs.getString("TeamName");
          if (null == teamName) {
            teamName = "&nbsp;";
          }
          formatter.format("<td class='left truncate'>%s</td>%n", teamName);
          if (showOrg) {
            String organization = rs.getString("Organization");
            if (null == organization) {
              organization = "&nbsp;";
            }
            formatter.format("<td class='left truncate'>%s</td>%n", organization);
          }
          formatter.format("<td class='right'>%s</td>%n",
                           Utilities.getFormatForScoreType(performanceScoreType).format(score));

          formatter.format("</tr>");

          prevScore = score;
          ++i;
        } // end while next
      } // end divisions not empty
      formatter.format("</table>%n");
      formatter.format("</body>%n");
      formatter.format("</html>%n");
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
