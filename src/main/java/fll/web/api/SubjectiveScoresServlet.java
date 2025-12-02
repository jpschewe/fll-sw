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
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.NonNumericNominees;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * API access to subjective scores.
 * GET: {category, {judge, {teamNumber, SubjectiveScore}}}
 * POST: expects the data from GET and returns UploadResult
 */
@WebServlet("/api/SubjectiveScores/*")
public class SubjectiveScoresServlet extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isJudge()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Tournament currentTournament = Tournament.getCurrentTournament(connection);

      // category->judge->teamNumber->score
      final Map<String, Map<String, Map<Integer, SubjectiveScore>>> allScores = new HashMap<String, Map<String, Map<Integer, SubjectiveScore>>>();

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      for (final SubjectiveScoreCategory sc : challengeDescription.getSubjectiveCategories()) {
        // judge->teamNumber->score
        final Map<String, Map<Integer, SubjectiveScore>> categoryScores = new HashMap<String, Map<Integer, SubjectiveScore>>();

        for (final SubjectiveScore score : SubjectiveScore.getCategoryScores(connection, sc, currentTournament)) {
          final Map<Integer, SubjectiveScore> judgeScores = categoryScores.computeIfAbsent(score.getJudge(),
                                                                                           k -> new HashMap<>());
          judgeScores.put(score.getTeamNumber(), score);

        } // foreach result

        allScores.put(sc.getName(), categoryScores);
      } // foreach category

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();
      jsonMapper.writeValue(writer, allScores);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isJudge()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    final ServletContext application = getServletContext();

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament currentTournament = Tournament.getCurrentTournament(connection);

      final StringWriter debugWriter = new StringWriter();
      request.getReader().transferTo(debugWriter);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Read data: "
            + debugWriter.toString());
      }

      final Reader reader = new StringReader(debugWriter.toString());

      // category -> judge -> team -> score
      final Map<String, Map<String, Map<Integer, SubjectiveScore>>> allScores = jsonMapper.readValue(reader,
                                                                                                     ScoresTypeInfo.INSTANCE);

      final int numModified = processScores(connection, challengeDescription, currentTournament, allScores);

      final UploadResult result = new UploadResult(true, Optional.of("Successfully uploaded scores"), numModified);
      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();
      jsonMapper.writeValue(writer, result);

    } catch (final SQLException sqle) {
      LOGGER.error("Error uploading scores", sqle);

      final UploadResult result = new UploadResult(false, Optional.ofNullable(sqle.getMessage()), -1);
      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();
      jsonMapper.writeValue(writer, result);
    }
  }

  /**
   * Process uploaded scores.
   * 
   * @param connection database connection
   * @param challengeDescription description for the tournament
   * @param currentTournament the tournament to process scores for
   * @param allScores the scores to process
   * @return the number of modified scores
   * @throws SQLException on a database error
   */
  public static int processScores(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final Tournament currentTournament,
                                  final Map<String, Map<String, Map<Integer, SubjectiveScore>>> allScores)
      throws SQLException {

    int numModified = 0;
    for (final Map.Entry<String, Map<String, Map<Integer, SubjectiveScore>>> catEntry : allScores.entrySet()) {
      final String category = catEntry.getKey();
      final SubjectiveScoreCategory categoryDescription = challengeDescription.getSubjectiveCategoryByName(category);
      if (null == categoryDescription) {
        throw new FLLRuntimeException("Category with name '"
            + category
            + "' is not known");
      }

      try (PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM subjective "
          + " WHERE tournament_id " //
          + " AND category_name = ?" //
          + " AND judge = ?" //
          + " AND = team_number = ?" //
      );

          PreparedStatement insert = connection.prepareStatement("INSERT INTO subjective"
              + "(tounament_id, category_name, judge, team_number, NoShow, note, comment_great_job, comment_think_about)" //
              + " VALUES(?, ?, ?, ?, ?, ?, ?, ?)");

          PreparedStatement insertSimpleGoal = connection.prepareStatement("INSERT INTO subjective_goals" //
              + " (tournament_id, category_name, judge, team_number, goal_name, goal_value, comment)" //
              + " VALUES(?, ?, ?, ?, ?, ?, ?)" //
          );

          PreparedStatement insertEnumGoal = connection.prepareStatement("INSERT INTO subjective_enum_goals" //
              + " (tournament_id, category_name, judge, team_number, goal_name, goal_value, comment)" //
              + " VALUES(?, ?, ?, ?, ?, ?, ?)" //
          )) {
        deletePrep.setInt(1, currentTournament.getTournamentID());
        deletePrep.setString(2, category);

        insert.setInt(1, currentTournament.getTournamentID());
        insert.setString(2, category);

        insertSimpleGoal.setInt(1, currentTournament.getTournamentID());
        insertSimpleGoal.setString(2, category);

        insertEnumGoal.setInt(1, currentTournament.getTournamentID());
        insertEnumGoal.setString(2, category);

        for (final Map.Entry<String, Map<Integer, SubjectiveScore>> judgeEntry : catEntry.getValue().entrySet()) {
          final String judgeId = judgeEntry.getKey();
          deletePrep.setString(3, judgeId);

          insert.setString(3, judgeId);
          insertSimpleGoal.setString(3, judgeId);
          insertEnumGoal.setString(3, judgeId);

          for (final Map.Entry<Integer, SubjectiveScore> teamEntry : judgeEntry.getValue().entrySet()) {
            final int teamNumber = teamEntry.getKey();
            deletePrep.setInt(4, teamNumber);
            insert.setInt(4, teamNumber);
            insertSimpleGoal.setInt(4, teamNumber);
            insertEnumGoal.setInt(4, teamNumber);

            final SubjectiveScore score = teamEntry.getValue();

            if (score.getModified()) {
              ++numModified;
              if (score.getDeleted()) {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("Deleting team: "
                      + teamNumber
                      + " judge: "
                      + judgeId
                      + " category: "
                      + category);
                }

                deletePrep.executeUpdate();
              } else if (score.getNoShow()) {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("NoShow team: "
                      + teamNumber
                      + " judge: "
                      + judgeId
                      + " category: "
                      + category);
                }

                deletePrep.executeUpdate();

                insert.setBoolean(5, true);
                insert.setString(6, null);
                insert.setString(7, null);
                insert.setString(8, null);
                insert.executeUpdate();
              } else {
                // update score
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("scores for team: "
                      + teamNumber
                      + " judge: "
                      + judgeId
                      + " category: "
                      + category);
                }

                deletePrep.executeUpdate();

                insert.setBoolean(5, false);
                insert.setString(6, score.getNote());
                insert.setString(7, score.getCommentGreatJob());
                insert.setString(8, score.getCommentThinkAbout());
                insert.executeUpdate();

                // insert goals and goal comments
                boolean insertedGoalValue = false;
                final Map<String, Double> standardSubScores = score.getStandardSubScores();
                final Map<String, String> enumSubScores = score.getEnumSubScores();
                final Map<String, String> goalComments = score.getGoalComments();
                for (final AbstractGoal goalDescription : categoryDescription.getAllGoals()) {
                  if (!goalDescription.isComputed()) {

                    final String goalName = goalDescription.getName();
                    insertSimpleGoal.setString(5, goalName);
                    insertEnumGoal.setString(5, goalName);

                    final String goalComment = goalComments.get(goalName);
                    insertSimpleGoal.setString(7, goalComment);
                    insertEnumGoal.setString(7, goalComment);

                    if (goalDescription.isEnumerated()) {
                      final String value = enumSubScores.get(goalName);
                      if (null != value) {
                        insertEnumGoal.setString(6, value);
                        insertEnumGoal.executeUpdate();
                        insertedGoalValue = true;
                      }

                    } else {
                      final Double value = standardSubScores.get(goalName);
                      if (null != value) {
                        insertSimpleGoal.setDouble(6, value.doubleValue());
                        insertSimpleGoal.executeUpdate();
                        insertedGoalValue = true;
                      }
                    }
                  } // not computed
                } // end foreach goal

                if (!insertedGoalValue) {
                  // if no goal values were stored, then delete any record of the score
                  deletePrep.executeUpdate();
                }

              } // update score

              final Set<String> nominations;
              if (score.getDeleted()) {
                nominations = Collections.emptySet();
              } else if (score.getNoShow()) {
                nominations = Collections.emptySet();
              } else {
                nominations = score.getNonNumericNominations();
              }
              NonNumericNominees.storeNomineesByJudgeForTeam(connection, currentTournament, judgeId, teamNumber,
                                                             nominations);

            } // is modified
          } // foreach team score
        } // foreach judge

      } // allocate statements

    } // foreach category

    final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament.getTournamentID());
    tournament.recordSubjectiveModified(connection);

    return numModified;
  }

  /**
   * Result to send back after an upload.
   */
  public static final class UploadResult extends ApiResult {

    /**
     * @param success {@link #getSuccess()}
     * @param message {@link #getMessage()}
     * @param numModified {@link #getNumModified()}
     */
    public UploadResult(final boolean success,
                        final Optional<String> message,
                        final int numModified) {
      super(success, message);
      mNumModified = numModified;
    }

    private final int mNumModified;

    /**
     * @return number of modified scores
     */
    public int getNumModified() {
      return mNumModified;
    }

  }

  private static final class ScoresTypeInfo
      extends TypeReference<Map<String, Map<String, Map<Integer, SubjectiveScore>>>> {
    public static final ScoresTypeInfo INSTANCE = new ScoresTypeInfo();
  }
}
