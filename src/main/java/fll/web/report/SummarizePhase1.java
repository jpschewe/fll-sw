/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.ScoreStandardization;
import fll.ScoreStandardization.TooFewScoresException;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Do first part of summarizing scores and gather information to show the user
 * about where we are.
 */
public class SummarizePhase1 {

  /**
   * Page key for judge information. Type is a {@link Map} of station to
   * {@link java.util.Collection} of
   * JudgeSummary.
   */
  public static final String JUDGE_SUMMARY = "judgeSummary";

  /**
   * Page key for missing categories. Type is a {@link Map} of station to
   * {@link java.util.Collection} category titles.
   */
  public static final String MISSING_CATEGORIES = "missingCategories";

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate table name from category")
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext)
      throws IOException, ServletException {
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (final Connection connection = datasource.getConnection()) {
      final int tournamentID = Queries.getCurrentTournament(connection);

      Queries.updateScoreTotals(challengeDescription, connection);

      try {
        ScoreStandardization.standardizeSubjectiveScores(connection, tournamentID);
      } catch (final TooFewScoresException e) {
        pageContext.setAttribute("ERROR", e.getMessage());
        return;
      }

      ScoreStandardization.summarizeScores(connection, tournamentID);

      final Map<String, Set<String>> seenCategoryNames = new HashMap<>();
      final SortedMap<String, SortedSet<JudgeSummary>> summary = new TreeMap<>();

      try (final PreparedStatement getJudges = connection.prepareStatement("SELECT id, category, station from Judges"
          + " WHERE Tournament = ?"
          + " ORDER BY station ASC, category ASC")) {
        getJudges.setInt(1, tournamentID);
        try (final ResultSet judges = getJudges.executeQuery()) {
          while (judges.next()) {
            final String judge = judges.getString(1);
            final String categoryName = judges.getString(2);
            final String station = judges.getString(3);

            final int numExpected = getNumScoresExpected(connection, tournamentID, station);
            final int numActual = getNumScoresEntered(connection, judge, categoryName, station, tournamentID);

            if (numActual > 0) {
              final String categoryTitle = challengeDescription.getSubjectiveCategoryByName(categoryName).getTitle();

              final SortedSet<JudgeSummary> value = summary.computeIfAbsent(station, k -> new TreeSet<>());
              value.add(new JudgeSummary(judge, categoryTitle, station, numExpected, numActual));

              seenCategoryNames.computeIfAbsent(station, k -> new HashSet<>()).add(categoryName);
            }

          }
        } // result set
      } // statement

      // add in entries for missing scores

      addMissingCategories(connection, tournamentID, challengeDescription, seenCategoryNames, summary);

      pageContext.setAttribute(JUDGE_SUMMARY, summary);

    } catch (final SQLException e) {
      throw new FLLRuntimeException("There was an error talking to the database", e);
    } catch (final ParseException e) {
      throw new FLLInternalException("There was an error parsing the challenge description", e);
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
        final String title = challengeDescription.getSubjectiveCategoryByName(missingName).getTitle();
        final int expected = getNumScoresExpected(connection, tournamentID, judgingGroup);

        summary.computeIfAbsent(judgingGroup, k -> new TreeSet<>())
               .add(new JudgeSummary(null, title, judgingGroup, expected, 0));
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

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate table name from category")
  private static int getNumScoresEntered(final Connection connection,
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
        + " AND goal_group = ?" //
        + " AND ( computed_total IS NOT NULL OR no_show = true )"//
        + " AND team_number IN (" //
        + "  SELECT TeamNumber FROM TournamentTeams" //
        + "    WHERE Tournament = ?" //
        + "    AND judging_station = ?" //
        + ")")) {
      getActual.setInt(1, tournamentID);
      getActual.setString(2, judge);
      getActual.setString(3, categoryName);
      getActual.setString(4, "");
      getActual.setInt(5, tournamentID);
      getActual.setString(6, station);
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
