/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.diffplug.common.base.Errors;

import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Commit team information changes.
 */
@WebServlet("/schedule/CommitTeamInformationChanges")
public class CommitTeamInformationChanges extends BaseFLLServlet {

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
    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    try (Connection connection = datasource.getConnection()) {
      uploadScheduleData.getNameDifferences().forEach(Errors.rethrow().wrap(nameDifference -> {
        if (null != request.getParameter("name_"
            + nameDifference.getNumber())) {
          Queries.updateTeamName(connection, nameDifference.getNumber(), nameDifference.getNewName());
        }
      }));

      uploadScheduleData.getOrganizationDifferences().forEach(Errors.rethrow().wrap(organizationDifference -> {
        if (null != request.getParameter("organization_"
            + organizationDifference.getNumber())) {
          Queries.updateTeamOrganization(connection, organizationDifference.getNumber(),
                                         organizationDifference.getNewOrganization());
        }
      }));

    } catch (final SQLException e) {
      throw new FLLInternalException("Error talking to the database", e);
    }

    WebUtils.sendRedirect(application, response, "/schedule/CommitSchedule");

  }

}
