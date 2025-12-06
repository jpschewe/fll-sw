/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Team;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
import fll.scores.DatabasePerformanceTeamScore;
import fll.scores.PerformanceTeamScore;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
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

    final ChallengeDescription descritpion = ApplicationAttributes.getChallengeDescription(application);

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
      final Team team = Team.getTeamFromDatabase(connection, teamNumber);
      page.setAttribute("team", team);

      final List<PerformanceTeamScore> scores = DatabasePerformanceTeamScore.fetchTeamScores(tournamentData.getCurrentTournament(),
                                                                                             team, connection);

      final RunMetadataFactory metadataFactory = tournamentData.getRunMetadataFactory();

      final PerformanceScoreCategory performanceCategory = descritpion.getPerformance();

      final List<Data> data = scores.stream()
                                    .map(s -> new Data(s.getRunNumber(),
                                                       metadataFactory.getRunMetadata(s.getRunNumber())
                                                                      .getDisplayName(),
                                                       performanceCategory.evaluate(s), s.isNoShow(), s.isBye(),
                                                       s.getLastEdited()))
                                    .toList();

      page.setAttribute("data", data);

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

  /**
   * Data for the web page.
   * 
   * @param runNumber {@link PerformanceTeamScore#getRunNumber()}
   * @param runName {@link RunMetadata#getDisplayName()}
   * @param computedTotal the computed score
   * @param noShow {@link PerformanceTeamScore#isNoShow()}
   * @param bye {@link PerformanceTeamScore#isBye()}
   * @param lastEdited {@link PerformanceTeamScore#getLastEdited()}
   */
  public record Data(int runNumber,
                     String runName,
                     double computedTotal,
                     boolean noShow,
                     boolean bye,
                     LocalDateTime lastEdited) {
  }

}
