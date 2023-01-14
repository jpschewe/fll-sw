/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.JudgeInformation;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.report.finalist.FinalistSchedule;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Populate the page context for the report index page.
 */
public final class ReportIndex {

  private ReportIndex() {
  }

  /**
   * @param session session variables
   * @param application application context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {

    // clear out some variables
    session.removeAttribute(PromptSummarizeScores.SUMMARY_REDIRECT_KEY);

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?")) {
        prep.setInt(1, Queries.getCurrentTournament(connection));

        try (ResultSet rs = prep.executeQuery()) {
          final int maxRunNumber;
          if (rs.next()) {
            maxRunNumber = rs.getInt(1);
          } else {
            maxRunNumber = 1;
          }
          pageContext.setAttribute("maxRunNumber", maxRunNumber);
        } // result set
      } // prepared statement

      pageContext.setAttribute("tournamentTeams", Queries.getTournamentTeams(connection).values());

      final Collection<String> finalistDivisions = FinalistSchedule.getAllDivisions(connection, tournament);
      pageContext.setAttribute("finalistDivisions", finalistDivisions);

      // gather up judges per category
      final Collection<JudgeInformation> judges = JudgeInformation.getJudges(connection, tournament);
      final Map<String, List<String>> categoryJudges = new HashMap<>();
      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final List<String> judgesInCategory = judges.stream().filter(ji -> ji.getCategory().equals(category.getName()))
                                                    .map(JudgeInformation::getId).toList();
        categoryJudges.put(category.getName(), judgesInCategory);
      }

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      // assume that the string is going to be put inside single quotes in the
      // javascript code
      final String categoryJudgesJson = jsonMapper.writeValueAsString(categoryJudges).replace("'", "\\'");
      pageContext.setAttribute("categoryJudgesJson", categoryJudgesJson);

      pageContext.setAttribute("awardGroups", Queries.getAwardGroups(connection, tournament));
      pageContext.setAttribute("judgingStations", Queries.getJudgingStations(connection, tournament));
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    } catch (final JsonProcessingException e) {
      throw new FLLRuntimeException("Error converting variable to JSON", e);
    }
  }

}
