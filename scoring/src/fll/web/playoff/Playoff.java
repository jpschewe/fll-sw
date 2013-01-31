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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import fll.Team;
import fll.db.Queries;
import fll.util.FP;
import fll.util.LogUtils;
import fll.xml.BracketSortType;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.TiebreakerTest;
import fll.xml.WinnerType;

/**
 * Handle playoff information.
 */
public final class Playoff {

  private static final Logger LOGGER = LogUtils.getLogger();

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
   * @throws SQLException on a database error
   */
  public static List<Team> buildInitialBracketOrder(final Connection connection,
                                                    final BracketSortType bracketSort,
                                                    final WinnerType winnerCriteria,
                                                    final List<Team> teams) throws SQLException {
    if (null == connection) {
      throw new NullPointerException("Connection cannot be null");
    }

    final List<Team> seedingOrder;
    if (BracketSortType.ALPHA_TEAM == bracketSort) {
      seedingOrder = teams;

      // sort by team name
      Collections.sort(seedingOrder, Team.TEAM_NAME_COMPARATOR);
    } else if (BracketSortType.RANDOM == bracketSort) {
      seedingOrder = teams;
      // assign a random number to each team
      final double[] randoms = new double[seedingOrder.size()];
      final Random generator = new Random();
      for (int i = 0; i < randoms.length; ++i) {
        randoms[i] = generator.nextDouble();
      }
      Collections.sort(seedingOrder, new Comparator<Team>() {
        public int compare(final Team one,
                           final Team two) {
          final int oneIdx = seedingOrder.indexOf(one);
          final int twoIdx = seedingOrder.indexOf(two);
          return Double.compare(randoms[oneIdx], randoms[twoIdx]);
        }
      });
    } else {
      // standard seeding
      seedingOrder = Queries.getPlayoffSeedingOrder(connection, winnerCriteria, teams);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("seedingOrder: "
          + seedingOrder);
    }

    final int[] seeding = computeInitialBrackets(seedingOrder.size());

    // give byes to the last byesNeeded teams.
    final List<Team> list = new LinkedList<Team>();
    for (final int element : seeding) {
      if (element > seedingOrder.size()) {
        list.add(Team.BYE);
      } else {
        final Team team = seedingOrder.get(element - 1);
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
   * @throws SQLException on a database error.
   * @throws ParseException if the XML document is invalid.
   * @throws RuntimeException if database contains no data for teamA for
   *           runNumber.
   */
  public static Team pickWinner(final Connection connection,
                                final int tournament,
                                final PerformanceScoreCategory performanceElement,
                                final List<TiebreakerTest> tiebreakerElement,
                                final WinnerType winnerCriteria,
                                final Team teamA,
                                final Team teamB,
                                final TeamScore teamBScore,
                                final int runNumber) throws SQLException, ParseException {
    final TeamScore teamAScore = new DatabaseTeamScore("Performance", tournament, teamA.getTeamNumber(), runNumber, connection);
    final Team retval = pickWinner(performanceElement, tiebreakerElement, winnerCriteria, teamA, teamAScore, teamB,
                                   teamBScore);
    teamAScore.cleanup();
    teamBScore.cleanup();
    return retval;
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
   * @throws ParseException if the XML document is invalid
   */
  public static Team pickWinner(final Connection connection,
                                final int tournament,
                                final PerformanceScoreCategory performanceElement,
                                final List<TiebreakerTest> tiebreakerElement,
                                final WinnerType winnerCriteria,
                                final Team teamA,
                                final Team teamB,
                                final int runNumber) throws SQLException, ParseException {
    final TeamScore teamAScore = new DatabaseTeamScore("Performance", tournament, teamA.getTeamNumber(), runNumber, connection);
    final TeamScore teamBScore = new DatabaseTeamScore("Performance", tournament, teamB.getTeamNumber(), runNumber, connection);
    final Team retval = pickWinner(performanceElement, tiebreakerElement, winnerCriteria, teamA, teamAScore, teamB,
                                   teamBScore);
    teamAScore.cleanup();
    teamBScore.cleanup();
    return retval;
  }

  /**
   * Pick the winner between the scores of two teams
   * 
   * @return the winner, null on a tie or a missing score
   */
  private static Team pickWinner(final PerformanceScoreCategory perf,
                                 final List<TiebreakerTest> tiebreakerElement,
                                 final WinnerType winnerCriteria,
                                 final Team teamA,
                                 final TeamScore teamAScore,
                                 final Team teamB,
                                 final TeamScore teamBScore) throws ParseException {

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
                                         final TeamScore teamBScore) throws ParseException {

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
   * @throws SQLException on a database error
   */
  public static void insertBye(final Connection connection,
                               final Team team,
                               final int runNumber) throws SQLException {
    final int tournament = Queries.getCurrentTournament(connection);
    PreparedStatement prep = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Inserting bye for team: "
            + team.getTeamNumber() + " run: " + runNumber);
      }
      prep = connection.prepareStatement("INSERT INTO Performance(TeamNumber, Tournament, RunNumber, Bye, Verified)"
          + " VALUES( ?, ?, ?, 1, 1)");
      prep.setInt(1, team.getTeamNumber());
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the performance score for the given team, tournament and run number
   * 
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static double getPerformanceScore(final Connection connection,
                                           final int tournament,
                                           final Team team,
                                           final int runNumber) throws SQLException, IllegalArgumentException {
    if (null == team) {
      throw new IllegalArgumentException("Cannot get score for null team");
    } else {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {
        stmt = connection.prepareStatement("SELECT ComputedTotal FROM Performance WHERE TeamNumber = ?"
            + " AND Tournament = ?" + " AND RunNumber = ?");
        stmt.setInt(1, team.getTeamNumber());
        stmt.setInt(2, tournament);
        stmt.setInt(3, runNumber);
        rs = stmt.executeQuery();
        if (rs.next()) {
          return rs.getDouble(1);
        } else {
          throw new IllegalArgumentException("No score exists for tournament: "
              + tournament + " teamNumber: " + team.getTeamNumber() + " runNumber: " + runNumber);
        }
      } finally {
        SQLFunctions.close(rs);
        SQLFunctions.close(stmt);
      }
    }
  }

  /**
   * Get the value of NoShow for the given team, tournament and run number
   * 
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static boolean isNoShow(final Connection connection,
                                 final int tournament,
                                 final Team team,
                                 final int runNumber) throws SQLException {
    return Queries.isNoShow(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * Get the value of Bye for the given team, tournament and run number
   * 
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static boolean isBye(final Connection connection,
                              final int tournament,
                              final Team team,
                              final int runNumber) throws SQLException {
    return Queries.isBye(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * Initialize the database portion of the playoff brackets. The current
   * tournament is assumed to be the tournament to initialize.
   * 
   * @param connection the connection
   * @param division the playoff division that the specified teams are in
   * @param enableThird true if 3rd place bracket needs to be computed
   * @param teams the teams that are to compete in the specified playoff
   *          division
   * @throws SQLException on database error
   * @throws RuntimeException if teams for the brackets are involved in
   *           unfinished playoffs
   */
  public static void initializeBrackets(final Connection connection,
                                        final ChallengeDescription challengeDescription,
                                        final String division,
                                        final boolean enableThird,
                                        final List<Team> teams) throws SQLException {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initializing brackets for division: "
          + division + " enableThird: " + enableThird);
    }
    final int currentTournament = Queries.getCurrentTournament(connection);

    final BracketSortType bracketSort = challengeDescription.getBracketSort();
    final WinnerType winnerCriteria = challengeDescription.getWinner();

    // Initialize currentRound to contain a full bracket setup (i.e. playoff
    // round 1 teams)
    // Note: Our math will rely on the length of the list returned by
    // buildInitialBracketOrder to be a power of 2. It always should be.
    final List<Team> firstRound = buildInitialBracketOrder(connection, bracketSort, winnerCriteria, teams);

    final List<Integer> teamNumbers = new LinkedList<Integer>();
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
      baseRunNumber = Queries.getNumSeedingRounds(connection, currentTournament);
    } else {
      baseRunNumber = maxRoundForTeams;
    }

    // insert byes for each team through baseRunNumber to ensure that the
    // performance table lines up
    insertByes(connection, baseRunNumber, teams);

    PreparedStatement insertStmt = null;
    PreparedStatement selStmt = null;
    ResultSet rs = null;
    try {
      insertStmt = connection.prepareStatement("INSERT INTO PlayoffData"
          + " (Tournament, event_division, PlayoffRound, LineNumber, Team, run_number)" //
          + " VALUES (?, ?, ?, ?, ?, ?)");

      // Insert those teams into the database.
      // At this time we let the table assignment field default to NULL.
      final Iterator<Team> it = firstRound.iterator();
      insertStmt.setInt(1, currentTournament);
      insertStmt.setString(2, division);
      insertStmt.setInt(3, 1);

      // run_number may overlap, but never for the same team with the
      // exception of the NULL team
      insertStmt.setInt(6, 1 + baseRunNumber);
      int lineNbr = 1;
      while (it.hasNext()) {
        insertStmt.setInt(4, lineNbr);
        insertStmt.setInt(5, it.next().getTeamNumber());

        insertStmt.executeUpdate();

        lineNbr++;
      }

      // Create the remaining entries for the playoff data table using Null team
      // number
      int currentRoundSize = firstRound.size() / 2;
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
          lineNbr = lineNbr * 2;
        }
        while (lineNbr >= 1) {
          insertStmt.setInt(4, lineNbr);
          insertStmt.setInt(5, Team.NULL.getTeamNumber());
          insertStmt.executeUpdate();
          lineNbr--;
        }
        roundNumber++;
        currentRoundSize = currentRoundSize / 2;
      }
      SQLFunctions.close(insertStmt);
      insertStmt = null;

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
      selStmt = connection.prepareStatement("SELECT PlayoffRound,LineNumber,Team FROM PlayoffData"//
          + " WHERE Tournament= ?" //
          + " AND event_division= ?" //
          + " ORDER BY PlayoffRound,LineNumber");
      selStmt.setInt(1, currentTournament);
      selStmt.setString(2, division);
      rs = selStmt.executeQuery();
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
              + " of slots in playoff round " + round1);
        }
        final int round2 = rs.getInt(1);
        final int line2 = rs.getInt(2);
        final int team2 = rs.getInt(3);

        // Basic sanity checks...
        if (round1 != round2) {
          throw new RuntimeException("Error initializing brackets. Round number"
              + " mismatch between teams expected to be in the same match");
        }
        if (line1 + 1 != line2) {
          throw new RuntimeException("Error initializing brackets. Line numbers"
              + " are not consecutive");
        }

        // Advance teams if one of them is a bye...
        if ((team1 == Team.BYE_TEAM_NUMBER || team2 == Team.BYE_TEAM_NUMBER)
            && !(team1 == Team.BYE_TEAM_NUMBER && team2 == Team.BYE_TEAM_NUMBER)) {
          final int teamToAdvance = (team1 == Team.BYE_TEAM_NUMBER ? team2 : team1);

          insertBye(connection, Team.getTeamFromDatabase(connection, teamToAdvance), baseRunNumber + 1);

          insertStmt = connection.prepareStatement("UPDATE PlayoffData SET Team= ?" //
              + " WHERE Tournament= ?" //
              + " AND event_division= ?" //
              + " AND PlayoffRound= ?" //
              + " AND LineNumber=?");
          insertStmt.setInt(1, teamToAdvance);
          insertStmt.setInt(2, currentTournament);
          insertStmt.setString(3, division);
          insertStmt.setInt(4, round1 + 1);
          insertStmt.setInt(5, line2 / 2); // technically (line2+1)/2 but we
          // know
          // line2 is always the even team so this
          // is the same...
          insertStmt.execute();
          // Degenerate case of BYE team advancing to the loser's bracket
          // (i.e. a 3-team tournament with 3rd/4th place bracket enabled...)
          if (enableThird
              && (numPlayoffRounds - round1) == 1) {
            insertStmt.setInt(1, Team.BYE_TEAM_NUMBER);
            insertStmt.setInt(5, line2 / 2 + 2);
          }
          SQLFunctions.close(insertStmt);
          insertStmt = null;
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(selStmt);
      SQLFunctions.close(insertStmt);
    }
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
   * @throws SQLException
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate query from list of teams")
  private static int getMaxPerformanceRound(final Connection connection,
                                            final int currentTournament,
                                            final List<Integer> teamNumbers) throws SQLException {
    final String teamNumbersStr = StringUtils.join(teamNumbers, ",");

    PreparedStatement divisionsPrep = null;
    ResultSet divisions = null;
    int maxRunNumber = 0;
    try {
      divisionsPrep = connection.prepareStatement("SELECT DISTINCT event_division from PlayoffData WHERE" //
          + " Tournament = ?" //
          + " AND Team IN ( " + teamNumbersStr + " )");
      divisionsPrep.setInt(1, currentTournament);
      divisions = divisionsPrep.executeQuery();

      while (divisions.next()) {
        final String eventDivision = divisions.getString(1);

        final int runNumber = getMaxPerformanceRound(connection, eventDivision);
        if (-1 != runNumber) {
          maxRunNumber = Math.max(maxRunNumber, runNumber);
        }
      }

    } finally {
      SQLFunctions.close(divisions);
      SQLFunctions.close(divisionsPrep);
    }

    return maxRunNumber;

  }

  /**
   * Get max performance run number for playoff division.
   * 
   * @param division the event division to check
   * @return performance round, -1 if there are no playoff rounds for this
   *         division
   */
  public static int getMaxPerformanceRound(final Connection connection,
                                           final String playoffDivision) throws SQLException {
    PreparedStatement maxPrep = null;
    ResultSet max = null;
    try {
      maxPrep = connection.prepareStatement("SELECT MAX(run_number) FROM PlayoffData WHERE" //
          + " event_division = ?");

      maxPrep.setString(1, playoffDivision);
      max = maxPrep.executeQuery();
      if (max.next()) {
        final int runNumber = max.getInt(1);
        return runNumber;
      } else {
        return -1;
      }
    } finally {
      SQLFunctions.close(max);
      SQLFunctions.close(maxPrep);
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
          + numTeams + " perhaps teams have not had scores entered for seeding rounds?");
    }

    int n = numTeams;
    while (!isPowerOfTwoFast(n)) {
      ++n;
    }

    if (2 == n
        || 1 == n) {
      return new int[] { 1, 2 };
    } else {
      final int[] smallerBracket = computeInitialBrackets(n / 2);
      final int[] retval = new int[n];
      for (int index = 0; index < smallerBracket.length; ++index) {
        final int team = smallerBracket[index];
        final int opponent = n
            - team + 1;

        if (index + 1 < n / 2) {
          retval[index * 2] = team;
          retval[index * 2 + 1] = opponent;
        } else {
          retval[index * 2] = opponent;
          retval[index * 2 + 1] = team;
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
    return ((n != 0) && (n & (n - 1)) == 0);
  }

  /**
   * Get the list of playoff divisions at the specified tournament as a List of
   * Strings. This may be different from the event divisions for the overall
   * tournament if one has defined virtual divisions for running the playoffs
   * over a subset of teams.
   * 
   * @param connection the database connection
   * @return the List of divisions. List of strings.
   * @throws SQLException on a database error
   */
  public static List<String> getPlayoffDivisions(final Connection connection,
                                                 final int tournament) throws SQLException {
    final List<String> list = new LinkedList<String>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT DISTINCT event_division FROM PlayoffData WHERE Tournament = ? ORDER BY event_division");
      prep.setInt(1, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String division = rs.getString(1);
        list.add(division);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return list;
  }

  /**
   * Get the run number for a given playoff bracket.
   * This run number specifies the winner of the playoff bracket.
   * 
   * @return the run max run number or -1 if not found
   */
  public static int getMaxRunNumber(final Connection connection,
                                    final int tournament,
                                    final String division) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT MAX(run_number) FROM PlayoffData WHERE event_division = ? AND Tournament = ?");
      prep.setString(1, division);
      prep.setInt(2, tournament);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        return -1;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check if some teams are involved in an playoff bracket that isn't finished.
   * 
   * @param teamNumbers the teams to check, NULL, BYE and TIE team
   *          numbers will be ignored as they can be in multiple playoffs at the
   *          same time
   * @return null if no teams are involved in an unfinished playoff
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate query from list of teams")
  public static String involvedInUnfinishedPlayoff(final Connection connection,
                                                   final int tournament,
                                                   final List<Integer> teamNumbers) throws SQLException {
    final StringBuilder message = new StringBuilder();

    final String teamNumbersStr = StringUtils.join(teamNumbers, ",");

    PreparedStatement divisionsPrep = null;
    ResultSet divisions = null;
    PreparedStatement checkPrep = null;
    ResultSet check = null;
    PreparedStatement detailPrep = null;
    ResultSet detail = null;
    try {
      divisionsPrep = connection.prepareStatement("SELECT DISTINCT event_division from PlayoffData WHERE" //
          + " Tournament = ?" //
          + " AND Team IN ( " + teamNumbersStr + " )");
      divisionsPrep.setInt(1, tournament);
      divisions = divisionsPrep.executeQuery();

      checkPrep = connection.prepareStatement("SELECT * FROM PlayoffData WHERE" //
          + " run_number = " //
          + "   (SELECT MAX(run_number) FROM PlayoffData WHERE event_division = ? AND Tournament = ?)" //
          + "     AND team = ? AND Tournament = ?");
      checkPrep.setInt(2, tournament);
      checkPrep.setInt(3, Team.NULL.getTeamNumber());
      checkPrep.setInt(4, tournament);

      detailPrep = connection.prepareStatement("SELECT DISTINCT Team from PlayoffData WHERE event_division = ?" //
          + " AND tournament = ?" //
          + " AND Team NOT IN (?, ?, ?)" // exclude internal teams
          + " AND Team IN ( " + teamNumbersStr + " )");
      detailPrep.setInt(2, tournament);
      detailPrep.setInt(3, Team.BYE.getTeamNumber());
      detailPrep.setInt(4, Team.TIE.getTeamNumber());
      detailPrep.setInt(5, Team.NULL.getTeamNumber());

      while (divisions.next()) {
        final String eventDivision = divisions.getString(1);

        checkPrep.setString(1, eventDivision);
        check = checkPrep.executeQuery();
        if (check.next()) {

          detailPrep.setString(1, eventDivision);
          detail = detailPrep.executeQuery();
          while (detail.next()) {
            final int teamNumber = detail.getInt(1);
            message.append("<li>Team "
                + teamNumber + " is involved in the playoff for '" + eventDivision + "', which isn't finished</li>");
          }
          SQLFunctions.close(detail);
          detail = null;

        }
        SQLFunctions.close(check);
        check = null;
      }

    } finally {
      SQLFunctions.close(detail);
      SQLFunctions.close(detailPrep);
      SQLFunctions.close(check);
      SQLFunctions.close(checkPrep);
      SQLFunctions.close(divisions);
      SQLFunctions.close(divisionsPrep);
    }

    if (message.length() == 0) {
      return null;
    } else {
      return "<ul class='error'>"
          + message.toString() + "</ul>";
    }

  }

  /**
   * Insert byes for the specified teams up through baseRunNumber (inclusive).
   * 
   * @throws SQLException
   */
  static private void insertByes(final Connection connection,
                                 final int baseRunNumber,
                                 final List<Team> teams) throws SQLException {
    for (final Team team : teams) {
      final int maxRunCompleted = Queries.maxPerformanceRunNumberCompleted(connection, team.getTeamNumber());
      for (int round = maxRunCompleted + 1; round <= baseRunNumber; ++round) {
        insertBye(connection, team, round);
      }
    }
  }

  /**
   * Find the playoff round run number for the specified division and
   * performance
   * run number in the current tournament.
   * 
   * @return the playoff round or -1 if not found
   */
  public static int getPlayoffRound(final Connection connection,
                                    final String division,
                                    final int runNumber) throws SQLException {

    final int tournament = Queries.getCurrentTournament(connection);
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT PlayoffRound FROM PlayoffData"
          + " WHERE Tournament = ?" + " AND event_division = ?" + " AND run_number = ?");
      prep.setInt(1, tournament);
      prep.setString(2, division);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int playoffRound = rs.getInt(1);
        return playoffRound;
      } else {
        return -1;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Given a team and run number, get the playoff division
   * 
   * @return the division or null if not found
   */
  public static String getPlayoffDivision(final Connection connection,
                                          final int teamNumber,
                                          final int runNumber) throws SQLException {
    final int tournament = Queries.getCurrentTournament(connection);
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT event_division FROM PlayoffData"
          + " WHERE Team = ?"//
          + " AND run_number = ?" //
          + " AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, runNumber);
      prep.setInt(3, tournament);
      rs = prep.executeQuery();
      if (rs.next()) {
        final String division = rs.getString(1);
        return division;
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Given a team number and playoff round get the performance run number in the
   * current tournament
   * 
   * @return the run number or -1 if not found
   * @throws SQLException
   */
  public static int getRunNumber(final Connection connection,
                                 final int teamNumber,
                                 final int playoffRound) throws SQLException {
    final int tournament = Queries.getCurrentTournament(connection);
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT run_number FROM PlayoffData" //
          + " WHERE Tournament = ?" //
          + " AND PlayoffRound = ?" //
          + " AND Team = ?");
      prep.setInt(1, tournament);
      prep.setInt(2, playoffRound);
      prep.setInt(3, teamNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int runNumber = rs.getInt(1);
        return runNumber;
      } else {
        return -1;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }
}
