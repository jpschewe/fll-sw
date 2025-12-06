/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

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
 * Helper code for report/teamPerformanceReport.jsp.
 */
public final class TeamPerformanceReport {

  private TeamPerformanceReport() {
  }

  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final HttpServletRequest request,
                                     final PageContext page) {
    @Nullable
    String workflowId = request.getParameter(SessionAttributes.WORKFLOW_ID);

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    page.setAttribute("tournament", tournamentData.getCurrentTournament().getTournamentID());

    try (Connection connection = tournamentData.getDataSource().getConnection()) {

      final int teamNumber;
      final @Nullable String teamNumberParam = request.getParameter("TeamNumber");
      if (StringUtils.isEmpty(teamNumberParam)) {
        if (null != workflowId) {
          teamNumber = SessionAttributes.getNonNullWorkflowAttribute(session, workflowId, "TeamNumber", Integer.class)
                                        .intValue();
        } else {
          throw new MissingRequiredParameterException("TeamNumber");
        }
      } else {
        teamNumber = Integer.parseInt(teamNumberParam);
      }
      page.setAttribute("TeamNumber", teamNumber);

      if (null == workflowId) {
        workflowId = SessionAttributes.createWorkflowSession(session);
      }

      // save for later redirects back from editing a score
      SessionAttributes.setWorkflowAttribute(session, workflowId, "TeamNumber", teamNumber);
      page.setAttribute(SessionAttributes.WORKFLOW_ID, workflowId);
    } catch (final SQLException e) {
      throw new FLLInternalException(e);
    }
  }

}
