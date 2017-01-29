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
import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.ScoreStandardization;
import fll.db.Queries;
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
                                     final PageContext pageContext) throws IOException, ServletException {
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    PreparedStatement getJudges = null;
    PreparedStatement getExpected = null;
    PreparedStatement getActual = null;
    ResultSet judges = null;
    ResultSet actual = null;
    ResultSet expected = null;
    try {
      final Connection connection = datasource.getConnection();
      final int tournament = Queries.getCurrentTournament(connection);

      Queries.updateScoreTotals(challengeDescription, connection);

      ScoreStandardization.standardizeSubjectiveScores(connection, challengeDescription, tournament);
      ScoreStandardization.summarizeScores(connection, challengeDescription, tournament);

      final Collection<JudgeSummary> summary = new LinkedList<JudgeSummary>();

      getJudges = connection.prepareStatement("SELECT id, category, station from Judges"
              + " WHERE Tournament = ?"
              + " ORDER BY category ASC, station ASC");
      getJudges.setInt(1, tournament);
      judges = getJudges.executeQuery();
      while (judges.next()) {
        final String judge = judges.getString(1);
        final String category = judges.getString(2);
        final String station = judges.getString(3);

        getExpected = connection.prepareStatement("SELECT COUNT(*) FROM TournamentTeams WHERE Tournament = ? AND judging_station = ?");
        getExpected.setInt(1, tournament);
        getExpected.setString(2, station);
        expected = getExpected.executeQuery();
        int numExpected = -1;
        if (expected.next()) {
          numExpected = expected.getInt(1);
          if (expected.wasNull()) {
            numExpected = -1;
          }
        }

        getActual = connection.prepareStatement("SELECT COUNT(*)" //
            + " FROM " + category //
            + " WHERE tournament = ?" //
            + " AND Judge = ?" //
            + " AND ( ComputedTotal IS NOT NULL OR NoShow = true )"//
            + " AND TeamNumber IN (" //
            + "  SELECT TeamNumber FROM TournamentTeams" //
            + "    WHERE Tournament = ?" //
            + "    AND judging_station = ?" //
            + ")");
        getActual.setInt(1, tournament);
        getActual.setString(2, judge);
        getActual.setInt(3, tournament);
        getActual.setString(4, station);
        actual = getActual.executeQuery();
        int numActual = -1;
        if (actual.next()) {
          numActual = actual.getInt(1);
          if (actual.wasNull()) {
            numActual = -1;
          }
        }

        summary.add(new JudgeSummary(judge, category, station, numExpected, numActual));

      }

      pageContext.setAttribute(JUDGE_SUMMARY, summary);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(judges);
      SQLFunctions.close(actual);
      SQLFunctions.close(expected);
      SQLFunctions.close(getJudges);
      SQLFunctions.close(getExpected);
      SQLFunctions.close(getActual);
    }

  }
}
