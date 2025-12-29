/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
import fll.Tournament;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.api.AwardsReportSortedGroupsServlet;
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
      final Tournament tournament = tournamentData.getCurrentTournament();

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
      page.setAttribute("RunName", tournamentData.getRunMetadataFactory().getRunMetadata(runNumber).getDisplayName());

      if (null == workflowId) {
        workflowId = SessionAttributes.createWorkflowSession(session);
      }

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT Teams.TeamNumber,Teams.TeamName,Teams.Organization,Performance.ComputedTotal,Performance.NoShow,Performance.TIMESTAMP" //
              + " FROM Teams,Performance,TournamentTeams" //
              + " WHERE Performance.RunNumber = ?" //
              + " AND Teams.TeamNumber = Performance.TeamNumber" //
              + " AND TournamentTeams.TeamNumber = Teams.TeamNumber" //
              + " AND Performance.Tournament = ?" //
              + " AND TournamentTeams.event_division  = ?" //
              + " AND TournamentTeams.Tournament = Performance.Tournament" //
              + " ORDER BY ComputedTotal DESC" //
          )) {
        prep.setInt(1, runNumber);
        prep.setInt(2, tournament.getTournamentID());

        // award group (sorted order) -> data sorted by team number
        final Map<String, List<Data>> results = new LinkedHashMap<>();
        for (final String awardGroup : AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                            tournament.getTournamentID())) {
          prep.setString(3, awardGroup);

          try (ResultSet rs = prep.executeQuery()) {
            final List<Data> scores = new LinkedList<>();
            while (rs.next()) {
              final int teamNumber = rs.getInt(1);
              final String teamName = castNonNull(rs.getString(2));
              final @Nullable String organization = rs.getString(3);
              final double computedTotal = rs.getDouble(4);
              final boolean noShow = rs.getBoolean(5);
              final Timestamp ts = (Timestamp) castNonNull(rs.getTimestamp(6));
              final LocalDateTime timestamp = ts.toLocalDateTime();
              final Data data = new Data(teamNumber, teamName, organization, noShow, computedTotal, timestamp);
              scores.add(data);
            }
            if (!scores.isEmpty()) {
              results.put(awardGroup, scores);
            }
          }
        }
        page.setAttribute("results", results);
      }

      // save for later redirects back from editing a score
      SessionAttributes.setWorkflowAttribute(session, workflowId, "RunNumber", runNumber);
      page.setAttribute(SessionAttributes.WORKFLOW_ID, workflowId);
    } catch (final SQLException e) {
      throw new FLLInternalException(e);
    }
  }

  /**
   * Data for the web page.
   * 
   * @param teamNumber {@link Team#getTeamNumber()}
   * @param teamName {@link
   */
  public record Data(int teamNumber,
                     String teamName,
                     @Nullable String organization,
                     boolean noShow,
                     double computedTotal,
                     LocalDateTime lastEdited) {

  }

}
