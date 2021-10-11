/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.diffplug.common.base.Errors;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Tournament;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.DummyTeamScore;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FP;
import fll.xml.AbstractGoal;
import fll.xml.BracketSortType;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.EnumeratedValue;
import fll.xml.Goal;
import fll.xml.PerformanceScoreCategory;
import fll.xml.TiebreakerTest;
import fll.xml.WinnerType;

/**
 * Handle playoff information.
 */
public final class Playoff {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Tolerance for comparing floating point numbers in the tiebreaker.
   */
  private static final double TIEBREAKER_TOLERANCE = 1E-4;

  private Playoff() {
    // no instances
  }

  /**
   * Build the list of teams ordered from top to bottom (visually) of a single
   * elimination bracket.
   *
   * @param connection connection to the database
   * @param teams the teams in the playoffs
   * @return a List of teams
   * @param bracketSort how to sort the initial seeding of the teams
   * @param winnerCriteria what constitutes a winning score
   * @throws SQLException on a database error
   */
  public static List<Team> buildInitialBracketOrder(final Connection connection,
                                                    final BracketSortType bracketSort,
                                                    final WinnerType winnerCriteria,
                                                    final List<? extends Team> teams)
      throws SQLException {
    if (null == connection) {
      throw new NullPointerException("Connection cannot be null");
    }

    final List<? extends Team> seedingOrder;
    if (BracketSortType.ALPHA_TEAM == bracketSort) {
      seedingOrder = teams;

      // sort by team name
      Collections.sort(seedingOrder, Team.TEAM_NAME_COMPARATOR);
    } else if (BracketSortType.RANDOM == bracketSort) {
      seedingOrder = teams;
      Collections.shuffle(seedingOrder);
    } else {
      // standard seeding
      final int tournament = Queries.getCurrentTournament(connection);
      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament);
      if (numSeedingRounds < 1) {
        throw new FLLRuntimeException("Cannot initialize playoff brackets using scores from regular match play when the number of regular match play rounds is less than 1");
      }

      seedingOrder = Queries.getPlayoffSeedingOrder(connection, winnerCriteria, teams);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("seedingOrder: "
          + seedingOrder);
    }

    final int[] seeding = computeInitialBrackets(seedingOrder.size());

    // give byes to the last byesNeeded teams.
    final List<Team> list = new LinkedList<>();
    for (final int element : seeding) {
      if (element > seedingOrder.size()) {
        list.add(Team.BYE);
      } else {
        final Team team = seedingOrder.get(element
            - 1);
        list.add(team);
      }
    }

