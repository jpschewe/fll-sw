/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreboard;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.DelayedPerformance;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.flltools.displaySystem.list.SetArray;
import fll.util.FP;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.report.FinalComputedScores;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import fll.xml.WinnerType;

/**
 * Compute top scores from the seeding rounds.
 */
@WebServlet("/scoreboard/Top10")
public class Top10 extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Max number of characters in a team name to display.
   */
  public static final int MAX_TEAM_NAME = 12;

  /**
   * Max number of characters in an organization to display.
   */
  public static final int MAX_ORG_NAME = 20;

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Entering doPost");
    }

    final Formatter formatter = new Formatter(response.getWriter());
    final String showOrgStr = request.getParameter("showOrganization");
    final boolean showOrg = null == showOrgStr ? true : Boolean.parseBoolean(showOrgStr);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final Integer divisionIndexObj = SessionAttributes.getAttribute(session, "divisionIndex", Integer.class);
      int awardGroupIndex;
      if (null == divisionIndexObj) {
        awardGroupIndex = 0;
      } else {
        awardGroupIndex = divisionIndexObj.intValue();
      }
      ++awardGroupIndex;

      final List<String> allAwardGroups = Queries.getAwardGroups(connection);
      if (awardGroupIndex >= allAwardGroups.size()) {
        awardGroupIndex = 0;
      }
      session.setAttribute("divisionIndex", Integer.valueOf(awardGroupIndex));

      formatter.format("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">%n");
      formatter.format("<html>%n");
      formatter.format("<head>%n");
      formatter.format("<link rel='stylesheet' type='text/css' href='../style/fll-sw.css' />%n");
      formatter.format("<link rel='stylesheet' type='text/css' href='score_style.css' />%n");
      formatter.format("<meta http-equiv='refresh' content='%d' />%n",
                       GlobalParameters.getIntGlobalParameter(connection, GlobalParameters.DIVISION_FLIP_RATE));
      formatter.format("</head>%n");

      formatter.format("<body class='scoreboard'>%n");

      formatter.format("<table border='1' cellpadding='2' cellspacing='0' width='98%%'>%n");

      formatter.format("<colgroup>%n");
      formatter.format("<col width='30px' />%n");
      formatter.format("<col width='75px' />%n");
      formatter.format("<col />%n");
      if (showOrg) {
        formatter.format("<col />%n");
      }
      formatter.format("<col width='70px' />%n");
      formatter.format("</colgroup>%n");

      if (!allAwardGroups.isEmpty()) {
        final String awardGroupName = allAwardGroups.get(awardGroupIndex);

        formatter.format("<tr>%n");
        int numColumns = 5;
        if (!showOrg) {
          --numColumns;
        }
        formatter.format("<th colspan='%d' bgcolor='%s'>Top Performance Scores: %s</th>", numColumns,
                         Queries.getColorForIndex(allAwardGroups.indexOf(awardGroupName)), awardGroupName);
        formatter.format("</tr>%n");

        processScoresForAwardGroup(connection, description, awardGroupName, (teamName,
                                                                             teamNumber,
                                                                             organization,
                                                                             formattedScore,
                                                                             rank) -> {
          formatter.format("<tr>%n");
          formatter.format("<td class='center'>%d</td>%n", rank);
          formatter.format("<td class='right'>%d</td>%n", teamNumber);
          if (null == teamName) {
            teamName = "&nbsp;";
          }
          formatter.format("<td class='left truncate'>%s</td>%n", teamName);

          if (showOrg) {
            if (null == organization) {
              organization = "&nbsp;";
            }
            formatter.format("<td class='left truncate'>%s</td>%n", organization);
          }
          formatter.format("<td class='right'>%s</td>%n", formattedScore);
          formatter.format("</tr>");
        });

      } // end divisions not empty

    } catch (final SQLException e) {
      throw new RuntimeException("Error talking to the database", e);
    }

    formatter.format("</table>%n");
    formatter.format("</body>%n");
    formatter.format("</html>%n");

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Exiting doPost");
    }
  }

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
    final Map<String, List<ScoreEntry>> data = new HashMap<>();
    final List<String> awardGroups = Queries.getAwardGroups(connection);
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

  /**
   * Get the displayed data as a list for flltools.
   *
   * @param awardGroupName the award group to get scores for
   * @param connection database connection
   * @param description challenge description
   * @return payload for the set array message
   * @throws SQLException on a database error
   */
  public static SetArray.Payload getTableAsListForAwardGroup(final Connection connection,
                                                             final ChallengeDescription description,
                                                             final String awardGroupName)
      throws SQLException {
    final List<List<String>> data = new LinkedList<>();
    processScoresForAwardGroup(connection, description, awardGroupName, (teamName,
                                                                         teamNumber,
                                                                         organization,
                                                                         formattedScore,
                                                                         rank) -> {
      final List<String> row = new LinkedList<>();

      row.add(String.valueOf(rank));
      row.add(String.valueOf(teamNumber));
      if (null == teamName) {
        row.add("");
      } else {
        row.add(teamName);
      }

      if (null == organization) {
        row.add("");
      } else {
        row.add(organization);
      }
      row.add(formattedScore);
      data.add(row);
    });

    final SetArray.Payload payload = new SetArray.Payload("Top Performance Scores: "
        + awardGroupName, data);
    return payload;

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
