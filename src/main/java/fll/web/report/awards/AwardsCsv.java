/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.diffplug.common.base.Errors;
import com.opencsv.CSVWriter;

import fll.Tournament;
import fll.TournamentLevel;
import fll.TournamentTeam;
import fll.db.AwardWinners;
import fll.db.CategoriesIgnored;
import fll.db.OverallAwardWinner;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.playoff.Playoff;
import fll.web.report.PromptSummarizeScores;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Awards CSV file for shipping awards.
 */
@WebServlet("/report/AwardsCsv")
public class AwardsCsv extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    if (PromptSummarizeScores.checkIfSummaryUpdated(request, response, application, session, "/report/AwardsCsv")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      response.reset();
      response.setContentType("text/csv");
      response.setHeader("Content-Disposition", "filename=awards.csv");

      try (CSVWriter csv = new CSVWriter(response.getWriter())) {

        writeHeader(csv);
        writeData(connection, description, csv);
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeHeader(final CSVWriter csv) {
    csv.writeNext(new String[] { "Award", "Team Number", "Team Name" });
  }

  private void writeData(final Connection connection,
                         final ChallengeDescription description,
                         final CSVWriter csv)
      throws SQLException {

    final Tournament tournament = Tournament.getCurrentTournament(connection);

    final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, tournament.getTournamentID());

    addNonNumericAwardWinners(connection, description, csv, tournament, teams);

    addPerformance(connection, csv, description);

    if (TournamentParameters.getRunningHeadToHead(connection, tournament.getTournamentID())) {
      addHeadToHead(connection, tournament, csv, teams);
    }

    addSubjectiveChallengeWinners(connection, csv, tournament, teams);
    addSubjectiveOverallWinners(connection, description, csv, tournament, teams);
  }

  private void addHeadToHead(final Connection connection,
                             final Tournament tournament,
                             final CSVWriter csv,
                             final Map<Integer, TournamentTeam> teams)
      throws SQLException {

    final List<String> playoffDivisions = Playoff.getCompletedBrackets(connection, tournament.getTournamentID());
    for (final String bracketName : playoffDivisions) {
      final int playoffRound = Playoff.getMaxPlayoffRound(connection, tournament.getTournamentID(), bracketName);
      final int teamNumber = Playoff.getPlayoffTeamNumber(connection, tournament, bracketName, playoffRound, 1);
      final @Nullable TournamentTeam team = teams.get(teamNumber);
      csv.writeNext(new String[] { String.format("%s Head to Head", bracketName), String.valueOf(teamNumber),
                                   null == team ? "NULL" : team.getTeamName() });

    }
  }

  private void addSubjectiveChallengeWinners(final Connection connection,
                                             final CSVWriter csv,
                                             final Tournament tournament,
                                             final Map<Integer, TournamentTeam> teams)
      throws SQLException {
    AwardWinners.getSubjectiveAwardWinners(connection, tournament.getTournamentID()) //
                .forEach(winner -> {
                  final @Nullable TournamentTeam team = teams.get(winner.getTeamNumber());
                  csv.writeNext(new String[] { winner.getName(), String.valueOf(winner.getTeamNumber()),
                                               null == team ? "NULL" : team.getTeamName() });
                });
  }

  /**
   * @param connection database connection
   * @param description challenge description
   * @param level tournament level
   * @param winner winner to check
   * @return true if the winner should be given an award for this tournament level
   * @throws SQLException on a database error
   */
  public static boolean isNonNumericAwarded(final Connection connection,
                                            final ChallengeDescription description,
                                            final TournamentLevel level,
                                            final OverallAwardWinner winner)
      throws SQLException {
    final NonNumericCategory category = description.getNonNumericCategoryByTitle(winner.getName());
    if (null == category) {
      // assume the category is something that isn't in the description
      return true;
    } else {
      return !CategoriesIgnored.isNonNumericCategoryIgnored(connection, level, category);
    }
  }

  private void addNonNumericAwardWinners(final Connection connection,
                                         final ChallengeDescription description,
                                         final CSVWriter csv,
                                         final Tournament tournament,
                                         final Map<Integer, TournamentTeam> teams)
      throws SQLException {
    final TournamentLevel level = tournament.getLevel();

    AwardWinners.getNonNumericAwardWinners(connection, tournament.getTournamentID()).stream() //
                .filter(Errors.rethrow().wrapPredicate(w -> isNonNumericAwarded(connection, description, level, w))) //
                .forEach(winner -> {
                  final @Nullable TournamentTeam team = teams.get(winner.getTeamNumber());
                  csv.writeNext(new String[] { winner.getName(), String.valueOf(winner.getTeamNumber()),
                                               null == team ? "NULL" : team.getTeamName() });
                });
  }

  private void addSubjectiveOverallWinners(final Connection connection,
                                           final ChallengeDescription description,
                                           final CSVWriter csv,
                                           final Tournament tournament,
                                           final Map<Integer, TournamentTeam> teams)
      throws SQLException {

    AwardWinners.getNonNumericOverallAwardWinners(connection, tournament.getTournamentID()).stream() //
                .filter(Errors.rethrow()
                              .wrapPredicate(w -> isNonNumericAwarded(connection, description, tournament.getLevel(),
                                                                      w))) //
                .forEach(winner -> {
                  final @Nullable TournamentTeam team = teams.get(winner.getTeamNumber());
                  csv.writeNext(new String[] { winner.getName(), String.valueOf(winner.getTeamNumber()),
                                               null == team ? "NULL" : team.getTeamName() });
                });
  }

  private void addPerformance(final Connection connection,
                              final CSVWriter csv,
                              final ChallengeDescription description)
      throws SQLException {

    final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description);

    for (final Map.Entry<String, List<Top10.ScoreEntry>> entry : scores.entrySet()) {
      final String awardGroup = entry.getKey();
      final List<Top10.ScoreEntry> scoreList = entry.getValue();

      final Optional<Top10.ScoreEntry> firstWinner = scoreList.stream().findFirst();
      if (firstWinner.isPresent()) {
        final String topScore = firstWinner.get().getFormattedScore();

        final Collection<Top10.ScoreEntry> allWinners = scoreList.stream()
                                                                 .filter(e -> e.getFormattedScore().equals(topScore))
                                                                 .collect(Collectors.toList());

        for (Top10.ScoreEntry winner : allWinners) {
          csv.writeNext(new String[] { String.format("%s Performance", awardGroup),
                                       String.valueOf(winner.getTeamNumber()), winner.getTeamName() });
        }
      } // have a winner
    } // foreach group
  }

}
