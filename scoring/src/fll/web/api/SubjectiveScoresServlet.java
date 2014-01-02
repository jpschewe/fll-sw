/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.SubjectiveScore;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

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

      final Map<String, Collection<SubjectiveScore>> allScores = new HashMap<String, Collection<SubjectiveScore>>();

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      for (final ScoreCategory sc : challengeDescription.getSubjectiveCategories()) {
        final Collection<SubjectiveScore> scores = new LinkedList<SubjectiveScore>();

        prep = connection.prepareStatement("SELECT * FROM "
            + sc.getName() + " WHERE Tournament = ?");
        prep.setInt(1, currentTournament);

        rs = prep.executeQuery();
        while (rs.next()) {
          final SubjectiveScore score = new SubjectiveScore();

          score.setTeamNumber(rs.getInt("TeamNumber"));
          score.setJudge(rs.getString("Judge"));
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

          scores.add(score);
        }

        allScores.put(sc.getName(), scores);

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

}
