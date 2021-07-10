/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.opencsv.CSVWriter;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * CSV file for seeding round performance scores.
 */
@WebServlet("/report/PerformanceScoreDump")
public class PerformanceScoreDump extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();

      final int tournamentID = Queries.getCurrentTournament(connection);

      response.reset();
      response.setContentType("text/csv");
      response.setHeader("Content-Disposition", "filename=performance_scores.csv");

      try (CSVWriter csv = new CSVWriter(response.getWriter())) {

        writeHeader(csv);
        writeData(connection, tournamentID, performanceScoreType, csv);
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write out the data.
   * 
   * @param connection where to get the data from
   * @param tournamentID which tournament
   * @param csv where to write
   * @throws SQLException problem talking to the database
   */
  private void writeData(final Connection connection,
                         final int tournamentID,
                         final ScoreType performanceScoreType,
                         final CSVWriter csv)
      throws SQLException {

    PreparedStatement getScores = null;
    ResultSet scores = null;
    try {
      getScores = connection.prepareStatement("SELECT Teams.TeamName, Performance.TeamNumber, Performance.RunNumber, Performance.ComputedTotal"//
          + ", TournamentTeams.event_division, TournamentTeams.judging_station" //
          + " FROM Teams, Performance, TournamentTeams" //
          + " WHERE Teams.TeamNumber = Performance.TeamNumber" //
          + " AND Teams.TeamNumber = TournamentTeams.TeamNumber"
          + " AND Performance.Tournament = TournamentTeams.Tournament" //
          + " AND Performance.Tournament = ?" //
          + " AND Performance.verified = TRUE" //
          + " AND Performance.Bye = FALSE");
      getScores.setInt(1, tournamentID);

      scores = getScores.executeQuery();
      while (scores.next()) {
        final String teamName = castNonNull(scores.getString(1));
        final int teamNumber = scores.getInt(2);
        final int runNumber = scores.getInt(3);
        final double score = scores.getDouble(4);
        final String eventDivision = castNonNull(scores.getString(5));
        final String judgingStation = castNonNull(scores.getString(6));

        final String[] row = new String[] { Integer.toString(teamNumber), teamName, Integer.toString(runNumber),
                                            Utilities.getFormatForScoreType(performanceScoreType).format(score),
                                            eventDivision, judgingStation };
        csv.writeNext(row);
      }

    } finally {
      SQLFunctions.close(scores);
      SQLFunctions.close(getScores);
    }

  }

  /**
   * Write out the header
   * 
   * @param csv where to write
   */
  private void writeHeader(final CSVWriter csv) {
    csv.writeNext(new String[] { "team#", "team name", "round", "score", "award_group", "judging_group" });
  }

}
