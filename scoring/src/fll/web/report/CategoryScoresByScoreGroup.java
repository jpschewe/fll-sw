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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

/**
 * Display the report for scores by score group.
 * 
 * @author jpschewe
 * @web.servlet name="CategoryScoresByScoreGroup"
 * @web.servlet-mapping url-pattern="/report/CategoryScoresByScoreGroup"
 */
public class CategoryScoresByScoreGroup extends BaseFLLServlet {

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { 
  "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table name, winner criteria determines sort")
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);
    final String ascDesc = WinnerType.HIGH == winnerCriteria ? "DESC" : "ASC";

    final PrintWriter writer = response.getWriter();
    writer.write("<html><body>");
    writer.write("<h1>FLL Categorized Score Summary by score group</h1>");
    writer.write("<hr/>");

    // cache the subjective categories title->dbname
    final Map<String, String> subjectiveCategories = new HashMap<String, String>();
    for (final Element subjectiveElement : new NodelistElementCollectionAdapter(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
      final String title = subjectiveElement.getAttribute("title");
      final String name = subjectiveElement.getAttribute("name");
      subjectiveCategories.put(title, name);
    }

    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      final Connection connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);

      // foreach division
      for (final String division : Queries.getEventDivisions(connection)) {
        // foreach subjective category
        for(final Map.Entry<String, String> entry : subjectiveCategories.entrySet()) {
          final String categoryTitle = entry.getKey();
          final String categoryName = entry.getValue();

          final Map<String, Collection<Integer>> scoreGroups = Queries.computeScoreGroups(connection, currentTournament, division, categoryName);

          // select from FinalScores
          for(final Map.Entry<String, Collection<Integer>> scoreGroupEntry : scoreGroups.entrySet()) {
            final String scoreGroup = scoreGroupEntry.getKey();
            writer.write("<h3>"
                + categoryTitle + " Division: " + division + " Score Group: " + scoreGroup + "</h3>");
            writer.write("<table border='0'>");
            writer.write("<tr><th colspan='3'>Team # / Organization / Team Name</th><th>Scaled Score</th></tr>");

            final String teamSelect = StringUtils.join(scoreGroupEntry.getValue().iterator(), ", ");
            prep = connection.prepareStatement("SELECT Teams.TeamNumber,Teams.Organization,Teams.TeamName,FinalScores."
                + categoryName + " FROM Teams, FinalScores WHERE FinalScores.TeamNumber IN ( " + teamSelect
                + ") AND Teams.TeamNumber = FinalScores.TeamNumber AND FinalScores.Tournament = ? ORDER BY FinalScores." + categoryName + " " + ascDesc);
            prep.setInt(1, currentTournament);
            rs = prep.executeQuery();
            while (rs.next()) {
              final int teamNum = rs.getInt(1);
              final String org = rs.getString(2);
              final String name = rs.getString(3);
              final double score = rs.getDouble(4);
              final boolean scoreWasNull = rs.wasNull();
              writer.write("<tr>");
              writer.write("<td>");
              writer.write(teamNum);
              writer.write("</td>");
              writer.write("<td>");
              if (null == org) {
                writer.write("");
              } else {
                writer.write(org);
              }
              writer.write("</td>");
              writer.write("<td>");
              if (null == name) {
                writer.write("");
              } else {
                writer.write(name);
              }
              writer.write("</td>");
              if (!scoreWasNull) {
                writer.write("<td>");
                writer.write(Utilities.NUMBER_FORMAT_INSTANCE.format(score));
              } else {
                writer.write("<td align='center' class='warn'>No Score");
              }
              writer.write("</td>");
              writer.write("</tr>");
            }
            writer.write("</table>");
          }
        }
      }

    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    writer.write("</body></html>");
  }
}
