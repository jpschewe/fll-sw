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
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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

/**
 * Do first part of summarizing scores and gather information to show the user
 * about where we are.
 */
public class SummarizePhase1 {

  /**
   * Page key for judge information. Type is Collection of JudgeSummary.
   */
  public static final String JUDGE_SUMMARY = "judgeSummary";

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
        ScoreStandardization.standardizeSubjectiveScores(connection, challengeDescription, tournamentID);
      } catch (final TooFewScoresException e) {
        pageContext.setAttribute("ERROR", e.getMessage());
        return;
      }

      ScoreStandardization.summarizeScores(connection, challengeDescription, tournamentID);

      final SortedMap<String, List<JudgeSummary>> summary = new TreeMap<>();

      try (PreparedStatement getJudges = connection.prepareStatement("SELECT id, category, station from Judges"
          + " WHERE Tournament = ?"
          + " ORDER BY station ASC, category ASC")) {
        getJudges.setInt(1, tournamentID);
        try (ResultSet judges = getJudges.executeQuery()) {
          while (judges.next()) {
            final String judge = judges.getString(1);
            final String categoryName = judges.getString(2);
            final String station = judges.getString(3);

            final int numExpected = getNumScoresExpected(connection, tournamentID, station);
            final int numActual = getNumScoresEntered(connection, judge, categoryName, station, tournamentID);

            final List<JudgeSummary> value = summary.computeIfAbsent(station, k -> new LinkedList<>());
            value.add(new JudgeSummary(judge, categoryName, station, numExpected, numActual));

          }
        } // result set
      } // statement

      pageContext.setAttribute(JUDGE_SUMMARY, summary);

    } catch (final SQLException e) {
      throw new FLLRuntimeException("There was an error talking to the database", e);
    } catch (final ParseException e) {
      throw new FLLInternalException("There was an error parsing the challenge description", e);
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
        + " FROM "
        + categoryName //
        + " WHERE tournament = ?" //
        + " AND Judge = ?" //
        + " AND ( ComputedTotal IS NOT NULL OR NoShow = true )"//
        + " AND TeamNumber IN (" //
        + "  SELECT TeamNumber FROM TournamentTeams" //
        + "    WHERE Tournament = ?" //
        + "    AND judging_station = ?" //
        + ")")) {
      getActual.setInt(1, tournamentID);
      getActual.setString(2, judge);
      getActual.setInt(3, tournamentID);
      getActual.setString(4, station);
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
