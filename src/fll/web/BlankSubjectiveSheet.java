/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.itextpdf.text.DocumentException;

import fll.Tournament;
import fll.db.Queries;
import fll.documents.elements.SheetElement;
import fll.documents.writers.SubjectivePdfWriter;
import fll.scheduler.TeamScheduleInfo;
import fll.util.FLLRuntimeException;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * Generate blank subjective sheets for the specified subjective category.
 */
@WebServlet("/BlankSubjectiveSheet/*")
public class BlankSubjectiveSheet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final String pathInfo = request.getPathInfo();
      if (null != pathInfo
          && pathInfo.length() > 1) {
        final String subjectiveCategoryName = pathInfo.substring(1);

        final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", String.format("filename=subjective-%s.pdf", subjectiveCategoryName));

        final ScoreCategory category = challengeDescription.getSubjectiveCategoryByName(subjectiveCategoryName);

        final SheetElement sheetElement = new SheetElement(category);

        final TeamScheduleInfo dummy = new TeamScheduleInfo(0, 111111);
        dummy.setTeamName("Really long team name, something that is really really long");
        dummy.setOrganization("Some organization");
        dummy.setDivision("State");
        dummy.setJudgingGroup("Lakes");

        final Tournament tournament = Tournament.findTournamentByID(connection,
                                                                    Queries.getCurrentTournament(connection));

        SubjectivePdfWriter.createDocument(response.getOutputStream(), challengeDescription, tournament.getName(),
                                           sheetElement, null, Collections.singletonList(dummy));

      } else {
        throw new FLLRuntimeException("You must specify a subjective category name in the URL");
      }
    } catch (final SQLException | DocumentException e) {
      throw new FLLRuntimeException(e);
    }

  }

}
