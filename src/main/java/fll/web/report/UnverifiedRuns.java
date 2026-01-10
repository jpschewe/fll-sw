/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import fll.Team;
import fll.Tournament;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
import fll.scores.DatabasePerformanceTeamScore;
import fll.scores.PerformanceTeamScore;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.TournamentData;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for report/unverifiedRuns.jsp.
 */
public final class UnverifiedRuns {

  private UnverifiedRuns() {
  }

  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final HttpServletRequest request,
                                     final PageContext page) {
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final Tournament tournament = tournamentData.getCurrentTournament();
    try (Connection connection = tournamentData.getDataSource().getConnection()) {
      final RunMetadataFactory metadataFactory = tournamentData.getRunMetadataFactory();

      final List<PerformanceTeamScore> scores = DatabasePerformanceTeamScore.fetchUnverifiedScores(tournament,
                                                                                                   connection);
      final List<Data> data = scores.stream() //
                                    .map(s -> new Data(s.getRunNumber(),
                                                       metadataFactory.getRunMetadata(s.getRunNumber())
                                                                      .getDisplayName(),
                                                       s.getTable(), s.getTeamNumber(),
                                                       getTeam(connection, s.getTeamNumber()).getTeamName())) //
                                    .toList();

      page.setAttribute("data", data);
    } catch (final SQLException e) {
      throw new FLLInternalException(e);
    }
  }

  private static Team getTeam(final Connection connection,
                              final int teamNumber) {
    try {
      return Team.getTeamFromDatabase(connection, teamNumber);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

  /**
   * @param runNumber {@link PerformanceTeamScore#getRunNumber()}
   * @param runName {@link RunMetadata#getDisplayName()}
   * @param tablename the table the run was on
   * @param teamNumber {@link Team#getTeamNumber()}
   * @param teamName {@link Team#getTeamName()}
   */
  public record Data(int runNumber,
                     String runName,
                     String tablename,
                     int teamNumber,
                     String teamName) {
  }
}
