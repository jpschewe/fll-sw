/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.opencsv.CSVWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.ScoreStandardization;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.AdvancingTeam;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FP;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * CSV file containing information about the teams advancing to another
 * tournament.
 */
@WebServlet("/report/TournamentAdvancement")
public class TournamentAdvancement extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      response.reset();
      response.setContentType("text/csv");
      response.setHeader("Content-Disposition", "filename=performance_scores.csv");

      try (CSVWriter csv = new CSVWriter(response.getWriter())) {

        writeHeader(csv, description);

        for (Tournament tournament : Tournament.getTournaments(connection)) {
          writeDataForTournament(connection, description, tournament, csv);
        }

      } // allocate csv

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error getting state advancement information from the database", e);
    }

  }

  private void writeDataForTournament(final Connection connection,
                                      final ChallengeDescription description,
                                      final Tournament tournament,
                                      final CSVWriter csv)
      throws SQLException {
    final PerformanceScoreCategory performanceCategory = description.getPerformance();

    final List<AdvancingTeam> advancing = AdvancingTeam.loadAdvancingTeams(connection, tournament.getTournamentID());
    if (advancing.isEmpty()) {
      // nothing to do
      return;
    }

    if (!summarizeScoresForTournament(connection, description, tournament)) {
      return;
    }

    // gather ranks and scores
    final List<String> awardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());

    // award group -> team -> {rank, score}
    final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> performanceRanks = new HashMap<>();
    // award group -> category -> Judging Group -> team number -> {rank, score}
    final Map<String, Map<ScoreCategory, Map<String, Map<Integer, ImmutablePair<Integer, Double>>>>> subjectiveRanks = new HashMap<>();

    final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> overallRanks = gatherOverallRanks(connection,
                                                                                                      description,
                                                                                                      tournament,
                                                                                                      awardGroups);

    for (final String awardGroup : awardGroups) {
      performanceRanks.put(awardGroup,
                           FinalComputedScores.gatherRankedPerformanceTeams(connection, description.getWinner(),
                                                                            tournament, awardGroup));

      subjectiveRanks.put(awardGroup,
                          FinalComputedScores.gatherRankedSubjectiveTeams(connection,
                                                                          description.getSubjectiveCategories(),
                                                                          description.getWinner(), tournament,
                                                                          awardGroup));
    } // foreach award group

    final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection,
                                                                                    tournament.getTournamentID());

    for (final AdvancingTeam adv : advancing) {
      final Integer teamNumber = adv.getTeamNumber();
      if (!tournamentTeams.containsKey(teamNumber)) {
        throw new FLLInternalException("Team "
            + teamNumber
            + " is listed as advancing from tournament "
            + tournament.getDescription()
            + ", however that team is not in the list of tournament teams");
      }

      final TournamentTeam team = tournamentTeams.get(teamNumber);

      final List<@Nullable String> csvData = new LinkedList<>();
      csvData.add(String.valueOf(teamNumber));
      csvData.add(team.getTeamName());
      csvData.add(tournament.getDescription());
      csvData.add(tournament.getLevel());
      csvData.add(tournament.getNextLevel());
      csvData.add(team.getAwardGroup());
      csvData.add(team.getJudgingGroup());

      final Map<ScoreCategory, Map<String, Map<Integer, ImmutablePair<Integer, Double>>>> sranks = subjectiveRanks.getOrDefault(team.getAwardGroup(),
                                                                                                                                Collections.emptyMap());

      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> cranks = sranks.getOrDefault(category,
                                                                                                     Collections.emptyMap());
        final Map<Integer, ImmutablePair<Integer, Double>> jranks = cranks.getOrDefault(team.getJudgingGroup(),
                                                                                        Collections.emptyMap());

        if (jranks.containsKey(teamNumber)) {
          final ImmutablePair<Integer, Double> pair = jranks.get(teamNumber);

          final String formattedScore = Utilities.getFormatForScoreType(category.getScoreType())
                                                 .format(pair.getRight());
          csvData.add(formattedScore);
          csvData.add(String.valueOf(pair.getLeft()));
        } else {
          csvData.add(""); // score
          csvData.add(""); // rank
        }
      }

      final Map<Integer, ImmutablePair<Integer, Double>> pAwardRanks = performanceRanks.getOrDefault(team.getAwardGroup(),
                                                                                                     Collections.emptyMap());
      if (pAwardRanks.containsKey(teamNumber)) {
        final ImmutablePair<Integer, Double> pData = pAwardRanks.get(teamNumber);
        final String formattedScore = Utilities.getFormatForScoreType(performanceCategory.getScoreType())
                                               .format(pData.getRight());
        csvData.add(formattedScore);
        csvData.add(String.valueOf(pData.getLeft()));
      } else {
        csvData.add(""); // performance score
        csvData.add(""); // performance rank
      }

      final Map<Integer, ImmutablePair<Integer, Double>> overallAwardRanks = overallRanks.getOrDefault(team.getAwardGroup(),
                                                                                                       Collections.emptyMap());
      if (overallAwardRanks.containsKey(teamNumber)) {
        final ImmutablePair<Integer, Double> pair = overallAwardRanks.get(teamNumber);
        final String formattedScore = Utilities.getFloatingPointNumberFormat().format(pair.getRight());
        csvData.add(formattedScore);
        csvData.add(String.valueOf(pair.getLeft()));
      }

      csv.writeNext(csvData.toArray(String[]::new));
    } // foreach advancing team

  }

  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", justification = "Sort string cannot be added with a parameter")
  private Map<String, Map<Integer, ImmutablePair<Integer, Double>>> gatherOverallRanks(final Connection connection,
                                                                                       final ChallengeDescription description,
                                                                                       final Tournament tournament,
                                                                                       final Collection<String> awardGroups)
      throws SQLException {
    // award group -> team number -> {rank, score}
    final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> result = new HashMap<>();

    try (PreparedStatement overallPrep = connection.prepareStatement("SELECT team_number, overall_score" //
        + " FROM overall_scores, TournamentTeams" //
        + " WHERE overall_scores.tournament = ?"//
        + " AND overall_scores.tournament = TournamentTeams.tournament" //
        + " AND overall_scores.team_number = TournamentTeams.TeamNumber" //
        + " AND TournamentTeams.event_division = ?"//
        + " ORDER BY overall_scores.overall_score "
        + description.getWinner().getSortString() //
    )) {
      overallPrep.setInt(1, tournament.getTournamentID());

      for (final String awardGroup : awardGroups) {
        overallPrep.setString(2, awardGroup);

        final Map<Integer, ImmutablePair<Integer, Double>> awardGroupResult = result.computeIfAbsent(awardGroup,
                                                                                                     k -> new HashMap<>());

        try (ResultSet overallResult = overallPrep.executeQuery()) {
          int numTied = 1;
          int rank = 0;
          double prevScore = Double.NaN;
          while (overallResult.next()) {
            final int teamNumber = overallResult.getInt(1);

            final double overallScore;
            final double ts = overallResult.getDouble(2);
            if (overallResult.wasNull()) {
              overallScore = Double.NaN;
            } else {
              overallScore = ts;
            }

            if (!FP.equals(overallScore, prevScore, FinalComputedScores.TIE_TOLERANCE)) {
              rank += numTied;
              numTied = 1;
            } else {
              ++numTied;
            }

            awardGroupResult.put(teamNumber, ImmutablePair.of(rank, overallScore));

            prevScore = overallScore;
          } // foreach score result
        } // ResultSet
      } // foreach award group
    } // PreparedStatement

    return result;
  }

  /**
   * @return true on success, false on failure to summarize scores. This method
   *         will log when returning false.
   */
  private boolean summarizeScoresForTournament(final Connection connection,
                                               final ChallengeDescription description,
                                               final Tournament tournament)
      throws SQLException {
    LOGGER.debug("Summarizing scores for {}", tournament.getDescription());

    Queries.updateScoreTotals(description, connection, tournament.getTournamentID());

    ScoreStandardization.standardizeSubjectiveScores(connection, tournament.getTournamentID());

    ScoreStandardization.summarizeScores(connection, tournament.getTournamentID());

    ScoreStandardization.updateTeamTotalScores(connection, description, tournament.getTournamentID());

    return true;
  }

  private void writeHeader(final CSVWriter csv,
                           final ChallengeDescription description) {
    final List<String> headers = new LinkedList<>();
    headers.add("Team #");
    headers.add("Team Name");
    headers.add("Touranment");
    headers.add("Tournament Level");
    headers.add("Next Tournament Level");
    headers.add("Award Group");
    headers.add("Judging Group");
    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      headers.add(String.format("Top %s score", category.getTitle()));
      headers.add(String.format("%s Rank", category.getTitle()));
    }

    headers.add("Top Performance Score");
    headers.add("Performance Rank");

    headers.add("Overall Score");
    headers.add("Overall Rank");

    csv.writeNext(headers.toArray(String[]::new));
  }

}
