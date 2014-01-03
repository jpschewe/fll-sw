/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.BufferedReader;
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.SubjectiveScore;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * GET: {category, {judge, {teamNumber, SubjectiveScore}}}
 * POST: expects the data from GET and returns JSON string "success" or error
 * message
 */
@WebServlet("/api/SubjectiveScores/*")
public class SubjectiveScoresServlet extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);

      // category->judge->teamNumber->score
      final Map<String, Map<String, Map<Integer, SubjectiveScore>>> allScores = new HashMap<String, Map<String, Map<Integer, SubjectiveScore>>>();

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      for (final ScoreCategory sc : challengeDescription.getSubjectiveCategories()) {
        // judge->teamNumber->score
        final Map<String, Map<Integer, SubjectiveScore>> categoryScores = new HashMap<String, Map<Integer, SubjectiveScore>>();

        prep = connection.prepareStatement("SELECT * FROM "
            + sc.getName() + " WHERE Tournament = ?");
        prep.setInt(1, currentTournament);

        rs = prep.executeQuery();
        while (rs.next()) {
          final SubjectiveScore score = new SubjectiveScore();

          final String judge = rs.getString("Judge");
          final Map<Integer, SubjectiveScore> judgeScores;
          if (categoryScores.containsKey(judge)) {
            judgeScores = categoryScores.get(judge);
          } else {
            judgeScores = new HashMap<Integer, SubjectiveScore>();
            categoryScores.put(judge, judgeScores);
          }

          score.setTeamNumber(rs.getInt("TeamNumber"));
          score.setJudge(judge);
          score.setNoShow(rs.getBoolean("NoShow"));

          final Map<String, Double> standardSubScores = new HashMap<String, Double>();
          final Map<String, String> enumSubScores = new HashMap<String, String>();
          for (final AbstractGoal goal : sc.getGoals()) {
            if (goal.isEnumerated()) {
              final String value = rs.getString(goal.getName());
              enumSubScores.put(goal.getName(), value);
            } else {
              final double value = rs.getDouble(goal.getName());
              standardSubScores.put(goal.getName(), value);
            }
          }
          score.setStandardSubScores(standardSubScores);
          score.setEnumSubScores(enumSubScores);

          judgeScores.put(score.getTeamNumber(), score);
        }

        allScores.put(sc.getName(), categoryScores);

        SQLFunctions.close(rs);
        rs = null;
        SQLFunctions.close(prep);
        prep = null;
      }

      final ObjectMapper jsonMapper = new ObjectMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();
      jsonMapper.writeValue(writer, allScores);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }

  }

  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response) throws IOException, ServletException {
    final BufferedReader reader = request.getReader();
    final ObjectMapper jsonMapper = new ObjectMapper();

    final Map<String, Map<String, Map<Integer, SubjectiveScore>>> allScores = jsonMapper.readValue(reader,
                                                                                                   new TypeReference<Map<String, Map<String, Map<Integer, SubjectiveScore>>>>() {
                                                                                                   });
    for (final Map.Entry<String, Map<String, Map<Integer, SubjectiveScore>>> catEntry : allScores.entrySet()) {
      final String category = catEntry.getKey();
      for (final Map.Entry<String, Map<Integer, SubjectiveScore>> judgeEntry : catEntry.getValue().entrySet()) {
        final String judgeId = judgeEntry.getKey();
        for (final Map.Entry<Integer, SubjectiveScore> teamEntry : judgeEntry.getValue().entrySet()) {
          final int teamNumber = teamEntry.getKey();
          final SubjectiveScore score = teamEntry.getValue();
          if (score.getModified()) {
            if (score.getDeleted()) {
              // FIXME delete
              LOGGER.info("Deleting team: "
                  + teamNumber + " judge: " + judgeId + " category: " + category);
            } else if (score.getNoShow()) {
              // FIXME enter no show
              LOGGER.info("NoShow team: "
                  + teamNumber + " judge: " + judgeId + " category: " + category);
            } else {
              // FIXME enter all scores
              LOGGER.info("scores for team: "
                  + teamNumber + " judge: " + judgeId + " category: " + category);
            }
          }
        }
      }
    }

  }
}
