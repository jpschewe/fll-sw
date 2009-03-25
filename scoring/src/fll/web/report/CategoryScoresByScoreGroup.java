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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.db.Queries;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

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
    final ServletContext application = getServletContext();
    final Connection connection = (Connection)application.getAttribute("connection");
    final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);
    final String ascDesc = WinnerType.HIGH == winnerCriteria ? "DESC" : "ASC";

    final PrintWriter writer = response.getWriter();
    writer.write("<html><body>");
    writer.write("<h1>FLL Categorized Score Summary by score group</h1>");
    writer.write("<hr/>");

    // cache the subjective categories title->dbname
    final Map<String, String> subjectiveCategories = new HashMap<String, String>();
    for(final Element subjectiveElement : XMLUtils.filterToElements(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
      final String title = subjectiveElement.getAttribute("title");
      final String name = subjectiveElement.getAttribute("name");
      subjectiveCategories.put(title, name);
    }

    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      final String currentTournament = Queries.getCurrentTournament(connection);

      // foreach division
      for(String division : Queries.getDivisions(connection)) {
        // foreach subjective category
        for(String categoryTitle : subjectiveCategories.keySet()) {
          final String categoryName = subjectiveCategories.get(categoryTitle);
          
          final Map<String, Collection<Integer>> scoreGroups = Queries.computeScoreGroups(connection, currentTournament, division, categoryName);

          // select from FinalScores
          for(String scoreGroup : scoreGroups.keySet()) {
            writer.write("<h3>" + categoryTitle + " Division: " + division + " Score Group: " + scoreGroup + "</h3>");
            writer.write("<table border='0'>");
            writer.write("<tr><th colspan='3'>Team # / Organization / Team Name</th><th>Scaled Score</th></tr>");

            final String teamSelect = StringUtils.join(scoreGroups.get(scoreGroup).iterator(), ", ");
            prep = connection.prepareStatement("SELECT Teams.TeamNumber,Teams.Organization,Teams.TeamName,FinalScores." + categoryName
                + " FROM Teams, FinalScores WHERE FinalScores.TeamNumber IN ( " + teamSelect
                + ") AND Teams.TeamNumber = FinalScores.TeamNumber AND FinalScores.Tournament = ? ORDER BY FinalScores." + categoryName + " " + ascDesc);
            prep.setString(1, currentTournament);
            rs = prep.executeQuery();
            while(rs.next()) {
              final String teamNum = rs.getString(1);
              final String org = rs.getString(2);
              final String name = rs.getString(3);
              final double score = rs.getDouble(4);
              final boolean scoreWasNull = rs.wasNull();
              writer.write("<tr>");
              writer.write("<td>");
              if(null == teamNum) {
                writer.write("");
              } else {
                writer.write(teamNum);
              }
              writer.write("</td>");
              writer.write("<td>");
              if(null == org) {
                writer.write("");
              } else {
                writer.write(org);
              }
              writer.write("</td>");
              writer.write("<td>");
              if(null == name) {
                writer.write("");
              } else {
                writer.write(name);
              }
              writer.write("</td>");
              if(!scoreWasNull) {
                writer.write("<td>");
                writer.write(Utilities.NUMBER_FORMAT_INSTANCE.format(score));
              } else {
                writer.write("<td align='center' class='warn'>No Score");
              }
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
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
    }

    writer.write("</body></html>");
  }
}
