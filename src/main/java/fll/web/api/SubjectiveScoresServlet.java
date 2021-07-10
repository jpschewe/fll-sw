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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.SubjectiveScore;
import fll.Tournament;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.NonNumericNominees;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * API access to subjective scores.
 * GET: {category, {judge, {teamNumber, SubjectiveScore}}}
 * POST: expects the data from GET and returns UploadResult
 */
@WebServlet("/api/SubjectiveScores/*")
public class SubjectiveScoresServlet extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns and category are dynamic")
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

        try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
            + sc.getName()
            + " WHERE Tournament = ?")) {
          prep.setInt(1, currentTournament.getTournamentID());

          try (ResultSet rs = prep.executeQuery()) {
            while (rs.next()) {
              final SubjectiveScore score = SubjectiveScore.fromResultSet(connection, sc, currentTournament, rs);

              final Map<Integer, SubjectiveScore> judgeScores = categoryScores.computeIfAbsent(score.getJudge(),
                                                                                               k -> new HashMap<>());
              judgeScores.put(score.getTeamNumber(), score);

            } // foreach result

            allScores.put(sc.getName(), categoryScores);

          } // allocate result set
        } // allocate prep
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

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns and category are dynamic")
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

    int numModified = 0;
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

      final Map<String, Map<String, Map<Integer, SubjectiveScore>>> allScores = jsonMapper.readValue(reader,
                                                                                                     ScoresTypeInfo.INSTANCE);
      for (final Map.Entry<String, Map<String, Map<Integer, SubjectiveScore>>> catEntry : allScores.entrySet()) {
        final String category = catEntry.getKey();
        final SubjectiveScoreCategory categoryDescription = challengeDescription.getSubjectiveCategoryByName(category);
        if (null == categoryDescription) {
          throw new FLLRuntimeException("Category with name '"
              + category
              + "' is not known");
        }

        try (PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM "
            + category //
            + " WHERE TeamNumber = ?" //
            + " AND Tournament = ?" //
            + " AND Judge = ?" //
        );
            PreparedStatement noShowPrep = connection.prepareStatement("INSERT INTO "
                + category //
                + "(TeamNumber, Tournament, Judge, NoShow) VALUES(?, ?, ?, ?)");

            PreparedStatement insertPrep = createInsertStatement(connection, categoryDescription);) {
          deletePrep.setInt(2, currentTournament.getTournamentID());

          noShowPrep.setInt(2, currentTournament.getTournamentID());
          noShowPrep.setBoolean(4, true);

          final int columnIndexOfFirstGoal = 8;
          insertPrep.setInt(2, currentTournament.getTournamentID());
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
                insertPrep.setString(6, score.getCommentGreatJob());
                insertPrep.setString(7, score.getCommentThinkAbout());

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
                  noShowPrep.executeUpdate();
                } else {
                  if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("scores for team: "
                        + teamNumber
                        + " judge: "
                        + judgeId
                        + " category: "
                        + category);
                  }

                  int goalIndex = 0;
                  final Map<String, Double> standardSubScores = score.getStandardSubScores();
                  final Map<String, String> enumSubScores = score.getEnumSubScores();
                  final Map<String, String> goalComments = score.getGoalComments();
                  for (final AbstractGoal goalDescription : categoryDescription.getAllGoals()) {
                    if (!goalDescription.isComputed()) {

                      // goal score
                      final String goalName = goalDescription.getName();
                      if (goalDescription.isEnumerated()) {
                        final String value = enumSubScores.get(goalName);
                        if (null == value) {
                          insertPrep.setNull(goalIndex
                              + columnIndexOfFirstGoal, Types.VARCHAR);
                        } else {
                          insertPrep.setString(goalIndex
                              + columnIndexOfFirstGoal, value.trim());
                        }
                      } else {
                        final Double value = standardSubScores.get(goalName);
                        if (null == value) {
                          insertPrep.setNull(goalIndex
                              + columnIndexOfFirstGoal, Types.DOUBLE);
                        } else {
                          insertPrep.setDouble(goalIndex
                              + columnIndexOfFirstGoal, value);
                        }
                      }
                      ++goalIndex;

                      // goal comment
                      final String goalComment = goalComments.get(goalName);
                      if (null == goalComment) {
                        insertPrep.setNull(goalIndex
                            + columnIndexOfFirstGoal, Types.LONGVARCHAR);
                      } else {
                        insertPrep.setString(goalIndex
                            + columnIndexOfFirstGoal, goalComment);
                      }
                      ++goalIndex;

                    } // not computed

                  } // end foreach goal

                  deletePrep.executeUpdate();
                  insertPrep.executeUpdate();
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

      removeNullSubjectiveRows(connection, currentTournament.getTournamentID(), challengeDescription);

      final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament.getTournamentID());
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
    }
  }

  /**
   * Remove subjective score rows from database that are empty. These
   * are rows that have null for all scores and is not a no show.
   *
   * @param connection database connection
   * @param tournamentId which tournament to work on
   * @param challengeDescription the challenge description
   */
  private static void removeNullSubjectiveRows(final Connection connection,
                                               final int tournamentId,
                                               final ChallengeDescription challengeDescription)
      throws SQLException {
    for (final SubjectiveScoreCategory cat : challengeDescription.getSubjectiveCategories()) {
      removeNullRows(tournamentId, connection, cat.getName(), cat);
    }
  }

  /**
   * Remove rows from the specified subjective category that are empty. These
   * are rows that have null for all scores and is not a no show.
   *
   * @param currentTournament
   * @param connection
   * @param categoryName
   * @param categoryElement
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns are dynamic")
  private static void removeNullRows(final int currentTournament,
                                     final Connection connection,
                                     final String categoryName,
                                     final ScoreCategory categoryElement)
      throws SQLException {
    final List<AbstractGoal> goalDescriptions = categoryElement.getAllGoals();

    final StringBuffer sql = new StringBuffer();
    sql.append("DELETE FROM "
        + categoryName
        + " WHERE NoShow <> ? ");
    for (final AbstractGoal goalDescription : goalDescriptions) {
      sql.append(" AND "
          + goalDescription.getName()
          + " IS NULL ");
    }

    sql.append(" AND Tournament = ?");

    try (PreparedStatement prep = connection.prepareStatement(sql.toString())) {
      prep.setBoolean(1, true);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }
  }

  /**
   * Create the statement for inserting a score into category.
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns and category are dynamic")
  private PreparedStatement createInsertStatement(final Connection connection,
                                                  final SubjectiveScoreCategory categoryDescription)
      throws SQLException {
    final List<AbstractGoal> goalDescriptions = categoryDescription.getAllGoals();

    final StringBuffer insertSQLColumns = new StringBuffer();
    insertSQLColumns.append("INSERT INTO "
        + categoryDescription.getName()
        + " (TeamNumber, Tournament, Judge, NoShow, note, comment_great_job, comment_think_about");
    final StringBuffer insertSQLValues = new StringBuffer();
    insertSQLValues.append(") VALUES ( ?, ?, ?, ?, ?, ?, ?");

    for (final AbstractGoal goalDescription : goalDescriptions) {
      if (!goalDescription.isComputed()) {
        insertSQLColumns.append(", "
            + goalDescription.getName());
        insertSQLValues.append(", ?");

        // goal comment
        insertSQLColumns.append(", "
            + GenerateDB.getGoalCommentColumnName(goalDescription));
        insertSQLValues.append(", ?");
      }
    }

    final PreparedStatement insertPrep = connection.prepareStatement(insertSQLColumns.toString()
        + insertSQLValues.toString()
        + ")");

    return insertPrep;
  }

  /**
   * Result to send back after an upload.
   */
  public static final class UploadResult {

    /**
     * @param success {@link #getSuccess()}
     * @param message {@link #getMessage()}
     * @param numModified {@link #getNumModified()}
     */
    public UploadResult(final boolean success,
                        final @Nullable String message,
                        final int numModified) {
      mSuccess = success;
      mMessage = message;
      mNumModified = numModified;
    }

    private final boolean mSuccess;

    /**
     * @return if the upload was successful
     */
    public boolean getSuccess() {
      return mSuccess;
    }

    private final @Nullable String mMessage;

    /**
     * @return message for the user
     */
    public @Nullable String getMessage() {
      return mMessage;
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
