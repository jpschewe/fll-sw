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
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import fll.SubjectiveScore;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.documents.writers.SubjectivePdfWriter;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Creates a zip file with the rubrics for a category and judge.
 */
@WebServlet("/report/JudgeRubrics")
public class JudgeRubrics extends BaseFLLServlet {

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

    final String categoryName = WebUtils.getNonNullRequestParameter(request, "category_name");
    final String judge = WebUtils.getNonNullRequestParameter(request, "judge");

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      response.reset();
      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "filename=judge-rubrics.zip");

      final SubjectiveScoreCategory category = description.getSubjectiveCategoryByName(categoryName);
      if (null == category) {
        throw new FLLRuntimeException(String.format("Cannot find category with name %s", categoryName));
      }

      try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
        // go for speed and not compression
        zipOut.setLevel(Deflater.NO_COMPRESSION);

        for (final TournamentTeam team : Queries.getTournamentTeams(connection, tournament.getTournamentID())
                                                .values()) {
          final String directory = String.valueOf(team.getTeamNumber());

          final Collection<SubjectiveScore> scores = SubjectiveScore.getScoresForTeam(connection, category, tournament,
                                                                                      team)
                                                                    .stream().filter(s -> s.getJudge().equals(judge))
                                                                    .toList();
          if (!scores.isEmpty()) {
            final String filename = String.format("%s/%s.pdf", directory, category.getTitle());
            zipOut.putNextEntry(new ZipEntry(filename));
            SubjectivePdfWriter.createDocumentForScores(connection, tournament, zipOut, description, category, scores);
            zipOut.closeEntry();
          }
        } // foreach team
      }

    } catch (final SQLException e) {
      throw new FLLInternalException("Error creating the team results report", e);
    }
  }

}
