/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.DelayedPerformance;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FP;
import fll.web.report.FinalComputedScores;
import fll.web.report.awards.AwardsScriptReport;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import fll.xml.WinnerType;

/**
 * Compute top scores from the seeding rounds.
 */
public final class Top10 {

  private Top10() {
  }

  /**
   * Max number of characters in a team name to display.
   */
  public static final int MAX_TEAM_NAME = 12;

  /**
   * Max number of characters in an organization to display.
   */
  public static final int MAX_ORG_NAME = 20;

  /**
   * Highest rank to display.
   */
  public static final int MAX_DISPLAY_RANK = 5;

  /**
   * Used for processing the result of a score query.
   */
  private interface ProcessScoreEntry {
    void execute(String teamName,
                 int teamNumber,
                 String organization,
                 String formattedScore,
                 int rank);
  }

  /**
   * @param connection database connection
   * @param description challenge description
   * @return awardGroup to sorted scores
   * @throws SQLException if there is a problem talking to the database
   */
  public static Map<String, List<ScoreEntry>> getTableAsMapByAwardGroup(final Connection connection,
                                                                        final ChallengeDescription description)
      throws SQLException {
    final Tournament tournament = Tournament.getCurrentTournament(connection);

    // use a LinkedHashMap so that the iteration order matches the sorted order of
    // the award groups
    final Map<String, List<ScoreEntry>> data = new LinkedHashMap<>();
    final List<String> awardGroups = AwardsScriptReport.getAwardGroupOrder(connection, tournament);
    for (final String ag : awardGroups) {
      final List<ScoreEntry> scores = new LinkedList<>();
      processScoresForAwardGroup(connection, description, ag, (teamName,
                                                               teamNumber,
                                                               organization,
                                                               formattedScore,
                                                               rank) -> {
        final ScoreEntry row = new ScoreEntry(teamName, teamNumber, organization, formattedScore, rank);
        scores.add(row);
      });
      data.put(ag, scores);
    }
    return data;
  }

  /**
   * @param connection database connection
   * @param description challenge description
   * @param tournament the tournament to get scores for
   * @return judging station to sorted scores
   * @throws SQLException if there is a problem talking to the database
   */
  public static Map<String, List<ScoreEntry>> getTableAsMapByJudgingStation(final Connection connection,
                                                                            final ChallengeDescription description,
                                                                            final Tournament tournament)
      throws SQLException {
    final Map<String, List<ScoreEntry>> data = new HashMap<>();
    final List<String> judgingStations = Queries.getJudgingStations(connection, tournament.getTournamentID());
    for (final String ag : judgingStations) {
      final List<ScoreEntry> scores = new LinkedList<>();
      processScoresForJudgingStation(connection, description, ag, (teamName,
                                                                   teamNumber,
                                                                   organization,
                                                                   formattedScore,
                                                                   rank) -> {
        final ScoreEntry row = new ScoreEntry(teamName, teamNumber, organization, formattedScore, rank);
        scores.add(row);
      });
      data.put(ag, scores);
    }
    return data;
  }

  /**
   * Data class for scores within an award group.
   */
  public static class ScoreEntry {
    /**
     * @param teamName see {@link #getTeamName()}
     * @param teamNumber see {@link #getTeamNumber()}
     * @param organization see {@link #getOrganization()}
     * @param formattedScore see {@link #getFormattedScore()}
     * @param rank see {@link #getRank()}
     */
    public ScoreEntry(final String teamName,
                      final int teamNumber,
                      final String organization,
                      final String formattedScore,
                      final int rank) {
      this.teamName = teamName;
      this.teamNumber = teamNumber;
      this.organization = organization;
      this.formattedScore = formattedScore;
      this.rank = rank;
    }

    private final String teamName;

    /**
     * @return team name
     */
    public String getTeamName() {
      return teamName;
    }

    private final int teamNumber;

    /**
     * @return team number
     */
    public int getTeamNumber() {
      return teamNumber;
    }

    private final String formattedScore;

    /**
     * @return score formatted for display
     */
    public String getFormattedScore() {
      return formattedScore;
    }

    private final String organization;

    /**
     * @return team organization
     */
    public String getOrganization() {
      return organization;
    }

    private final int rank;

    /**
     * @return rank within their award group
     */
    public int getRank() {
      return rank;
    }

  }

  private static void processScoresForAwardGroup(final Connection connection,
                                                 final ChallengeDescription challengeDescription,
                                                 final String awardGroupName,
                                                 final ProcessScoreEntry processor)
      throws SQLException {
    processScores(connection, challengeDescription, "event_division", awardGroupName, processor);
  }

  private static void processScoresForJudgingStation(final Connection connection,
                                                     final ChallengeDescription challengeDescription,
                                                     final String judgingStation,
                                                     final ProcessScoreEntry processor)
      throws SQLException {
    processScores(connection, challengeDescription, "judging_station", judgingStation, processor);
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Determine sort order based upon winner criteria")
  private static void processScores(final Connection connection,
                                    final ChallengeDescription challengeDescription,
                                    final String divisionColumn, // event_division or judging_station
                                    final String awardGroupName,
                                    final ProcessScoreEntry processor)
      throws SQLException {
    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();
    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final Tournament currentTournament = Tournament.getCurrentTournament(connection);
    final int currentTournamentId = currentTournament.getTournamentID();
    final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, currentTournamentId);
    final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection, currentTournamentId);
    final int maxRunNumberToDisplay = DelayedPerformance.getMaxRunNumberToDisplay(connection, currentTournament);

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT Teams.TeamName, Teams.Organization, Teams.TeamNumber, T2.MaxOfComputedScore" //
            + " FROM (SELECT TeamNumber, " //
            + winnerCriteria.getMinMaxString()
            + "(ComputedTotal) AS MaxOfComputedScore" //
            + "  FROM verified_performance WHERE Tournament = ? "
            + "   AND NoShow = False" //
            + "   AND Bye = False" //
            + "   AND (? OR RunNumber <= ?)" //
            + "   AND RunNumber <= ?" //
            + "  GROUP BY TeamNumber) AS T2"
            + " JOIN Teams ON Teams.TeamNumber = T2.TeamNumber, TournamentTeams"
            + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber" //
            + " AND TournamentTeams."
            + divisionColumn
            + " = ?" //
            + " AND TournamentTeams.tournament = ?" //
            + " ORDER BY T2.MaxOfComputedScore "
            + winnerCriteria.getSortString())) {
      prep.setInt(1, currentTournamentId);
      prep.setBoolean(2, !runningHeadToHead);
      prep.setInt(3, numSeedingRounds);
      prep.setInt(4, maxRunNumberToDisplay);
      prep.setString(5, awardGroupName);
      prep.setInt(6, currentTournamentId);
      try (ResultSet rs = prep.executeQuery()) {

        double prevScore = -1;
        int i = 1;
        int rank = 0;
        while (rs.next()) {
          final double score = rs.getDouble("MaxOfComputedScore");
          if (!FP.equals(score, prevScore, FinalComputedScores.TIE_TOLERANCE)) {
            rank = i;
          }

          final int teamNumber = rs.getInt("TeamNumber");
          String teamName = rs.getString("TeamName");
          if (null == teamName) {
            teamName = "";
          }

          String organization = rs.getString("Organization");
          if (null == organization) {
            organization = "";
          }

          final String formattedScore = Utilities.getFormatForScoreType(performanceScoreType).format(score);

          processor.execute(teamName, teamNumber, organization, formattedScore, rank);
          prevScore = score;
          ++i;
        } // end while next
      } // try ResultSet
    } // try PreparedStatement

  }

}
