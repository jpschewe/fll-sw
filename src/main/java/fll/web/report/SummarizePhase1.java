/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.JudgeInformation;
import fll.ScoreStandardization;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Do first part of summarizing scores and gather information to show the user
 * about where we are.
 */
public final class SummarizePhase1 {

  private SummarizePhase1() {
  }

  /**
   * Page key for judge information. Type is a {@link Map} of station to
   * {@link java.util.Collection} of
   * JudgeSummary.
   */
  public static final String JUDGE_SUMMARY = "judgeSummary";

  /**
   * @param application application variables
   * @param pageContext page variables
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate table name from category")
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      final int tournamentID = Queries.getCurrentTournament(connection);

      ScoreStandardization.updateScoreTotals(challengeDescription, connection, tournamentID);

      ScoreStandardization.summarizeScores(connection, challengeDescription, tournamentID);

      final Map<String, Set<String>> seenCategoryNames = new HashMap<>();
      // judging group -> judge information
      final SortedMap<String, SortedSet<JudgeSummary>> summary = new TreeMap<>();

      final Collection<JudgeInformation> judges = JudgeInformation.getJudges(connection, tournamentID);
      for (final JudgeInformation judgeInfo : judges) {
        final int numExpected = getNumScoresExpected(connection, tournamentID, judgeInfo.getGroup());
        final int numActual = getNumScoresEntered(connection, judgeInfo.getId(), judgeInfo.getCategory(),
                                                  judgeInfo.getGroup(), tournamentID);

        if (numActual > 0) {
          final SubjectiveScoreCategory category = challengeDescription.getSubjectiveCategoryByName(judgeInfo.getCategory());
          if (null == category) {
            throw new FLLInternalException("Category with name '"
                + judgeInfo.getCategory()
                + "' is not known");
          }
          final String categoryTitle = category.getTitle();

          final SortedSet<JudgeSummary> value = summary.computeIfAbsent(judgeInfo.getGroup(), k -> new TreeSet<>());
          value.add(new JudgeSummary(judgeInfo.getId(), categoryTitle, judgeInfo.getGroup(), numExpected, numActual,
                                     judgeInfo.isFinalScores()));

          seenCategoryNames.computeIfAbsent(judgeInfo.getGroup(), k -> new HashSet<>()).add(judgeInfo.getCategory());
        }

      }

      // add in entries for missing scores

      addMissingCategories(connection, tournamentID, challengeDescription, seenCategoryNames, summary);

      pageContext.setAttribute(JUDGE_SUMMARY, summary);

    } catch (

    final SQLException e) {
      throw new FLLRuntimeException("There was an error talking to the database", e);
    }
  }

  private static void addMissingCategories(final Connection connection,
                                           final int tournamentID,
                                           final ChallengeDescription challengeDescription,
                                           final Map<String, Set<String>> seenCategoryNames,
                                           final SortedMap<String, SortedSet<JudgeSummary>> summary)
      throws SQLException {
    final Set<String> allCategoryNames = challengeDescription.getSubjectiveCategories().stream()
                                                             .map(SubjectiveScoreCategory::getName)
                                                             .collect(Collectors.toSet());

    for (final Map.Entry<String, Set<String>> entry : seenCategoryNames.entrySet()) {
      final String judgingGroup = entry.getKey();
      final Set<String> seenCategories = entry.getValue();

      final Set<String> missingNames = new HashSet<>(allCategoryNames);
      missingNames.removeAll(seenCategories);

      for (final String missingName : missingNames) {
        final SubjectiveScoreCategory category = challengeDescription.getSubjectiveCategoryByName(missingName);
        if (null == category) {
          throw new FLLRuntimeException("Category with name '"
              + missingName
              + "' is not known");
        }
        final String title = category.getTitle();
        final int expected = getNumScoresExpected(connection, tournamentID, judgingGroup);

        summary.computeIfAbsent(judgingGroup, k -> new TreeSet<>())
               .add(new JudgeSummary(null, title, judgingGroup, expected, 0, false));
      }
    }
  }

  private static int getNumScoresExpected(final Connection connection,
                                          final int tournamentID,
                                          final String station)
      throws SQLException {
    int numExpected = -1;
    try (PreparedStatement getExpected = connection.prepareStatement("SELECT COUNT(*)"//
        + " FROM TournamentTeams" //
        + " WHERE Tournament = ?"//
        + " AND judging_station = ?")) {
      getExpected.setInt(1, tournamentID);
      getExpected.setString(2, station);
      try (ResultSet expected = getExpected.executeQuery()) {
        if (expected.next()) {
          numExpected = expected.getInt(1);
          if (expected.wasNull()) {
            numExpected = -1;
          }
        }
      } // result set
    } // statement
    return numExpected;
  }

  /**
   * Determine how many scores have been entered in a category by a judge at a
   * judging station.
   * 
   * @param connection database connection
   * @param judge the judge
   * @param categoryName the name of the category
   * @param station the judging station
   * @param tournamentID the id of the tournament
   * @return the number of scores entered
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate table name from category")
  public static int getNumScoresEntered(final Connection connection,
                                        final String judge,
                                        final String categoryName,
                                        final String station,
                                        final int tournamentID)
      throws SQLException {
    int numActual = -1;
    try (PreparedStatement getActual = connection.prepareStatement("SELECT COUNT(*)" //
        + " FROM subjective_computed_scores"
        + " WHERE tournament = ?" //
        + " AND judge = ?" //
        + " AND category = ?" //
        + " AND ( computed_total IS NOT NULL OR no_show = true )"//
        + " AND team_number IN (" //
        + "  SELECT TeamNumber FROM TournamentTeams" //
        + "    WHERE Tournament = ?" //
        + "    AND judging_station = ?" //
        + ")")) {
      getActual.setInt(1, tournamentID);
      getActual.setString(2, judge);
      getActual.setString(3, categoryName);
      getActual.setInt(4, tournamentID);
      getActual.setString(5, station);
      try (ResultSet actual = getActual.executeQuery()) {
        if (actual.next()) {
          numActual = actual.getInt(1);
          if (actual.wasNull()) {
            numActual = -1;
          }
        }
      } // result set
    } // statement
    return numActual;
  }
}
