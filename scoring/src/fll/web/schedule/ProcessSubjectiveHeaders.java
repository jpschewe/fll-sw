/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.db.CategoryColumnMapping;
import fll.scheduler.SubjectiveStation;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * Processes results of chooseSubjectiveHeaders.jsp and redirects to
 * {@link CheckViolations}.
 */
@WebServlet("/schedule/ProcessSubjectiveHeaders")
public class ProcessSubjectiveHeaders extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);

    // J2EE doesn't have things typed yet
    @SuppressWarnings("unchecked")
    final List<String> unusedHeaders = SessionAttributes.getNonNullAttribute(session, CheckViolations.UNUSED_HEADERS,
                                                                             List.class);

    final Collection<CategoryColumnMapping> categoryColumns = new LinkedList<CategoryColumnMapping>();

    final Set<ScoreCategory> assignedCategories = new HashSet<ScoreCategory>();

    // get params for subjectiveHeader
    final List<SubjectiveStation> subjectiveStations = new LinkedList<SubjectiveStation>();
    for (int index = 0; index < unusedHeaders.size(); ++index) {
      final String header = unusedHeaders.get(index);

      final List<ScoreCategory> categories = new LinkedList<ScoreCategory>();
      for (final ScoreCategory cat : challenge.getSubjectiveCategories()) {
        final String value = request.getParameter(index
            + ":" + cat.getName());
        if (null != value) {
          categories.add(cat);

          if (!assignedCategories.add(cat)) {
            session.setAttribute(SessionAttributes.MESSAGE,
                                 String.format("<p class='error'>%s is assigned to 2 schedule columns. This is not allowed.</p>",
                                               cat.getTitle()));
            WebUtils.sendRedirect(application, response, "/schedule/chooseSubjectiveHeaders.jsp");
            return;
          }
        }
      }

      if (!categories.isEmpty()) {
        final String durationStr = request.getParameter(index
            + ":duration");

        subjectiveStations.add(new SubjectiveStation(header, Integer.parseInt(durationStr)));

        for (final ScoreCategory category : categories) {
          final CategoryColumnMapping mapping = new CategoryColumnMapping(category.getName(), header);
          categoryColumns.add(mapping);
        }
      }

    }

    session.setAttribute(UploadSchedule.MAPPINGS_KEY, categoryColumns);
    session.setAttribute(CheckViolations.SUBJECTIVE_STATIONS, subjectiveStations);

    WebUtils.sendRedirect(application, response, "/schedule/CheckViolations");
  }
}
