/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.CategoryColumnMapping;
import fll.db.Queries;
import fll.documents.writers.SubjectivePdfWriter;
import fll.scheduler.TournamentSchedule;
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

/**
 * Output the subjective sheets for a particular category. The category name is
 * specified in the url.
 */
@WebServlet("/admin/SubjectiveSheets/*")
public class SubjectiveSheets extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    final String pathInfo = request.getPathInfo();
    if (null != pathInfo
        && pathInfo.length() > 1) {
      final String subjectiveCategoryName = pathInfo.substring(1);

      try (Connection connection = datasource.getConnection()) {
        final int currentTournamentID = Queries.getCurrentTournament(connection);

        if (!TournamentSchedule.scheduleExistsInDatabase(connection, currentTournamentID)) {
          SessionAttributes.appendToMessage(session, "<p class='error'>There is no schedule for this tournament.</p>");
          WebUtils.sendRedirect(application, response, "/admin/index.jsp");
          return;
        }

        final TournamentSchedule schedule = new TournamentSchedule(connection, currentTournamentID);

        final Tournament tournament = Tournament.findTournamentByID(connection, currentTournamentID);

        final SubjectiveScoreCategory category = description.getSubjectiveCategoryByName(subjectiveCategoryName);
        if (null == category) {
          throw new FLLRuntimeException("A subjective category with name '"
              + subjectiveCategoryName
              + "' does not exist");
        }

        final Collection<CategoryColumnMapping> mappings = CategoryColumnMapping.load(connection, currentTournamentID);

        final Optional<CategoryColumnMapping> categoryMapping = mappings.stream()
                                                                        .filter(m -> m.getCategoryName()
                                                                                      .equals(category.getName()))
                                                                        .findFirst();
        if (!categoryMapping.isPresent()) {
          throw new FLLInternalException("Cannot find schedule column information for subjective category '"
              + category.getName()
              + "'");
        } else {
          response.reset();
          response.setContentType("application/pdf");
          response.setHeader("Content-Disposition",
                             String.format("filename=\"subjective-%s.pdf\"", subjectiveCategoryName));

          SubjectivePdfWriter.createDocumentForSchedule(response.getOutputStream(), description, tournament.getName(),
                                                        category, categoryMapping.get().getScheduleColumn(),
                                                        schedule.getSchedule());
        }
      } catch (final SQLException sqle) {
        LOGGER.error(sqle.getMessage(), sqle);
        throw new RuntimeException(sqle);
      }
    } else

    {
      throw new FLLRuntimeException("You must specify a subjective category name in the URL");
    }
  }

}
