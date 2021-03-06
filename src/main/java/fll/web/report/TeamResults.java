/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;

import fll.SubjectiveScore;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.CategoryColumnMapping;
import fll.db.Queries;
import fll.documents.writers.SubjectivePdfWriter;
import fll.scheduler.SubjectiveTime;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

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

    final String teamNumberStr = request.getParameter("TeamNumber");
    final int selectedTeamNumber = null == teamNumberStr ? Team.NULL_TEAM_NUMBER : Integer.parseInt(teamNumberStr);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final TournamentSchedule schedule = new TournamentSchedule(connection, tournament.getTournamentID());

      final Collection<CategoryColumnMapping> scheduleColumnMappings = CategoryColumnMapping.load(connection,
                                                                                                  tournament.getTournamentID());

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
            final TeamScheduleInfo schedInfo = schedule.getSchedInfoForTeam(team.getTeamNumber());

            writeTeamEntries(zipOut, description, connection, tournament, scheduleColumnMappings, team, schedInfo);
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
                                final Tournament tournament,
                                final Collection<CategoryColumnMapping> scheduleColumnMappings,
                                final TournamentTeam team,
                                final TeamScheduleInfo schedInfo)
      throws SQLException, IOException {
    final String directory = String.valueOf(team.getTeamNumber());

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      final Optional<CategoryColumnMapping> categoryMapping = scheduleColumnMappings.stream()
                                                                                    .filter(m -> m.getCategoryName()
                                                                                                  .equals(category.getName()))
                                                                                    .findFirst();
      final String scheduleColumn;
      if (categoryMapping.isPresent()) {
        scheduleColumn = categoryMapping.get().getScheduleColumn();
      } else {
        scheduleColumn = null;
      }

      final Collection<SubjectiveScore> scores = SubjectiveScore.getScoresForTeam(connection, category, tournament,
                                                                                  team);
      final String filename = String.format("%s/%s.pdf", directory, category.getTitle());

      zipOut.putNextEntry(new ZipEntry(filename));

      final LocalTime scheduledTime;
      if (null == scheduleColumn) {
        scheduledTime = null;
      } else {
        final SubjectiveTime stime = schedInfo.getSubjectiveTimeByName(scheduleColumn);
        if (null == stime) {
          scheduledTime = null;
        } else {
          scheduledTime = stime.getTime();
        }
      }

      SubjectivePdfWriter.createDocumentForScores(zipOut, description, tournament.getName(), category, scores,
                                                  team.getTeamNumber(), team.getTeamName(), team.getAwardGroup(),
                                                  scheduledTime);

      zipOut.closeEntry();
    } // foreach subjective category

    writePerformance(zipOut, description, connection, tournament, team, directory);
  }

  private void writePerformance(final ZipOutputStream zipOut,
                                final ChallengeDescription description,
                                final Connection connection,
                                final Tournament tournament,
                                final TournamentTeam team,
                                final String directory)
      throws SQLException, IOException {
    try {
      final Document document = PerformanceScoreReport.createDocument(connection, description, tournament,
                                                                      Collections.singleton(team));

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      final String filename = String.format("%s/Performance.pdf", directory);

      zipOut.putNextEntry(new ZipEntry(filename));

      FOPUtils.renderPdf(fopFactory, document, zipOut);

      zipOut.closeEntry();

    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

}