    if (list.size() != seeding.length) {
      throw new InternalError("Programming error, list size should be the same as seeding length");
    }
    return list;

  }

  /**
   * Decide who is the winner between teamA's score for the provided runNumber
   * and the score data contained in the request object. Calls
   * Queries.updateScoreTotals() to ensure the ComputedScore column is up to
   * date.
   *
   * @param connection Database connection with write access to Performance
   *          table.
   * @param performanceElement the XML element representing the performance
   *          scoring
   * @param tiebreakerElement the XML element representing the tiebreaker
   * @param winnerCriteria the criteria for picking a winner
   * @param teamA First team to check.
   * @param teamB Second team to check
   * @param teamBScore score for teamB
   * @param runNumber The run number to use for teamA's score.
   * @return The team that is the winner. Team.TIE is returned in the case of a
   *         tie and null when the score for teamA has not yet been entered.
   * @see Team#TIE
   * @see Team#NULL
   * @param tournament the tournament to work with
   * @throws SQLException on a database error.
   * @throws RuntimeException if database contains no data for teamA for
   *           runNumber.
   */
  public static @Nullable Team pickWinner(final Connection connection,
                                          final int tournament,
                                          final PerformanceScoreCategory performanceElement,
                                          final List<TiebreakerTest> tiebreakerElement,
                                          final WinnerType winnerCriteria,
                                          final Team teamA,
                                          final Team teamB,
                                          final TeamScore teamBScore,
                                          final int runNumber)
      throws SQLException {
    try (DatabaseTeamScore teamAScore = new DatabaseTeamScore(GenerateDB.PERFORMANCE_TABLE_NAME, tournament,
                                                              teamA.getTeamNumber(), runNumber, connection)) {
      final Team retval = pickWinner(performanceElement, tiebreakerElement, winnerCriteria, teamA, teamAScore, teamB,
                                     teamBScore);
      return retval;
    }
  }

  /**
   * Decide who is the winner of runNumber. Calls Queries.updateScoreTotals() to
   * ensure the ComputedScore column is up to date
   *
   * @param connection database connection with write access to Performance
   *          table
   * @param performanceElement the XML element representing the performance
   *          scoring
   * @param tiebreakerElement the XML element representing the tiebreaker
   * @param winnerCriteria the criteria for picking a winner
   * @param teamA first team to check
   * @param teamB second team to check
   * @param runNumber what run to compare scores for
   * @return the team that is the winner. Team.TIE is returned in the case of a
   *         tie and null when the scores have not yet been entered
   * @see Team#TIE
   * @throws SQLException on a database error
   * @param tournament the tournament to work with
   */
  public static @Nullable Team pickWinner(final Connection connection,
                                          final int tournament,
                                          final PerformanceScoreCategory performanceElement,
                                          final List<TiebreakerTest> tiebreakerElement,
                                          final WinnerType winnerCriteria,
                                          final Team teamA,
                                          final Team teamB,
                                          final int runNumber)
      throws SQLException {
    try (
        DatabaseTeamScore teamAScore = new DatabaseTeamScore(GenerateDB.PERFORMANCE_TABLE_NAME, tournament,
                                                             teamA.getTeamNumber(), runNumber, connection);
        DatabaseTeamScore teamBScore = new DatabaseTeamScore(PerformanceScoreCategory.CATEGORY_TITLE, tournament,
                                                             teamB.getTeamNumber(), runNumber, connection)) {
      final Team retval = pickWinner(performanceElement, tiebreakerElement, winnerCriteria, teamA, teamAScore, teamB,
                                     teamBScore);
      return retval;
    }
  }

  /**
   * Pick the winner between the scores of two teams
   *
   * @return the winner, null on a tie or a missing score
   */
  private static @Nullable Team pickWinner(final PerformanceScoreCategory perf,
                                           final List<TiebreakerTest> tiebreakerElement,
                                           final WinnerType winnerCriteria,
                                           final Team teamA,
                                           final TeamScore teamAScore,
                                           final Team teamB,
                                           final TeamScore teamBScore) {

    // teamA can actually be a bye here in the degenerate case of a 3-team
    // tournament with 3rd/4th place brackets enabled...
    if (Team.BYE.equals(teamA)) {
      return teamB;
    } else if (Team.TIE.equals(teamA)
        || Team.TIE.equals(teamB)) {
      return null;
    } else {
      if (teamAScore.scoreExists()
          && teamBScore.scoreExists()) {
        final boolean noshowA = teamAScore.isNoShow();
        final boolean noshowB = teamBScore.isNoShow();
        if (noshowA
            && !noshowB) {
          return teamB;
        } else if (!noshowA
            && noshowB) {
          return teamA;
        } else {
          final double scoreA = perf.evaluate(teamAScore);
          final double scoreB = perf.evaluate(teamBScore);
          if (FP.lessThan(scoreA, scoreB, TIEBREAKER_TOLERANCE)) {
            return WinnerType.HIGH == winnerCriteria ? teamB : teamA;
          } else if (FP.lessThan(scoreB, scoreA, TIEBREAKER_TOLERANCE)) {
            return WinnerType.HIGH == winnerCriteria ? teamA : teamB;
          } else {
            return evaluateTiebreaker(tiebreakerElement, teamA, teamAScore, teamB, teamBScore);
          }
        }
      } else {
        return null;
      }
    }
  }

  /**
   * Evaluate the tiebreaker to determine the winner.
   *
   * @param tiebreakerElement the element from the XML document specifying the
   *          tiebreaker
   * @param teamAScore team A's score information
   * @param teamBScore team B's score information
   * @return the winner, may be Team.TIE
   */
  private static Team evaluateTiebreaker(final List<TiebreakerTest> tiebreakerElement,
                                         final Team teamA,
                                         final TeamScore teamAScore,
                                         final Team teamB,
                                         final TeamScore teamBScore) {

    // walk test elements in tiebreaker to decide who wins
    for (final TiebreakerTest testElement : tiebreakerElement) {
      final double sumA = testElement.evaluate(teamAScore);
      final double sumB = testElement.evaluate(teamBScore);
      final WinnerType highlow = testElement.getWinner();
      if (sumA > sumB) {
        return (WinnerType.HIGH == highlow ? teamA : teamB);
      } else if (sumA < sumB) {
        return (WinnerType.HIGH == highlow ? teamB : teamA);
      }
    }
    return Team.TIE;
  }

  /**
   * Insert a by run for a given team, tournament, run number in the performance
   * table.
   *
   * @param connection database connection
   * @param team the team
   * @param runNumber the run number
   * @throws SQLException on a database error
   */
  public static void insertBye(final Connection connection,
                               final Team team,
                               final int runNumber)
      throws SQLException {
    final int tournament = Queries.getCurrentTournament(connection);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Inserting bye for team: "
          + team.getTeamNumber()
          + " run: "
          + runNumber);
    }
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO Performance(TeamNumber, Tournament, RunNumber, Bye, Verified)"
            + " VALUES( ?, ?, ?, 1, 1)")) {
      prep.setInt(1, team.getTeamNumber());
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      prep.executeUpdate();
    }
  }

  /**
   * Get the performance score for the given team, tournament and run number.
   *
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param team the team
   * @param runNumber the run number
   * @return the performance score
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static double getPerformanceScore(final Connection connection,
                                           final int tournament,
                                           final Team team,
                                           final int runNumber)
      throws SQLException, IllegalArgumentException {
    if (null == team) {
      throw new IllegalArgumentException("Cannot get score for null team");
    } else {
      try (
          PreparedStatement stmt = connection.prepareStatement("SELECT ComputedTotal FROM Performance WHERE TeamNumber = ?"
              + " AND Tournament = ?"
              + " AND RunNumber = ?")) {
        stmt.setInt(1, team.getTeamNumber());
        stmt.setInt(2, tournament);
        stmt.setInt(3, runNumber);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return rs.getDouble(1);
          } else {
            throw new IllegalArgumentException("No score exists for tournament: "
                + tournament
                + " teamNumber: "
                + team.getTeamNumber()
                + " runNumber: "
                + runNumber);
          }
        }
      }
    }
  }

  /**
   * Get the value of NoShow for the given team, tournament and run number.
   *
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param team the team
   * @param runNumber the run number
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   * @see Queries#isNoShow(Connection, int, int, int)
   * @return is the specified run a no show?
   */
  public static boolean isNoShow(final Connection connection,
                                 final int tournament,
                                 final Team team,
                                 final int runNumber)
      throws SQLException {
    return Queries.isNoShow(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * Get the value of Bye for the given team, tournament and run number.
   *
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param team the team
   * @param runNumber the run number
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   * @return is the specified run a bye?
   * @see Queries#isBye(Connection, int, int, int)
   */
  public static boolean isBye(final Connection connection,
                              final int tournament,
                              final Team team,
                              final int runNumber)
      throws SQLException {
    return Queries.isBye(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * Initialize the database portion of the playoff brackets. The current
   * tournament is assumed to be the tournament to initialize.
   * <p>
   * Make sure that the teams listed here are not involved in any unfinished
   * playoffs, otherwise there will be problems.
   * </p>
   *
   * @param connection the connection
   * @param division the playoff division that the specified teams are in
   * @param enableThird true if 3rd place bracket needs to be computed
   * @param teams the teams that are to compete in the specified playoff
   *          division
   * @throws SQLException on database error
   * @throws RuntimeException if teams for the brackets are involved in
   *           unfinished playoffs
   * @param challengeDescription description of the tournament
   * @param bracketSort how to sort the initial seeding of the teams
   */
  public static void initializeBrackets(final Connection connection,
                                        final ChallengeDescription challengeDescription,
                                        final String division,
                                        final boolean enableThird,
                                        final List<? extends Team> teams,
                                        final BracketSortType bracketSort)
      throws SQLException {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initializing playoff bracket: "
          + division
          + " enableThird: "
          + enableThird);
    }
    final int currentTournament = Queries.getCurrentTournament(connection);

    final WinnerType winnerCriteria = challengeDescription.getWinner();

    // Initialize currentRound to contain a full bracket setup (i.e. playoff
    // round 1 teams)
    // Note: Our math will rely on the length of the list returned by
    // buildInitialBracketOrder to be a power of 2. It always should be.
    final List<? extends Team> firstRound = buildInitialBracketOrder(connection, bracketSort, winnerCriteria, teams);

    final List<Integer> teamNumbers = new LinkedList<>();
    for (final Team t : firstRound) {
      teamNumbers.add(t.getTeamNumber());
    }
    final String errors = Playoff.involvedInUnfinishedPlayoff(connection, currentTournament, teamNumbers);
    if (null != errors) {
      throw new RuntimeException("Some teams are involved in unfinished playoffs: "
          + errors);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initial bracket order: "
          + firstRound);
    }

    final int maxRoundForTeams = Playoff.getMaxPerformanceRound(connection, currentTournament, teamNumbers);

    // the performance run number that is equal to playoff round 0, the round
    // before the first playoff round
    // for the teams
    final int baseRunNumber;
    if (0 == maxRoundForTeams) {
      baseRunNumber = TournamentParameters.getNumSeedingRounds(connection, currentTournament);
    } else {
      baseRunNumber = maxRoundForTeams;
    }

    // insert byes for each team through baseRunNumber to ensure that the
    // performance table lines up
    insertByes(connection, baseRunNumber, teams);

    try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO PlayoffData"
        + " (Tournament, event_division, PlayoffRound, LineNumber, Team, run_number)" //
        + " VALUES (?, ?, ?, ?, ?, ?)")) {

      // Insert those teams into the database.
      // At this time we let the table assignment field default to NULL.
      final Iterator<? extends Team> it = firstRound.iterator();
      insertStmt.setInt(1, currentTournament);
      insertStmt.setString(2, division);
      insertStmt.setInt(3, 1);

      // run_number may overlap, but never for the same team with the
      // exception of the NULL team
      insertStmt.setInt(6, 1
          + baseRunNumber);
      int lineNbr = 1;
      while (it.hasNext()) {
        insertStmt.setInt(4, lineNbr);
        insertStmt.setInt(5, it.next().getTeamNumber());

        insertStmt.executeUpdate();

        lineNbr++;
      }

      // Create the remaining entries for the playoff data table using Null team
      // number
      int currentRoundSize = firstRound.size()
          / 2;
      int roundNumber = 2;
      insertStmt.setInt(1, currentTournament);
      insertStmt.setString(2, division);
      while (currentRoundSize > 0) {
        insertStmt.setInt(3, roundNumber);
        insertStmt.setInt(6, roundNumber
            + baseRunNumber);
        lineNbr = currentRoundSize;
        if (enableThird
            && currentRoundSize <= 2) {
          lineNbr = lineNbr
              * 2;
        }
        while (lineNbr >= 1) {
          insertStmt.setInt(4, lineNbr);
          insertStmt.setInt(5, Team.NULL.getTeamNumber());
          insertStmt.executeUpdate();
          lineNbr--;
        }
        roundNumber++;
        currentRoundSize = currentRoundSize
            / 2;
      }
    }

    // Now get all entries, ordered by PlayoffRound and LineNumber, and do
    // table
    // assignments in order of their occurrence in the database. Additionally,
    // for
    // any byes in the first round, populate the "winner" in the second round,
    // and
    // enter a BYE in the Performance table (I think score entry depends on a
    // "score"
    // being present for every round.)
    // Number of rounds is the log base 2 of the number of teams in round1
    // (including "bye" teams)
    final int numPlayoffRounds = (int) Math.round(Math.log(firstRound.size())
        / Math.log(2));
    try (PreparedStatement selStmt = connection.prepareStatement("SELECT PlayoffRound,LineNumber,Team FROM PlayoffData"//
        + " WHERE Tournament= ?" //
        + " AND event_division= ?" //
        + " ORDER BY PlayoffRound,LineNumber")) {
      selStmt.setInt(1, currentTournament);
      selStmt.setString(2, division);
      try (ResultSet rs = selStmt.executeQuery()) {
        // Condition must look at roundnumber because we don't need to assign
        // anything
        // to the rightmost "round" where the winning team numbers will reside,
        // which
        // exist because I'm lazy and don't want to add special checks to the
        // update/insert
        // methods to see if they shouldn't write that last winning team entry
        // because they
        // are on the last round that has scores associated with it.
        while (rs.next()
            && numPlayoffRounds
                - rs.getInt(1) >= 0) {
          // Obtain the data for both teams in a match
          final int round1 = rs.getInt(1);
          final int line1 = rs.getInt(2);
          final int team1 = rs.getInt(3);
          if (!rs.next()) {
            throw new RuntimeException("Error initializing brackets: uneven number"
                + " of slots in playoff round "
                + round1);
          }
          final int round2 = rs.getInt(1);
          final int line2 = rs.getInt(2);
          final int team2 = rs.getInt(3);

          // Basic sanity checks...
          if (round1 != round2) {
            throw new RuntimeException("Error initializing brackets. Round number"
                + " mismatch between teams expected to be in the same match");
          }
          if (line1
              + 1 != line2) {
            throw new RuntimeException("Error initializing brackets. Line numbers"
                + " are not consecutive");
          }

          // Advance teams if one of them is a bye...
          if ((team1 == Team.BYE_TEAM_NUMBER
              || team2 == Team.BYE_TEAM_NUMBER)
              && !(team1 == Team.BYE_TEAM_NUMBER
                  && team2 == Team.BYE_TEAM_NUMBER)) {
            final int teamToAdvance = (team1 == Team.BYE_TEAM_NUMBER ? team2 : team1);

            insertBye(connection, Team.getTeamFromDatabase(connection, teamToAdvance), baseRunNumber
                + 1);

            try (PreparedStatement insertStmt = connection.prepareStatement("UPDATE PlayoffData SET Team= ?" //
                + " WHERE Tournament= ?" //
                + " AND event_division= ?" //
                + " AND PlayoffRound= ?" //
                + " AND LineNumber=?")) {
              insertStmt.setInt(1, teamToAdvance);
              insertStmt.setInt(2, currentTournament);
              insertStmt.setString(3, division);
              insertStmt.setInt(4, round1
                  + 1);
              insertStmt.setInt(5, line2
                  / 2); // technically (line2+1)/2 but we
              // know
              // line2 is always the even team so this
              // is the same...
              insertStmt.execute();
              // Degenerate case of BYE team advancing to the loser's bracket
              // (i.e. a 3-team tournament with 3rd/4th place bracket enabled...)
              if (enableThird
                  && (numPlayoffRounds
                      - round1) == 1) {
                insertStmt.setInt(1, Team.BYE_TEAM_NUMBER);
                insertStmt.setInt(5, line2
                    / 2
                    + 2);
              }
            } // allocate insertStmt
          }
        }
      } // allocate rs
    } // allocate selStmt
  }

  /**
   * Determine the max performance run number used by any playoff bracket that
   * any of the listed teams have been involved in.
   *
   * @param connection the database connection
   * @param currentTournament the tournament
   * @param teamNumbers the team numbers to check
   * @return the max performance run number or 0 if no teams are in the playoffs
   *         yet
   * @throws SQLException on database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate query from list of teams")
  private static int getMaxPerformanceRound(final Connection connection,
                                            final int currentTournament,
                                            final List<Integer> teamNumbers)
      throws SQLException {
    final String teamNumbersStr = StringUtils.join(teamNumbers, ",");

    int maxRunNumber = 0;
    try (
        PreparedStatement divisionsPrep = connection.prepareStatement("SELECT DISTINCT event_division from PlayoffData WHERE" //
            + " Tournament = ?" //
            + " AND Team IN ( "
            + teamNumbersStr
            + " )")) {
      divisionsPrep.setInt(1, currentTournament);
      try (ResultSet divisions = divisionsPrep.executeQuery()) {

        while (divisions.next()) {
          final String eventDivision = castNonNull(divisions.getString(1));

          // find max run number for ANY team in the specified division, not
          // necessarily those in our list
          final int runNumber = getMaxPerformanceRound(connection, currentTournament, eventDivision);
          if (-1 != runNumber) {
            maxRunNumber = Math.max(maxRunNumber, runNumber);
          }
        }
      }
    }

    return maxRunNumber;

  }

  /**
   * Maximum playoff round, this is the final winner.
   *
   * @param connection database connection
   * @param tournament the tournament
   * @param playoffDivision the bracket name
   * @return max playoff round, -1 if there are no playoff rounds for this
   *         bracket
   * @throws SQLException on database error
   */
  public static int getMaxPlayoffRound(final Connection connection,
                                       final int tournament,
                                       final String playoffDivision)
      throws SQLException {
    final int maxPerformanceRound = getMaxPerformanceRound(connection, tournament, playoffDivision);
    if (maxPerformanceRound < 0) {
      return -1;
    } else {
      return getPlayoffRound(connection, tournament, playoffDivision, maxPerformanceRound);
    }
  }

  /**
   * Get max performance run number for playoff division.
   *
   * @param playoffDivision the division to check
   * @return performance round, -1 if there are no playoff rounds for this
   *         division
   * @param connection database connection
   * @param currentTournament tournament to work with
   * @throws SQLException on database error
   */
  public static int getMaxPerformanceRound(final Connection connection,
                                           final int currentTournament,
                                           final String playoffDivision)
      throws SQLException {
    try (PreparedStatement maxPrep = connection.prepareStatement("SELECT MAX(run_number) FROM PlayoffData WHERE" //
        + " event_division = ? AND tournament = ?")) {

      maxPrep.setString(1, playoffDivision);
      maxPrep.setInt(2, currentTournament);

      try (ResultSet max = maxPrep.executeQuery()) {
        if (max.next()) {
          final int runNumber = max.getInt(1);
          return runNumber;
        } else {
          return -1;
        }
      }
    }
  }

  /**
   * Compute the assignments to the initial playoff brackets.
   *
   * @param numTeams will be rounded up to the next power of 2
   * @return the initial bracket index 0 plays 1, 2 plays 3, will have size of
   *         numTeams rounded up to next power of 2
   * @throws IllegalArgumentException if numTeams is less than 1
   */
  public static int[] computeInitialBrackets(final int numTeams) {
    if (numTeams < 1) {
      throw new IllegalArgumentException("numTeams must be greater than or equal to 1 found: "
          + numTeams
          + " perhaps teams have not had scores entered for regular match play?");
    }

    int n = numTeams;
    while (!isPowerOfTwoFast(n)) {
      ++n;
    }

    if (2 == n
        || 1 == n) {
      return new int[] { 1, 2 };
    } else {
      final int[] smallerBracket = computeInitialBrackets(n
          / 2);
      final int[] retval = new int[n];
      for (int index = 0; index < smallerBracket.length; ++index) {
        final int team = smallerBracket[index];
        final int opponent = n
            - team
            + 1;

        if (index
            + 1 < n
                / 2) {
          retval[index
              * 2] = team;
          retval[index
              * 2
              + 1] = opponent;
        } else {
          retval[index
              * 2] = opponent;
          retval[index
              * 2
              + 1] = team;
        }
      }
      return retval;
    }
  }

  /**
   * Check if a number is a power of 2. Found at
   * http://sabbour.wordpress.com/2008/07/24/interview-question-check-that
   * -an-integer-is-a-power-of-two/
   *
   * @param n the number
   * @return if the number is a power of 2
   */
  private static boolean isPowerOfTwoFast(final int n) {
    return ((n != 0)
        && (n
            & (n
                - 1)) == 0);
  }

  /**
   * Get the list of playoff brackets at the specified tournament as a List of
   * Strings.
   *
   * @param connection the database connection
   * @return the List of brackets sorted by bracket name
   * @throws SQLException on a database error
   * @param tournament the tournament to work with
   */
  public static List<String> getPlayoffBrackets(final Connection connection,
                                                final int tournament)
      throws SQLException {
    final List<String> list = new LinkedList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT DISTINCT bracket_name FROM playoff_bracket_teams WHERE tournament_id = ? ORDER BY bracket_name")) {
      prep.setInt(1, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String division = castNonNull(rs.getString(1));
          list.add(division);
        }
      }
    }
    return list;
  }

  /**
   * Check if a particular playoff bracket is unfinished.
   *
   * @param connection the database connection
   * @param tournamentId the tournament
   * @param bracketName the bracket to check
   * @return true if the bracket is finished
   * @throws SQLException on database error
   */
  public static boolean isPlayoffBracketUnfinished(final Connection connection,
                                                   final int tournamentId,
                                                   final String bracketName)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT team FROM PlayoffData WHERE"//
        + " run_number = (SELECT MAX(run_number) FROM PlayoffData WHERE event_division = ? AND Tournament = ?)" //
        + " AND (team = ? OR team = ?)" //
        + " AND Tournament = ?" //
        + " AND event_division = ?")) {
      prep.setString(1, bracketName);
      prep.setInt(2, tournamentId);
      prep.setInt(3, Team.NULL.getTeamNumber()); // if the null team is the "winner", then it's not done
      prep.setInt(4, Team.TIE.getTeamNumber()); // if the tie team is the "winner", then it's not done
      prep.setInt(5, tournamentId);
      prep.setString(6, bracketName);

      try (ResultSet rs = prep.executeQuery()) {
        // if there are any results then the bracket is unfinished
        return rs.next();
      }
    }
  }

  /**
   * Find all unfinished playoff brackets.
   *
   * @param connection the database connection
   * @param tournamentId the tournament
   * @throws SQLException on a database error
   * @return name of brackets that are unfinished
   * @throws SQLException on database error
   */
  public static List<String> getUnfinishedPlayoffBrackets(final Connection connection,
                                                          final int tournamentId)
      throws SQLException {
    return getPlayoffBrackets(connection,
                              tournamentId).stream()
                                           .filter(Errors.rethrow()
                                                         .wrapPredicate(bracket -> isPlayoffBracketUnfinished(connection,
                                                                                                              tournamentId,
                                                                                                              bracket)))
                                           .collect(Collectors.toList());
  }

  /**
   * Check if some teams are involved in a playoff bracket that isn't finished.
   *
   * @param teamNumbers the teams to check, NULL, BYE and TIE team
   *          numbers will be ignored as they can be in multiple playoffs at the
   *          same time
   * @return null if no teams are involved in an unfinished playoff
   * @param connection database connection
   * @param tournament the tournament to work with
   * @throws SQLException on database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate query from list of teams")
  public static @Nullable String involvedInUnfinishedPlayoff(final Connection connection,
                                                             final int tournament,
                                                             final List<Integer> teamNumbers)
      throws SQLException {
    final StringBuilder message = new StringBuilder();

    final List<String> unfinishedPlayoffBrackets = getUnfinishedPlayoffBrackets(connection, tournament);

    final String teamNumbersStr = StringUtils.join(teamNumbers, ",");

    try (
        PreparedStatement detailPrep = connection.prepareStatement("SELECT DISTINCT Team from PlayoffData WHERE event_division = ?" //
            + " AND tournament = ?" //
            + " AND Team NOT IN (?, ?, ?)" // exclude internal teams
            + " AND Team IN ( "
            + teamNumbersStr
            + " )")) {
      detailPrep.setInt(2, tournament);
      detailPrep.setInt(3, Team.BYE.getTeamNumber());
      detailPrep.setInt(4, Team.TIE.getTeamNumber());
      detailPrep.setInt(5, Team.NULL.getTeamNumber());

      for (final String bracketName : unfinishedPlayoffBrackets) {
        detailPrep.setString(1, bracketName);
        try (ResultSet detail = detailPrep.executeQuery()) {
          while (detail.next()) {
            final int teamNumber = detail.getInt(1);
            message.append("<li>Team "
                + teamNumber
                + " is involved in the playoff bracket '"
                + bracketName
                + "', which isn't finished</li>");
          }
        }
      }
    }

    if (message.length() == 0) {
      return null;
    } else {
      return "<ul class='error'>"
          + message.toString()
          + "</ul>";
    }

  }

  /**
   * Insert byes for the specified teams up through baseRunNumber (inclusive).
   *
   * @throws SQLException on a database error
   */
  private static void insertByes(final Connection connection,
                                 final int baseRunNumber,
                                 final List<? extends Team> teams)
      throws SQLException {
    for (final Team team : teams) {
      final int maxRunCompleted = Queries.maxPerformanceRunNumberCompleted(connection, team.getTeamNumber());
      for (int round = maxRunCompleted
          + 1; round <= baseRunNumber; ++round) {
        insertBye(connection, team, round);
      }
    }
  }

  /**
   * Determine the playoff bracket number given a team number and performance
   * run number (1-based).
   *
   * @param connection the database connection
   * @param tournamentId id of the tournament
   * @param teamNumber the team
   * @param runNumber the run
   * @return the bracket number or -1 if the bracket cannot be determined
   * @throws SQLException if a database error occurs
   */
  public static int getBracketNumber(final Connection connection,
                                     final int tournamentId,
                                     final int teamNumber,
                                     final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT LineNumber FROM PlayoffData"//
        + " WHERE Team = ? " //
        + " AND run_number = ?"
        + " AND tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, runNumber);
      prep.setInt(3, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int lineNumber = rs.getInt(1);
          // Always want to round up
          final int bracket = (lineNumber
              + 1)
              / 2;
          return bracket;
        } else {
          return -1;
        }
      }
    }
  }

  /**
   * Find the playoff round run number for the specified division and
   * performance
   * run number in the tournament.
   *
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param division head to head bracket name
   * @param runNumber run number
   * @return the playoff round or -1 if not found
   * @throws SQLException on database error
   */
  public static int getPlayoffRound(final Connection connection,
                                    final int tournament,
                                    final String division,
                                    final int runNumber)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT PlayoffRound FROM PlayoffData"
        + " WHERE Tournament = ?"
        + " AND event_division = ?"
        + " AND run_number = ?")) {
      prep.setInt(1, tournament);
      prep.setString(2, division);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int playoffRound = rs.getInt(1);
          return playoffRound;
        } else {
          return -1;
        }
      }
    }
  }

  /**
   * Given a team, get the playoff brackets that the team is associated with.
   *
   * @param connection database connection
   * @param teamNumber team number
   * @return the brackets, may be an empty list
   * @throws SQLException on database error
   */
  public static List<String> getPlayoffBracketsForTeam(final Connection connection,
                                                       final int teamNumber)
      throws SQLException {
    final List<String> ret = new LinkedList<>();
    final int tournament = Queries.getCurrentTournament(connection);
    try (PreparedStatement prep = connection.prepareStatement("SELECT bracket_name FROM playoff_bracket_teams"
        + " WHERE team_number = ?"//
        + " AND tournament_id = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String bracket = castNonNull(rs.getString(1));
          ret.add(bracket);
        }
      }
    }
    return ret;
  }

  /**
   * Given a team and run number, get the playoff bracket.
   *
   * @param connection database connection
   * @param teamNumber the team number
   * @param tournamentId the tournament
   * @param runNumber the performance run number
   * @return the division
   * @throws SQLException on database error
   * @throws IllegalArgumentException if the the playoff bracket cannot be found
   */
  public static String getPlayoffDivision(final Connection connection,
                                          final int tournamentId,
                                          final int teamNumber,
                                          final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT event_division FROM PlayoffData"
        + " WHERE Team = ?"//
        + " AND run_number = ?" //
        + " AND Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, runNumber);
      prep.setInt(3, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String division = castNonNull(rs.getString(1));
          return division;
        } else {
          throw new IllegalArgumentException("Cannot find playoff bracket for team "
              + teamNumber
              + " run number "
              + runNumber
              + " in tournament "
              + tournamentId);
        }
      }
    }
  }

  /**
   * Given a team number and playoff round get the performance run number in the
   * current tournament.
   *
   * @param connection database connection
   * @param division the name of the head to head bracket
   * @param teamNumber the team number
   * @param playoffRound the playoff round number
   * @return the run number or -1 if not found
   * @throws SQLException on database error
   */
  public static int getRunNumber(final Connection connection,
                                 final String division,
                                 final int teamNumber,
                                 final int playoffRound)
      throws SQLException {
    final int tournament = Queries.getCurrentTournament(connection);
    try (PreparedStatement prep = connection.prepareStatement("SELECT run_number FROM PlayoffData" //
        + " WHERE Tournament = ?" //
        + " AND event_division = ?"
        + " AND PlayoffRound = ?" //
        + " AND Team = ?")) {
      prep.setInt(1, tournament);
      prep.setString(2, division);
      prep.setInt(3, playoffRound);
      prep.setInt(4, teamNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int runNumber = rs.getInt(1);
          return runNumber;
        } else {
          return -1;
        }
      }
    }
  }

  /**
   * Get the list of team numbers that are in the specified playoff bracket.
   * The bracket may not be initialized yet.
   *
   * @param connection database connection
   * @param tournamentId the tournament work with
   * @param bracketName the head to head bracket name
   * @throws SQLException on database error
   * @return list of team numbers
   */
  public static List<Integer> getTeamNumbersForPlayoffBracket(final Connection connection,
                                                              final int tournamentId,
                                                              final String bracketName)
      throws SQLException {
    final List<Integer> teams = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT team_number" //
        + " FROM playoff_bracket_teams" //
        + " WHERE tournament_id = ?" //
        + "   AND bracket_name = ?")) {
      prep.setInt(1, tournamentId);
      prep.setString(2, bracketName);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          teams.add(teamNumber);
        }
      }
    }

    return teams;
  }

  /**
   * Create a playoff bracket.
   * Does not check if the bracket already exists.
   * 
   * @param connection database connection
   * @param tournamentId the tournament to work with
   * @param bracketName the name of the bracket to create
   * @param teamNumbers the teams to put into the playoff bracket
   * @throws SQLException on database error
   */
  public static void createPlayoffBracket(final Connection connection,
                                          final int tournamentId,
                                          final String bracketName,
                                          final List<Integer> teamNumbers)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO playoff_bracket_teams (tournament_id, bracket_name, team_number) VALUES(?, ?, ?)")) {
      prep.setInt(1, tournamentId);
      prep.setString(2, bracketName);
      for (final Integer teamNumber : teamNumbers) {
        prep.setInt(3, teamNumber);
        prep.executeUpdate();
      }
    }
  }

  /**
   * Package visibility for testing.
   *
   * @param challenge the challenge description
   * @param simpleGoals populated with simple goal initial values
   * @param enumGoals populated with enum goal initial values
   */
  /* package */ static void populateInitialScoreMaps(final ChallengeDescription challenge,
                                                     final Map<String, Double> simpleGoals,
                                                     final Map<String, String> enumGoals) {
    for (final AbstractGoal agoal : challenge.getPerformance().getAllGoals()) {
      if (agoal instanceof Goal) {
        final Goal goal = (Goal) agoal;
        if (!goal.isComputed()) {
          final String name = goal.getName();
          final double initial = goal.getInitialValue();
          if (goal.isEnumerated()) {
            final EnumeratedValue einitial = goal.getValues().stream().filter(value -> Math.abs(initial
                - value.getScore()) < ChallengeParser.INITIAL_VALUE_TOLERANCE).findAny().orElse(null);
            if (null == einitial) {
              throw new FLLInternalException("Enumerated goal "
                  + name
                  + " doesn't have a value that matches the initial value "
                  + initial);
            } else {
              enumGoals.put(name, einitial.getValue());
            }
          } else {
            simpleGoals.put(name, initial);
          }
        } // not computed
      } // Goal
    } // foreach goal
  }

  private static boolean bracketHasTie(final Connection connection,
                                       final int tournamentId,
                                       final String bracketName)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM PlayoffData " //
        + " WHERE event_division = ?" //
        + " AND Tournament = ?" //
        + " AND Team = ?")) {
      prep.setString(1, bracketName);
      prep.setInt(2, tournamentId);
      prep.setInt(3, Team.TIE.getTeamNumber());

      try (ResultSet rs = prep.executeQuery()) {
        if (!rs.next()) {
          throw new FLLInternalException("Did not get result from COUNT(*) looking for ties");
        }

        final int numTies = rs.getInt(1);
        if (numTies > 0) {
          LOGGER.debug("Found "
              + numTies
              + " in bracket "
              + bracketName
              + " of tournament "
              + tournamentId);
          return true;
        } else {
          return false;
        }
      }
    }
  }

  /**
   * Finish a bracket by adding dummy scores to complete it.
   * For each pair competing that doesn't have a score, one will get a "No Show"
   * score and the other will get a score with all initial values.
   * If the bracket is already finished, then this method returns true without
   * making changes.
   * If the bracket has a tie, then this method returns false without making
   * changes.
   * If an exception occurs the database is consistent, but the bracket will not
   * be finished.
   *
   * @param connection the database connection
   * @param tournament the tournament that the bracket is in
   * @param bracketName the name of the head to head bracket
   * @param challenge the challenge description, used to get the goal names and
   *          their initial values.
   * @return true if the bracket is finished when the method returns, false if a
   *         tie is found
   * @throws SQLException if there is a problem talking to the database
   * @throws ParseException if there is an error parsing the score data
   */
  public static boolean finishBracket(final Connection connection,
                                      final ChallengeDescription challenge,
                                      final Tournament tournament,
                                      final String bracketName)
      throws SQLException, ParseException {
    if (bracketHasTie(connection, tournament.getTournamentID(), bracketName)) {
      return false;
    }
    if (!isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName)) {
      // nothing to do
      return true;
    }

    // populate maps for DummyTeamScore
    final Map<String, Double> simpleGoals = new HashMap<>();
    final Map<String, String> enumGoals = new HashMap<>();
    populateInitialScoreMaps(challenge, simpleGoals, enumGoals);

    // finish rounds from the beginning
    final List<RoundInfo> unfinishedRounds = gatherUnfinishedRounds(connection, tournament.getTournamentID(),
                                                                    bracketName);
    for (final RoundInfo info : unfinishedRounds) {
      LOGGER.trace("Computing winner for bracket: "
          + bracketName
          + " round: "
          + info.round
          + " line: "
          + info.dbLine);
      finishRound(connection, challenge, tournament, simpleGoals, enumGoals, bracketName, info);
    }

    // mark bracket as automatically finished
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO automatic_finished_playoff (tournament_id, bracket_name) VALUES (?, ?)")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, bracketName);
      prep.executeUpdate();
    }

    return true;
  }

  private static void finishRound(final Connection connection,
                                  final ChallengeDescription description,
                                  final Tournament tournament,
                                  final Map<String, Double> simpleGoals,
                                  final Map<String, String> enumGoals,
                                  final String bracketName,
                                  final RoundInfo info)
      throws SQLException, ParseException {
    // info is the round that we need the winner for, so we need to walk back
    // through the bracket to find the 2 teams that are competing for this spot.

    final int roundToFinish = info.round
        - 1;
    final int performanceRunNumberToEnter = info.runNumber
        - 1;

    final int finalRound = Queries.getNumPlayoffRounds(connection, tournament.getTournamentID(), bracketName);

    if (finalRound == info.round
        && info.dbLine > 2) {
      // must be the 3rd place bracket computation
      // these teams are already filled in, so skip this
      return;
    }

    final int teamBdbLine = info.dbLine
        * 2;
    final int teamAdbLine = teamBdbLine
        - 1;

    final int teamAteamNumber = Queries.getTeamNumberByPlayoffLine(connection, tournament.getTournamentID(),
                                                                   bracketName, teamAdbLine,
                                                                   performanceRunNumberToEnter);
    if (Team.NULL.getTeamNumber() == teamAteamNumber) {
      throw new FLLInternalException("Cannot find team for bracket: "
          + bracketName
          + " round: "
          + roundToFinish
          + " line: "
          + teamAdbLine);
    }
    final boolean teamAscoreExists = Queries.performanceScoreExists(connection, tournament.getTournamentID(),
                                                                    teamAteamNumber, performanceRunNumberToEnter);

    final int teamBteamNumber = Queries.getTeamNumberByPlayoffLine(connection, tournament.getTournamentID(),
                                                                   bracketName, teamBdbLine,
                                                                   performanceRunNumberToEnter);
    if (Team.NULL.getTeamNumber() == teamBteamNumber) {
      throw new FLLInternalException("Cannot find team for bracket: "
          + bracketName
          + " round: "
          + roundToFinish
          + " line: "
          + teamBdbLine);
    }
    final boolean teamBscoreExists = Queries.performanceScoreExists(connection, tournament.getTournamentID(),
                                                                    teamBteamNumber, performanceRunNumberToEnter);

    LOGGER.trace("Finishing performance run: "
        + performanceRunNumberToEnter
        + " for teams "
        + teamAteamNumber
        + ", "
        + teamBteamNumber);

    if (teamAscoreExists
        && teamBscoreExists) {
      LOGGER.warn("Trying to finish bracket "
          + bracketName
          + " round "
          + roundToFinish
          + " found that it's already finished, skipping");
      return;
    } else if (teamAscoreExists) {
      final TeamScore teamBscore = new DummyTeamScore(teamBteamNumber, performanceRunNumberToEnter, simpleGoals,
                                                      enumGoals, true, false);
      Queries.insertPerformanceScore(connection, description, tournament, true, teamBscore);

    } else if (teamBscoreExists) {
      final TeamScore teamAscore = new DummyTeamScore(teamAteamNumber, performanceRunNumberToEnter, simpleGoals,
                                                      enumGoals, true, false);
      Queries.insertPerformanceScore(connection, description, tournament, true, teamAscore);
    } else {
      // initial value score
      final TeamScore teamAscore = new DummyTeamScore(teamAteamNumber, performanceRunNumberToEnter, simpleGoals,
                                                      enumGoals, false, false);
      Queries.insertPerformanceScore(connection, description, tournament, true, teamAscore);

      // no show
      final TeamScore teamBscore = new DummyTeamScore(teamBteamNumber, performanceRunNumberToEnter, simpleGoals,
                                                      enumGoals, true, false);
      Queries.insertPerformanceScore(connection, description, tournament, true, teamBscore);
    }

  }

  /**
   * Compute the 3rd place database line given the database line of a team
   * competing in the semi-finals.
   *
   * @param dbLine the line of a team in the semi-finals
   * @return the database line that the team will be in if they lost in the
   *         semi-finals
   */
  public static int computeThirdPlaceDbLine(final int dbLine) {
    final int loserDbLine = (dbLine
        + 5)
        / 2;
    return loserDbLine;
  }

  /**
   * This method is the inverse of {@link #computeThirdPlaceDbLine(int)}.
   *
   * @param thirdPlaceDbLine a database line in the 3rd place bracket
   * @return a database line in the semi-finals
   */
  public static int computeSemiFinalDbLine(final int thirdPlaceDbLine) {
    return (2
        * thirdPlaceDbLine)
        - 5;
  }

  /**
   * @return list of rounds that are sorted by lowest round, then lowest line
   *         number.
   * @throws SQLException on database error
   */
  private static List<RoundInfo> gatherUnfinishedRounds(final Connection connection,
                                                        final int tournamentId,
                                                        final String bracketName)
      throws SQLException {
    final List<RoundInfo> unfinishedRounds = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT playoffround, linenumber, run_number" //
        + " FROM PlayoffData" //
        + " WHERE tournament = ?"//
        + " AND event_division = ?"
        + " AND team = ?")) {
      prep.setInt(1, tournamentId);
      prep.setString(2, bracketName);
      prep.setInt(3, Team.NULL.getTeamNumber());

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final RoundInfo info = new RoundInfo();
          info.round = rs.getInt(1);
          info.dbLine = rs.getInt(2);
          info.runNumber = rs.getInt(3);
          unfinishedRounds.add(info);
        }
      }
    }

    Collections.sort(unfinishedRounds);
    return unfinishedRounds;
  }

  /**
   * Get the list of brackets that are finished and have not been automatically
   * completed.
   * 
   * @param connection database connection
   * @param tournamentId tournament identifier
   * @return non-null list of brackets, may be empty
   * @throws SQLException on a database error
   * @see #isPlayoffBracketUnfinished(Connection, int, String)
   * @see #finishBracket(Connection, ChallengeDescription, Tournament, String)
   */
  @Nonnull
  public static List<String> getCompletedBrackets(final Connection connection,
                                                  final int tournamentId)
      throws SQLException {

    return getPlayoffBrackets(connection,
                              tournamentId).stream()
                                           .filter(Errors.rethrow()
                                                         .wrapPredicate(bracket -> !isAutomaticallyFinished(connection,
                                                                                                            tournamentId,
                                                                                                            bracket)))
                                           .filter(Errors.rethrow()
                                                         .wrapPredicate(bracket -> !isPlayoffBracketUnfinished(connection,
                                                                                                               tournamentId,
                                                                                                               bracket)))
                                           .collect(Collectors.toList());
  }

  /**
   * Check if a bracket has been automatically finished.
   * 
   * @param connection database connection
   * @param tournamentId tournament id
   * @param bracketName name of bracket to check
   * @return true if automatically finished, false otherwise (including
   *         unfinished)
   * @throws SQLException on a database error
   * @see #finishBracket(Connection, ChallengeDescription, Tournament, String)
   */
  public static boolean isAutomaticallyFinished(final Connection connection,
                                                final int tournamentId,
                                                final String bracketName)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM automatic_finished_playoff WHERE bracket_name = ? and tournament_id = ?")) {
      prep.setString(1, bracketName);
      prep.setInt(2, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        } else {
          return false;
        }
      }
    }
  }

  /**
   * Track information about the playoff round that needs to be populated.
   * Serialized via JSON to send to the web clients.
   */
  private static final class RoundInfo implements Comparable<RoundInfo> {
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public int round;

    @SuppressWarnings("checkstyle:visibilitymodifier")
    public int dbLine;

    @SuppressWarnings("checkstyle:visibilitymodifier")
    public int runNumber;

    @Override
    public int compareTo(final RoundInfo o) {
      if (this.round == o.round) {
        if (this.dbLine == o.dbLine) {
          return 0;
        } else if (this.dbLine < o.dbLine) {
          return -1;
        } else {
          return 1;
        }
      } else if (this.round < o.round) {
        return -1;
      } else {
        return 1;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(round, dbLine);
    }

    @Override
    @EnsuresNonNullIf(expression = "#1", result = true)
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      } else if (null == o) {
        return false;
      } else if (this.getClass() == o.getClass()) {
        final RoundInfo other = (RoundInfo) o;
        return other.round == this.round
            && other.dbLine == this.dbLine;
      } else {
        return false;
      }
    }
  }

}
