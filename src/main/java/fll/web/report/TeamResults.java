/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;

import fll.SubjectiveScore;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.documents.writers.SubjectivePdfWriter;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Creates a zip file with the results for the teams.
 */
@WebServlet("/report/TeamResults")
public class TeamResults extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    final String teamNumberStr = request.getParameter("TeamNumber");
    final int selectedTeamNumber = null == teamNumberStr ? Team.NULL_TEAM_NUMBER : Integer.parseInt(teamNumberStr);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);

    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, tournament.getTournamentID());

      response.reset();
      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "filename=team-results.zip");

      try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
        // go for speed and not compression
        zipOut.setLevel(Deflater.NO_COMPRESSION);

        for (final Map.Entry<Integer, TournamentTeam> entry : teams.entrySet()) {
          final TournamentTeam team = entry.getValue();
          if (Team.NULL_TEAM_NUMBER == selectedTeamNumber
              || selectedTeamNumber == team.getTeamNumber()) {
            writeTeamEntries(zipOut, description, connection, tournamentData, tournament, team);
          }
        }
      }

    } catch (final SQLException e) {
      throw new FLLInternalException("Error creating the team results report", e);
    }
  }

  private void writeTeamEntries(final ZipOutputStream zipOut,
                                final ChallengeDescription description,
                                final Connection connection,
                                final TournamentData tournamentData,
                                final Tournament tournament,
                                final TournamentTeam team)
      throws SQLException, IOException {
    final String directory = String.valueOf(team.getTeamNumber());

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      final Collection<SubjectiveScore> scores = SubjectiveScore.getScoresForTeam(connection, category, tournament,
                                                                                  team);
      final String filename = String.format("%s/%s_%s.pdf", directory, directory, category.getTitle());

      zipOut.putNextEntry(new ZipEntry(filename));

      SubjectivePdfWriter.createDocumentForScores(connection, tournament, zipOut, description, category, scores);

      zipOut.closeEntry();
    } // foreach subjective category

    writePerformance(zipOut, description, tournamentData, connection, team, directory);
  }

  private void writePerformance(final ZipOutputStream zipOut,
                                final ChallengeDescription description,
                                final TournamentData tournamentData,
                                final Connection connection,
                                final TournamentTeam team,
                                final String directory)
      throws SQLException, IOException {
    try {
      final Document document = PerformanceScoreReport.createDocument(tournamentData, connection, description,
                                                                      Collections.singleton(team));

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      final String filename = String.format("%s/%s_Performance.pdf", directory, directory);

      zipOut.putNextEntry(new ZipEntry(filename));

      FOPUtils.renderPdf(fopFactory, document, zipOut);

      zipOut.closeEntry();

    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

}
