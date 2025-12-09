/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.JudgeInformation;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.web.WebUtils;
import fll.web.report.awards.AwardsScriptReport;
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
   * @param setRedirect if true, set the redirect URL
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext,
                                     final boolean setRedirect) {

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final int tournamentId = tournament.getTournamentID();

      pageContext.setAttribute("tournamentTeams", Queries.getTournamentTeams(connection, tournamentId).values());

      final Collection<String> finalistDivisions = FinalistSchedule.getAllDivisions(connection, tournamentId);
      pageContext.setAttribute("finalistDivisions", finalistDivisions);

      // gather up judges per category
      final Collection<JudgeInformation> judges = JudgeInformation.getJudges(connection, tournamentId);
      final Map<String, List<String>> categoryJudges = new HashMap<>();
      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final List<String> judgesInCategory = judges.stream().filter(ji -> ji.getCategory().equals(category.getName()))
                                                    .map(JudgeInformation::getId).toList();
        categoryJudges.put(category.getName(), judgesInCategory);
      }

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      // assume that the string is going to be put inside single quotes in the
      // javascript code
      final String categoryJudgesJson = WebUtils.escapeStringForJsonParse(jsonMapper.writeValueAsString(categoryJudges));
      pageContext.setAttribute("categoryJudgesJson", categoryJudgesJson);

      pageContext.setAttribute("awardGroups", AwardsScriptReport.getAwardGroupOrder(connection, tournament));
      pageContext.setAttribute("judgingStations", Queries.getJudgingStations(connection, tournamentId));
      pageContext.setAttribute("sortOrders", FinalComputedScores.SortOrder.values());

      if (setRedirect) {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "/report/index.jsp");
      }
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    } catch (final JsonProcessingException e) {
      throw new FLLRuntimeException("Error converting variable to JSON", e);
    }
  }

}
