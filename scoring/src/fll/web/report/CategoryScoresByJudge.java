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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.ScoreType;
import fll.xml.WinnerType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Display the report for scores by score group.
 */
@WebServlet("/report/CategoryScoresByJudge")
public class CategoryScoresByJudge extends BaseFLLServlet {

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table name, winner criteria determines sort")
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/CategoryScoresByJudge")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final PrintWriter writer = response.getWriter();
    writer.write("<html><body>");

    // cache the subjective categories title->dbname
    final Map<String, String> subjectiveCategories = new HashMap<>();
    final Map<String, ScoreType> categoryScoreType = new HashMap<>();
    for (final ScoreCategory subjectiveElement : challengeDescription.getSubjectiveCategories()) {
      final String title = subjectiveElement.getTitle();
      final String name = subjectiveElement.getName();
      subjectiveCategories.put(title, name);
      categoryScoreType.put(name, subjectiveElement.getScoreType());
    }

    ResultSet rs = null;
    PreparedStatement prep = null;
    PreparedStatement judgesPrep = null;
    ResultSet judgesRS = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);
      final String tournamentName = Queries.getCurrentTournamentName(connection);

      writer.format("<h1>%s - %s: Categorized Score Summary by judge</h1>", challengeDescription.getTitle(),
                    tournamentName);
      writer.write("<hr/>");

      // foreach division
      for (final String division : Queries.getAwardGroups(connection)) {

        // foreach subjective category
        for (final Map.Entry<String, String> entry : subjectiveCategories.entrySet()) {
          final String categoryTitle = entry.getKey();
          final String categoryName = entry.getValue();
          final ScoreType scoreType = categoryScoreType.get(categoryName);

          judgesPrep = connection.prepareStatement("SELECT DISTINCT "
              + categoryName + ".Judge"//
              + " FROM " + categoryName + ",current_tournament_teams"//
              + " WHERE " + categoryName + ".TeamNumber = current_tournament_teams.TeamNumber" + " AND " + categoryName
              + ".Tournament = ?" + " AND current_tournament_teams.event_division = ?");
          judgesPrep.setInt(1, currentTournament);
          judgesPrep.setString(2, division);
          judgesRS = judgesPrep.executeQuery();

          // select from FinalScores
          while (judgesRS.next()) {
            final String judge = judgesRS.getString(1);

            writer.write("<h3>"
                + categoryTitle + " Award Group: " + division + " Judge: " + judge + "</h3>");

            writer.write("<table border='0'>");
            writer.write("<tr><th colspan='3'>Team # / Organization / Team Name</th><th>Raw Score</th><th>Scaled Score</th></tr>");

            prep = connection.prepareStatement("SELECT"//
                + " Teams.TeamNumber"//
                + ",Teams.Organization"//
                + ",Teams.TeamName"//
                + "," + categoryName + ".ComputedTotal"//
                + "," + categoryName + ".StandardizedScore"//
                + " FROM Teams, " + categoryName//
                + " WHERE Teams.TeamNumber = " + categoryName + ".TeamNumber"//
                + " AND Tournament = ?"//
                + " AND Judge = ?"//
                + " AND " + categoryName + ".ComputedTotal IS NOT NULL"//
                + " ORDER BY " + categoryName + ".ComputedTotal " + winnerCriteria.getSortString() // get
                                                                                                   // best
                                                                                                   // score
                                                                                                   // first
            );
            prep.setInt(1, currentTournament);
            prep.setString(2, judge);
            rs = prep.executeQuery();
            while (rs.next()) {
              final int teamNum = rs.getInt(1);
              final String org = rs.getString(2);
              final String name = rs.getString(3);
              final double score = rs.getDouble(4);
              final boolean scoreWasNull = rs.wasNull();
              final double standardizedScore = rs.getDouble(5);
              final boolean rawScoreWasNull = rs.wasNull();

              writer.write("<tr>");
              writer.write("<td>");
              writer.write(String.valueOf(teamNum));
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
                writer.write(Utilities.getFormatForScoreType(scoreType).format(score));
              } else {
                writer.write("<td align='center' class='warn'>No Score");
              }
              writer.write("</td>");

              if (!rawScoreWasNull) {
                writer.write("<td>");
                writer.write(Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(standardizedScore));
              } else {
                writer.write("<td align='center' class='warn'>No Score");
              }
              writer.write("</td>");

              writer.write("</tr>");
            } // foreach team
            writer.write("<tr><td colspan='5'><hr/></td></tr>");
            writer.write("</table>");
            SQLFunctions.close(rs);
            SQLFunctions.close(prep);
          } // foreach judge
          SQLFunctions.close(judgesRS);
          SQLFunctions.close(judgesPrep);
        } // foreach category

      } // foreach division

    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(judgesRS);
      SQLFunctions.close(judgesPrep);
      SQLFunctions.close(connection);
    }

    writer.write("</body></html>");
  }
}
