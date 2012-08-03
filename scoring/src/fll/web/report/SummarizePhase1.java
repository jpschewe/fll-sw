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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.w3c.dom.Document;

import fll.ScoreStandardization;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Do first part of summarizing scores and gather information to show the user
 * about where we are.
 */
@WebServlet("/report/SummarizePhase1")
public class SummarizePhase1 extends BaseFLLServlet {

  /**
   * Session key for judge information. Type is Collection of JudgeSummary.
   */
  public static final String JUDGE_SUMMARY = "judgeSummary";

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate table name from category")
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
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

      Queries.updateScoreTotals(challengeDocument, connection);

      ScoreStandardization.standardizeSubjectiveScores(connection, challengeDocument, tournament);
      ScoreStandardization.summarizeScores(connection, challengeDocument, tournament);

      final Collection<JudgeSummary> summary = new LinkedList<JudgeSummary>();

      getJudges = connection.prepareStatement("SELECT id, category, station from Judges WHERE Tournament = ?");
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

        getActual = connection.prepareStatement("SELECT COUNT(*) FROM "
            + category + " WHERE tournament = ? AND Judge = ? AND ( ComputedTotal IS NOT NULL OR NoShow = true )");
        getActual.setInt(1, tournament);
        getActual.setString(2, judge);
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

      session.setAttribute(JUDGE_SUMMARY, summary);

      response.sendRedirect(response.encodeRedirectURL("summarizePhase1.jsp"));

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
