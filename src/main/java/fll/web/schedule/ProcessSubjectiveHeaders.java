/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.util.Collection;
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
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

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
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);
    try {
      final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);

      final List<String> unusedHeaders = uploadScheduleData.getUnusedHeaders();

      final Collection<CategoryColumnMapping> categoryColumns = new LinkedList<>();

      // get params for subjectiveHeader
      final List<SubjectiveStation> subjectiveStations = new LinkedList<>();

      for (final SubjectiveScoreCategory cat : challenge.getSubjectiveCategories()) {
        final String value = request.getParameter(cat.getName()
            + ":header");
        if (null != value) {
          final int headerIndex = Integer.parseInt(value);
          final String header = unusedHeaders.get(headerIndex);

          final String durationStr = request.getParameter(cat.getName()
              + ":duration");

          final int duration = Integer.parseInt(durationStr);
          subjectiveStations.add(new SubjectiveStation(header, duration));

          final CategoryColumnMapping mapping = new CategoryColumnMapping(cat.getName(), header);
          categoryColumns.add(mapping);
        }

      }

      uploadScheduleData.setCategoryColumnMappings(categoryColumns);
      uploadScheduleData.setSubjectiveStations(subjectiveStations);

      WebUtils.sendRedirect(application, response, "/schedule/LoadSchedule");
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }
}
