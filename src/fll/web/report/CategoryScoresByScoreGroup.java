/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;

/**
 * Display the report for scores by score group.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class CategoryScoresByScoreGroup extends HttpServlet {

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    doGet(request, response);
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    final PrintWriter writer = response.getWriter();
    final ServletContext application = getServletContext();
    final Connection connection = (Connection)application.getAttribute("connection");
    final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

    writer.write("<h1>FLL Categorized Score Summary by score group</h1>");
    writer.write("<hr/>");
    
    // cache the subjective categories title->dbname
    final Map<String, String> subjectiveCategories = new HashMap<String, String>();
    final NodeList subjectiveCategoryElements = challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
    for(int i = 0; i < subjectiveCategoryElements.getLength(); i++) {
      final Element subjectiveElement = (Element)subjectiveCategoryElements.item(i);
      final String title = subjectiveElement.getAttribute("title");
      final String name = subjectiveElement.getAttribute("name");
      subjectiveCategories.put(title, name);
    }

    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      final String currentTournament = Queries.getCurrentTournament(connection);

      stmt = connection.createStatement();

      // foreach division
      for(String division : Queries.getDivisions(connection)) {
        // foreach subjective category
        for(String categoryTitle : subjectiveCategories.keySet()) {
          final String categoryName = subjectiveCategories.get(categoryTitle);

          prep = connection.prepareStatement("SELECT Judge FROM " + categoryName + " WHERE TeamNumber = ? AND Tournament = ? AND ComputedTotal IS NOT NULL ORDER BY Judge");
          prep.setString(2, currentTournament);

          // foreach team, put the team in a score group
          final Map<String, Collection<Integer>> scoreGroups = new HashMap<String, Collection<Integer>>();
          for(Team team : Queries.getTournamentTeams(connection).values()) {
            // only show the teams for the division that we are looking at right
            // now
            if(division.equals(team.getEventDivision())) {
              final int teamNum = team.getTeamNumber();
              StringBuilder scoreGroup = new StringBuilder();
              prep.setInt(1, teamNum);
              rs = prep.executeQuery();
              while(rs.next()) {
                scoreGroup.append(rs.getString(1));
              }
              Utilities.closeResultSet(rs);

              final String scoreGroupStr = scoreGroup.toString();
              if(!scoreGroups.containsKey(scoreGroupStr)) {
                scoreGroups.put(scoreGroupStr, new LinkedList<Integer>());
              }
              scoreGroups.get(scoreGroupStr).add(teamNum);
            }
          }
          Utilities.closePreparedStatement(prep);

          // select from FinalScores
          for(String scoreGroup : scoreGroups.keySet()) {
            writer.write("<h3>" + categoryTitle + " Division: " + division + " Score Group: " + scoreGroup + "</h3");
            writer.write("<table border='0'>");
            writer.write("<tr><th colspan='3'>Team # / Organization / Team Name</th><th>Scaled Score</th></tr>");

            final String teamSelect = StringUtils.join(scoreGroups.get(scoreGroup).iterator(), ", ");
            prep = connection.prepareStatement("SELECT Teams.TeamNumber,Teams.Organization,Teams.TeamName,FinalScores." + categoryName
                + " FROM Teams, FinalScores WHERE FinalScores.TeamNumber IN ( " + teamSelect
                + ") AND Teams.TeamNumber = FinalScores.TeamNumber AND FinalScores.Tournament = ? ORDER BY FinalScores." + categoryName + " DESC");
            prep.setString(1, currentTournament);
            rs = prep.executeQuery();
            while(rs.next()) {
              writer.write("<tr>");
              writer.write("<td>");
              writer.write(rs.getString(1));
              writer.write("</td>");
              writer.write("<td>");
              writer.write(rs.getString(2));
              writer.write("</td>");
              writer.write("<td>");
              writer.write(rs.getString(3));
              writer.write("</td>");
              writer.write("<td>");
              writer.write(Utilities.NUMBER_FORMAT_INSTANCE.format(rs.getDouble(4)));
              writer.write("</td>");
              writer.write("</tr>");
            }
            writer.write("</table");
          }

        }
      }

    } catch(final SQLException sqle) {
      throw new RuntimeException(sqle);
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }

  }
}
