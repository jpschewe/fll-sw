/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.AwardDeterminationOrder;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.report.FinalComputedScores;
import fll.web.report.awards.AwardCategory;
import fll.web.report.awards.AwardsScriptReport;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Map of team number to overall score and rank.
 */
@WebServlet("/api/OverallScores")
public class OverallScoresServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isRef()
        && !auth.isJudge()
        && !auth.isReportGenerator()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final Tournament tournament = tournamentData.getCurrentTournament();
      final Collection<String> groups = AwardsScriptReport.getJudgingStationOrder(connection, tournament);
      final List<AwardCategory> awardOrder = Collections.unmodifiableList(AwardDeterminationOrder.get(connection,
                                                                                                      description));

      final Map<Integer, Data> scores = new HashMap<>();

      for (final String groupName : groups) {
        final List<FinalComputedScores.TeamScoreData> groupScoreData = FinalComputedScores.gatherReportData(connection,
                                                                                                            awardOrder,
                                                                                                            description,
                                                                                                            groupName,
                                                                                                            // compute
                                                                                                            // ranks
                                                                                                            // byF
                                                                                                            // judging
                                                                                                            // group
                                                                                                            FinalComputedScores.ReportSelector.JUDGING_STATION,
                                                                                                            tournament);
        for (final FinalComputedScores.TeamScoreData teamScoreData : groupScoreData) {
          final Data data = new Data(teamScoreData.overallScore(), teamScoreData.weightedRank());
          scores.put(teamScoreData.teamNumber(), data);
        }
      }

      jsonMapper.writeValue(writer, scores);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * @param overallScore the overall team score
   * @param weightedRank the team's overall weighted rank
   */
  public record Data(double overallScore,
                     double weightedRank) {
  }

}
