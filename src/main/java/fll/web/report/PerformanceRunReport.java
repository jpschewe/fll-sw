/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Helper code for report/performanceRunReport.jsp.
 */
public final class PerformanceRunReport {

  private PerformanceRunReport() {
  }

  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final HttpServletRequest request,
                                     final PageContext page) {
    @MonotonicNonNull
    String workflowId = request.getParameter(SessionAttributes.WORKFLOW_ID);

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    try (Connection connection = tournamentData.getDataSource().getConnection()) {
      page.setAttribute("divisions", Queries.getAwardGroups(connection));

      final int runNumber;
      final @Nullable String runNumberParam = request.getParameter("RunNumber");
      if (StringUtils.isEmpty(runNumberParam)) {
        if (null != workflowId) {
          runNumber = SessionAttributes.getNonNullWorkflowAttribute(session, workflowId, "RunNumber", Integer.class)
                                       .intValue();
        } else {
          throw new MissingRequiredParameterException("RunNumber");
        }
      } else {
        runNumber = Integer.parseInt(runNumberParam);
      }
      page.setAttribute("RunNumber", runNumber);

      if (null == workflowId) {
        workflowId = SessionAttributes.createWorkflowSession(session);
      }

      // save for later redirects back from editing a score
      SessionAttributes.setWorkflowAttribute(session, workflowId, "RunNumber", runNumber);
      page.setAttribute(SessionAttributes.WORKFLOW_ID, workflowId);
    } catch (final SQLException e) {
      throw new FLLInternalException(e);
    }
  }

}
