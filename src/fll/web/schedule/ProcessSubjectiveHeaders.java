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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.db.CategoryColumnMapping;
import fll.scheduler.SubjectiveStation;
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
                                final HttpSession session)
      throws IOException, ServletException {

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);
    try {
      final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);

      final List<String> unusedHeaders = uploadScheduleData.getUnusedHeaders();

      final Collection<CategoryColumnMapping> categoryColumns = new LinkedList<CategoryColumnMapping>();

      // get params for subjectiveHeader
      final List<SubjectiveStation> subjectiveStations = new LinkedList<SubjectiveStation>();

      for (final ScoreCategory cat : challenge.getSubjectiveCategories()) {
        final String value = request.getParameter(cat.getName()
            + ":header");
        if (null != value) {
          final int headerIndex = Integer.parseInt(value);
          final String header = unusedHeaders.get(headerIndex);

          final String durationStr = request.getParameter(cat.getName()
              + ":duration");

          // TODO issue:129 validate that duration is not empty and a number

          final int duration = Integer.parseInt(durationStr);
          subjectiveStations.add(new SubjectiveStation(header, duration));

          final CategoryColumnMapping mapping = new CategoryColumnMapping(cat.getName(), header);
          categoryColumns.add(mapping);
        }

      }

      uploadScheduleData.setCategoryColumnMappings(categoryColumns);
      uploadScheduleData.setSubjectiveStations(subjectiveStations);

      WebUtils.sendRedirect(application, response, "/schedule/CheckViolations");
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }
}
