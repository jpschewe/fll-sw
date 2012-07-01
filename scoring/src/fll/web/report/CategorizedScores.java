/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

/**
 * Display the report for scores by score group.
 */
@WebServlet("/report/CategorizedScores")
public class CategorizedScores extends BaseFLLServlet {

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table name, winner criteria determines sort")
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final DataSource datasource = ApplicationAttributes.getDataSource();
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);

    final Formatter writer = new Formatter(response.getWriter());
    writer.format("<html><body>");
    writer.format("<h1>FLL Categorized Scores</h1>");
    writer.format("<hr/>");

    // cache the subjective categories title->dbname
    final Map<String, String> subjectiveCategories = new HashMap<String, String>();
    for (final Element subjectiveElement : new NodelistElementCollectionAdapter(
                                                                                challengeDocument.getDocumentElement()
                                                                                                 .getElementsByTagName("subjectiveCategory"))) {
      final String title = subjectiveElement.getAttribute("title");
      final String name = subjectiveElement.getAttribute("name");
      subjectiveCategories.put(title, name);
    }

    ResultSet rs = null;
    PreparedStatement prep = null;
    PreparedStatement rawScorePrep = null;
    ResultSet rawScoreRS = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);

      // foreach division
      for (final String division : Queries.getEventDivisions(connection)) {
        // foreach subjective category
        for (final Map.Entry<String, String> entry : subjectiveCategories.entrySet()) {
          final String categoryTitle = entry.getKey();
          final String categoryName = entry.getValue();

          writer.format("<h3>%s Division: %s</h3>", categoryTitle, division);
          writer.format("<table border='0'>");
          writer.format("<tr><th colspan='3'>Team # / Organization / Team Name</th><th>Raw Score</th><th>Scaled Score</th></tr>");

          prep = connection.prepareStatement("SELECT"//
              + " Teams.TeamNumber"//
              + ",Teams.Organization"//
              + ",Teams.TeamName" //
              + ",FinalScores." + categoryName//
              + " FROM Teams, FinalScores, TournamentTeams"//
              + " WHERE Teams.TeamNumber = FinalScores.TeamNumber"//
              + " AND TournamentTeams.TeamNumber = Teams.TeamNumber"//
              + " AND TournamentTeams.Tournament = FinalScores.Tournament"//
              + " AND TournamentTeams.event_division = ?"//
              + " AND FinalScores.Tournament = ?"//
              + " ORDER BY FinalScores." + categoryName + " " + winnerCriteria.getSortString());
          prep.setString(1, division);
          prep.setInt(2, currentTournament);
          rawScorePrep = connection.prepareStatement("SELECT ComputedTotal"//
              + " FROM " + categoryName //
              + " WHERE TeamNumber = ?" //
              + " AND Tournament = ?" //
              + " ORDER BY ComputedTotal " + winnerCriteria.getSortString() // get
                                                                            // the
                                                                            // best
                                                                            // score
          );
          rawScorePrep.setInt(2, currentTournament);
          rs = prep.executeQuery();
          while (rs.next()) {
            final int teamNum = rs.getInt(1);
            final String org = rs.getString(2);
            final String name = rs.getString(3);
            final double score = rs.getDouble(4);
            final boolean scoreWasNull = rs.wasNull();
            writer.format("<tr>");
            writer.format("<td>%d</td>", teamNum);
            writer.format("<td>");
            if (null == org) {
              writer.format("");
            } else {
              writer.format(org);
            }
            writer.format("</td>");
            writer.format("<td>");
            if (null == name) {
              writer.format("");
            } else {
              writer.format(name);
            }
            writer.format("</td>");
            // raw score
            writer.format("<td>");
            rawScorePrep.setInt(1, teamNum);
            rawScoreRS = rawScorePrep.executeQuery();
            boolean first = true;
            while (rawScoreRS.next()) {
              if (!first) {
                writer.format(",");
              } else {
                first = false;
              }
              final double rawScore = rawScoreRS.getDouble(1);
              if (rawScoreRS.wasNull()) {
                writer.format("<span class='warn'>No Score</span>");
              } else {
                writer.format(Utilities.NUMBER_FORMAT_INSTANCE.format(rawScore));
              }
            }
            SQLFunctions.close(rawScoreRS);
            writer.format("</td>");

            // scaled score
            if (!scoreWasNull) {
              writer.format("<td>");
              writer.format(Utilities.NUMBER_FORMAT_INSTANCE.format(score));
            } else {
              writer.format("<td align='center' class='warn'>No Score");
            }
            writer.format("</td>");
            writer.format("</tr>");
          }
          SQLFunctions.close(rs);
          SQLFunctions.close(prep);
          SQLFunctions.close(rawScorePrep);
          writer.format("</table>");
        }
      }

    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(rawScoreRS);
      SQLFunctions.close(rawScorePrep);
      SQLFunctions.close(connection);
    }

    writer.format("</body></html>");
  }
}
