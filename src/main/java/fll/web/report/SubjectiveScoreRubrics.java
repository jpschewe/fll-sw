/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.sql.DataSource;

import fll.SubjectiveScore;
import fll.Tournament;
import fll.documents.writers.SubjectivePdfWriter;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
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
 * Generate PDF of subjective scores.
 */
@WebServlet("/report/SubjectiveScoreRubrics")
public class SubjectiveScoreRubrics extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      final String awardGroup = WebUtils.getNonNullRequestParameter(request, "awardGroup");
      final String categoryName = WebUtils.getNonNullRequestParameter(request, "categoryName");

      final SubjectiveScoreCategory category = description.getSubjectiveCategoryByName(categoryName);
      if (null == category) {
        throw new FLLRuntimeException("Unable to find category with name '"
            + categoryName
            + "'");
      }

      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Collection<SubjectiveScore> scores = SubjectiveScore.getScoresForCategoryAndAwardGroup(connection,
                                                                                                   tournament, category,
                                                                                                   awardGroup);

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition",
                         String.format("filename=subjective-rubrics_%s_%s.pdf", categoryName, awardGroup));
      SubjectivePdfWriter.createDocumentForScores(connection, tournament, response.getOutputStream(), description,
                                                  category, scores);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new FLLRuntimeException(e);
    }

  }

}
