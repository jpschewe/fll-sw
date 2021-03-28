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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.report.FinalComputedScores;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.WinnerType;

/**
 * Map of category title to team number to score.
 */
@WebServlet("/api/NumericCategoryScores")
public class NumericCategoryScoresServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isRef()
        && !auth.isJudge()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Map<String, Map<Integer, Double>> scores = new HashMap<>();

      final WinnerType winnerCriteria = description.getWinner();
      final List<String> awardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());
      final List<String> judgingStations = Queries.getJudgingStations(connection, tournament.getTournamentID());
      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final String categoryTitle = category.getTitle();

        final Map<Integer, Double> categoryScores = scores.computeIfAbsent(categoryTitle, k -> new HashMap<>());

        for (final String awardGroup : awardGroups) {
          for (final String judgingStation : judgingStations) {
            FinalComputedScores.iterateOverSubjectiveScores(connection, category, winnerCriteria, tournament,
                                                            awardGroup, judgingStation, (teamNumber,
                                                                                         score,
                                                                                         rank) -> {

                                                              categoryScores.put(teamNumber, score);
                                                            });
          } // judging station
        } // award group
      } // category

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      jsonMapper.writeValue(writer, scores);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
