/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.SubjectiveScore;
import fll.Tournament;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.admin.UploadSubjectiveData;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * GET: {category, {judge, {teamNumber, SubjectiveScore}}}
 * POST: expects the data from GET and returns UploadResult
 */
@WebServlet("/api/SubjectiveScores/*")
public class SubjectiveScoresServlet extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns and category are dynamic")
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
          score.setScoreOnServer(true);

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
          score.setNote(rs.getString("note"));

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

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns and category are dynamic")
  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response) throws IOException, ServletException {
    int numModified = 0;
    final ObjectMapper jsonMapper = new ObjectMapper();

    final ServletContext application = getServletContext();

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    Connection connection = null;
    PreparedStatement deletePrep = null;
    PreparedStatement noShowPrep = null;
    PreparedStatement insertPrep = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);

      final StringWriter debugWriter = new StringWriter();
      IOUtils.copy(request.getReader(), debugWriter);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Read data: "
            + debugWriter.toString());
      }

      final Reader reader = new StringReader(debugWriter.toString());

      final Map<String, Map<String, Map<Integer, SubjectiveScore>>> allScores = jsonMapper.readValue(reader,
                                                                                                     ScoresTypeInfo.INSTANCE);
      for (final Map.Entry<String, Map<String, Map<Integer, SubjectiveScore>>> catEntry : allScores.entrySet()) {
        final String category = catEntry.getKey();
        final ScoreCategory categoryDescription = challengeDescription.getSubjectiveCategoryByName(category);

        deletePrep = connection.prepareStatement("DELETE FROM "
            + category //
            + " WHERE TeamNumber = ?" //
            + " AND Tournament = ?" //
            + " AND Judge = ?" //
        );
        deletePrep.setInt(2, currentTournament);

        noShowPrep = connection.prepareStatement("INSERT INTO "
            + category //
            + "(TeamNumber, Tournament, Judge, NoShow) VALUES(?, ?, ?, ?)");
        noShowPrep.setInt(2, currentTournament);
        noShowPrep.setBoolean(4, true);

        final int NUM_COLUMNS_BEFORE_GOALS = 6;
        insertPrep = createInsertStatement(connection, categoryDescription);
        insertPrep.setInt(2, currentTournament);
        insertPrep.setBoolean(4, false);

        for (final Map.Entry<String, Map<Integer, SubjectiveScore>> judgeEntry : catEntry.getValue().entrySet()) {
          final String judgeId = judgeEntry.getKey();
          deletePrep.setString(3, judgeId);
          noShowPrep.setString(3, judgeId);
          insertPrep.setString(3, judgeId);

          for (final Map.Entry<Integer, SubjectiveScore> teamEntry : judgeEntry.getValue().entrySet()) {
            final int teamNumber = teamEntry.getKey();
            final SubjectiveScore score = teamEntry.getValue();

            if (score.getModified()) {
              deletePrep.setInt(1, teamNumber);
              noShowPrep.setInt(1, teamNumber);
              insertPrep.setInt(1, teamNumber);
              insertPrep.setString(5, score.getNote());

              ++numModified;
              if (score.getDeleted()) {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("Deleting team: "
                      + teamNumber + " judge: " + judgeId + " category: " + category);
                }

                deletePrep.executeUpdate();
              } else if (score.getNoShow()) {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("NoShow team: "
                      + teamNumber + " judge: " + judgeId + " category: " + category);
                }

                deletePrep.executeUpdate();
                noShowPrep.executeUpdate();
              } else {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("scores for team: "
                      + teamNumber + " judge: " + judgeId + " category: " + category);
                }

                int goalIndex = 0;
                for (final AbstractGoal goalDescription : categoryDescription.getGoals()) {
                  if (!goalDescription.isComputed()) {

                    final String goalName = goalDescription.getName();
                    if (goalDescription.isEnumerated()) {
                      final String value = score.getEnumSubScores().get(goalName);
                      if (null == value) {
                        insertPrep.setNull(goalIndex
                            + NUM_COLUMNS_BEFORE_GOALS, Types.VARCHAR);
                      } else {
                        insertPrep.setString(goalIndex
                            + NUM_COLUMNS_BEFORE_GOALS, value.trim());
                      }
                    } else {
                      final Double value = score.getStandardSubScores().get(goalName);
                      if (null == value) {
                        insertPrep.setNull(goalIndex
                            + NUM_COLUMNS_BEFORE_GOALS, Types.DOUBLE);
                      } else {
                        insertPrep.setDouble(goalIndex
                            + NUM_COLUMNS_BEFORE_GOALS, value);
                      }
                    }
                    ++goalIndex;

                  } // not computed

                } // end for

                deletePrep.executeUpdate();
                insertPrep.executeUpdate();
              }
            } // is modified
          } // foreach team score
        } // foreach judge

        SQLFunctions.close(deletePrep);
        deletePrep = null;

        SQLFunctions.close(noShowPrep);
        noShowPrep = null;

        SQLFunctions.close(insertPrep);
        insertPrep = null;

      } // foreach category

      UploadSubjectiveData.removeNullSubjectiveRows(connection, currentTournament, challengeDescription);
      
      final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament);
      tournament.recordSubjectiveModified(connection);

      final UploadResult result = new UploadResult(true, "Successfully uploaded scores", numModified);
      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();
      jsonMapper.writeValue(writer, result);

    } catch (final SQLException sqle) {
      LOGGER.error("Error uploading scores", sqle);

      final UploadResult result = new UploadResult(false, sqle.getMessage(), numModified);
      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();
      jsonMapper.writeValue(writer, result);

    } finally {
      SQLFunctions.close(deletePrep);
      SQLFunctions.close(noShowPrep);
      SQLFunctions.close(insertPrep);
      SQLFunctions.close(connection);
    }

  }

  /**
   * Create the statement for inserting a score into category.
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns and category are dynamic")
  private PreparedStatement createInsertStatement(final Connection connection,
                                                  final ScoreCategory categoryDescription) throws SQLException {
    final List<AbstractGoal> goalDescriptions = categoryDescription.getGoals();

    final StringBuffer insertSQLColumns = new StringBuffer();
    insertSQLColumns.append("INSERT INTO "
        + categoryDescription.getName() + " (TeamNumber, Tournament, Judge, NoShow, note");
    final StringBuffer insertSQLValues = new StringBuffer();
    insertSQLValues.append(") VALUES ( ?, ?, ?, ?, ?");

    for (final AbstractGoal goalDescription : goalDescriptions) {
      if (!goalDescription.isComputed()) {
        insertSQLColumns.append(", "
            + goalDescription.getName());
        insertSQLValues.append(", ?");
      }
    }

    final PreparedStatement insertPrep = connection.prepareStatement(insertSQLColumns.toString()
        + insertSQLValues.toString() + ")");

    return insertPrep;
  }

  public static final class UploadResult {
    public UploadResult(final boolean success,
                        final String message,
                        final int numModified) {
      mSuccess = success;
      mMessage = message;
      mNumModified = numModified;
    }

    private final boolean mSuccess;

    public boolean getSuccess() {
      return mSuccess;
    }

    private final String mMessage;

    public String getMessage() {
      return mMessage;
    }

    private final int mNumModified;

    public int getNumModified() {
      return mNumModified;
    }

  }

  private static final class ScoresTypeInfo extends
      TypeReference<Map<String, Map<String, Map<Integer, SubjectiveScore>>>> {
    public static final ScoresTypeInfo INSTANCE = new ScoresTypeInfo();
  }
}
