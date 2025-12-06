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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.diffplug.common.base.Errors;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
import fll.db.TableInformation;
import fll.scores.DatabasePerformanceTeamScore;
import fll.scores.DefaultPerformanceTeamScore;
import fll.scores.PerformanceTeamScore;
import fll.scores.TeamScore;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FP;
import fll.web.TournamentData;
import fll.xml.AbstractGoal;
import fll.xml.BracketSortType;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.EnumeratedValue;
import fll.xml.Goal;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
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
    if (BracketSortType.ALPHA_TEAM.equals(bracketSort)) {
      seedingOrder = teams;

      // sort by team name
      Collections.sort(seedingOrder, Team.TEAM_NAME_COMPARATOR);
    } else if (BracketSortType.RANDOM.equals(bracketSort)) {
      seedingOrder = teams;
      Collections.shuffle(seedingOrder);
    } else if (BracketSortType.CUSTOM.equals(bracketSort)) {
      seedingOrder = teams;
    } else if (BracketSortType.SEEDING.equals(bracketSort)) {
      // standard seeding
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final int numRegularMatchPlayRounds = RunMetadata.getNumRegularMatchPlayRounds(connection, tournament);
      if (numRegularMatchPlayRounds < 1) {
        throw new FLLRuntimeException("Cannot initialize playoff brackets using scores from regular match play when the number of regular match play rounds is less than 1");
      }

      seedingOrder = Queries.getPlayoffSeedingOrder(connection, winnerCriteria, teams);
    } else {
      throw new FLLInternalException(String.format("Unknown bracket sort type: '%s'", bracketSort));
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
                                          final PerformanceTeamScore teamBScore,
                                          final int runNumber)
      throws SQLException {
    final @Nullable PerformanceTeamScore teamAScore = DatabasePerformanceTeamScore.fetchTeamScore(tournament,
                                                                                                  teamA.getTeamNumber(),
                                                                                                  runNumber,
                                                                                                  connection);
    if (null == teamAScore) {
      return null;
    }

    final Team retval = pickWinner(performanceElement, tiebreakerElement, winnerCriteria, teamA, teamAScore, teamB,
                                   teamBScore);
    return retval;
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
                                           final PerformanceTeamScore teamAScore,
                                           final Team teamB,
                                           final PerformanceTeamScore teamBScore) {

    // teamA can actually be a bye here in the degenerate case of a 3-team
    // tournament with 3rd/4th place brackets enabled...
    if (Team.BYE.equals(teamA)) {
      return teamB;
    } else if (Team.TIE.equals(teamA)
        || Team.TIE.equals(teamB)) {
      return null;
    } else {
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
    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO performance"
        + " (TeamNumber, Tournament, RunNumber, Bye, Verified, tablename)"
        + " VALUES( ?, ?, ?, 1, 1, ?)")) {
      prep.setInt(1, team.getTeamNumber());
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      prep.setString(4, "BYE");
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

  private static void populateRunMetadata(final RunMetadataFactory runMetadataFactory,
                                          final int runNumber,
                                          final String playoffBracketName,
                                          final int playoffRound) {
    final RunMetadata metadata = new RunMetadata(runNumber, String.format("%s P%d", playoffBracketName, playoffRound),
                                                 true, RunMetadata.RunType.HEAD_TO_HEAD);
    runMetadataFactory.storeRunMetadata(metadata);
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
   * @param challengeDescription description of the tournament
   * @param tournamentData tournament data
   * @param division the playoff division that the specified teams are in
   * @param enableThird true if 3rd place bracket needs to be computed
   * @param teams the teams that are to compete in the specified playoff
   *          division
   * @param bracketSort how to sort the initial seeding of the teams
   * @throws SQLException on database error
   * @throws RuntimeException if teams for the brackets are involved in
   *           unfinished playoffs
   */
  public static void initializeBrackets(final Connection connection,
                                        final ChallengeDescription challengeDescription,
                                        final TournamentData tournamentData,
                                        final String division,
                                        final boolean enableThird,
                                        final List<? extends Team> teams,
                                        final BracketSortType bracketSort)
      throws SQLException {

    LOGGER.debug("initializing playoff bracket: {} enableThird: {}", division, enableThird);

    final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection);
    final Set<Team> less = Queries.getTeamsNeedingRegularMatchPlayRuns(connection,
                                                                       tournamentData.getRunMetadataFactory(),
                                                                       tournamentTeams, true);
    if (!less.isEmpty()) {
      throw new FLLRuntimeException(String.format("There are teams that have not completed all of their regular match play runs: %s",
                                                  less));
    }

    final Tournament tournament = tournamentData.getCurrentTournament();

    final WinnerType winnerCriteria = challengeDescription.getWinner();

    // Initialize currentRound to contain a full bracket setup (i.e. playoff
    // round 1 teams)
    // Note: Our math will rely on the length of the list returned by
    // buildInitialBracketOrder to be a power of 2. It always should be.
    final List<? extends Team> firstRound = buildInitialBracketOrder(connection, bracketSort, winnerCriteria, teams);

    final List<Integer> teamNumbers = new LinkedList<>();
    for (final Team t : firstRound) {
      if (!Team.isInternalTeamNumber(t.getTeamNumber())) {
        teamNumbers.add(t.getTeamNumber());
      }
    }
    final String errors = Playoff.involvedInUnfinishedPlayoff(connection, tournament.getTournamentID(), teamNumbers);
    if (null != errors) {
      throw new RuntimeException("Some teams are involved in unfinished playoffs: "
          + errors);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initial bracket order: "
          + firstRound);
    }

    // find the max performance round across the whole tournament. This puts each
    // playoff bracket in it's own set of performance rounds.
    // This makes it possible to uninitialize and delete brackets.
    final int maxRoundForTeams = Playoff.getMaxPerformanceRound(connection, tournament.getTournamentID());

    // the performance run number that is equal to playoff round 0, the round
    // before the first playoff round for the teams
    final int baseRunNumber;
    if (0 == maxRoundForTeams) {
      // if this is the case then either there are no regular match play runs or teams
      // have not finished the regular match play runs.
      // Above we check that all regular match play runs have been completed, so there
      // must not be any regular match play runs. So we just ask for the max run
      // number and go with that.
      baseRunNumber = Queries.getMaxRunNumber(connection, tournament);
    } else {
      baseRunNumber = maxRoundForTeams;
    }

    // insert byes for each team through baseRunNumber to ensure that the
    // performance table lines up
    insertByes(connection, baseRunNumber, teams);

    try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO PlayoffData"
        + " (Tournament, event_division, PlayoffRound, LineNumber, Team, run_number)" //
        + " VALUES (?, ?, ?, ?, ?, ?)");
        PreparedStatement insertTableStmt = connection.prepareStatement("INSERT INTO PlayoffTableData"
            + " (Tournament, event_division, PlayoffRound, LineNumber)" //
            + " VALUES (?, ?, ?, ?)")) {

      int roundNumber = 1;

      // Insert those teams into the database.
      // At this time we let the table assignment field default to NULL.
      final Iterator<? extends Team> it = firstRound.iterator();
      insertStmt.setInt(1, tournament.getTournamentID());
      insertStmt.setString(2, division);
      insertStmt.setInt(3, roundNumber);

      insertTableStmt.setInt(1, tournament.getTournamentID());
      insertTableStmt.setString(2, division);
      insertTableStmt.setInt(3, 1);

      // run_number may overlap, but never for the same team with the
      // exception of the NULL team
      insertStmt.setInt(6, roundNumber
          + baseRunNumber);
      int lineNbr = 1;
      populateRunMetadata(tournamentData.getRunMetadataFactory(), baseRunNumber
          + roundNumber, division, roundNumber);
      while (it.hasNext()) {
        insertStmt.setInt(4, lineNbr);
        insertTableStmt.setInt(4, lineNbr);
        insertStmt.setInt(5, it.next().getTeamNumber());

        insertStmt.executeUpdate();
        insertTableStmt.executeUpdate();

        ++lineNbr;
      }
      // initial table assignments
      for (int i = 1; i < lineNbr; ++i) {
        final int runNumber = baseRunNumber
            + roundNumber;
        final int playoffRound = Playoff.getPlayoffRound(connection, tournament.getTournamentID(), division, runNumber);
        assignPlayoffTable(connection, division, tournament.getTournamentID(), playoffRound, i);
      }
      ++roundNumber;

      // Create the remaining entries for the playoff data table using Null team
      // number
      int currentRoundSize = firstRound.size()
          / 2;
      while (currentRoundSize > 0) {
        insertStmt.setInt(3, roundNumber);
        insertStmt.setInt(6, baseRunNumber
            + roundNumber);
        insertTableStmt.setInt(3, roundNumber);

        lineNbr = currentRoundSize;
        if (enableThird
            && currentRoundSize <= 2) {
          lineNbr = lineNbr
              * 2;
        }

        populateRunMetadata(tournamentData.getRunMetadataFactory(), baseRunNumber
            + roundNumber, division, roundNumber);
        while (lineNbr >= 1) {
          insertStmt.setInt(4, lineNbr);
          insertStmt.setInt(5, Team.NULL.getTeamNumber());
          insertTableStmt.setInt(4, lineNbr);

          insertStmt.executeUpdate();
          insertTableStmt.executeUpdate();

          --lineNbr;
        }

        ++roundNumber;
        currentRoundSize = currentRoundSize
            / 2;
      }
    }
    // For any byes in the first round, populate the "winner" in the second round,
    // and enter a BYE in the Performance table (I think score entry depends on a
    // "score" being present for every round.)
    // Number of rounds is the log base 2 of the number of teams in round1
    // (including "bye" teams)
    final int numPlayoffRounds = (int) Math.round(Math.log(firstRound.size())
        / Math.log(2));
    try (PreparedStatement selStmt = connection.prepareStatement("SELECT PlayoffRound,LineNumber,Team FROM PlayoffData"//
        + " WHERE Tournament= ?" //
        + " AND event_division= ?" //
        + " ORDER BY PlayoffRound,LineNumber")) {
      selStmt.setInt(1, tournament.getTournamentID());
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
              final int lineNumber = (line2
                  + 1)
                  / 2;

              insertStmt.setInt(1, teamToAdvance);
              insertStmt.setInt(2, tournament.getTournamentID());
              insertStmt.setString(3, division);
              insertStmt.setInt(4, round1
                  + 1);
              insertStmt.setInt(5, lineNumber);
              insertStmt.execute();
              assignPlayoffTable(connection, division, tournament.getTournamentID(), round1
                  + 1, lineNumber);

              // Degenerate case of BYE team advancing to the loser's bracket
              // (i.e. a 3-team tournament with 3rd/4th place bracket enabled...)
              if (enableThird
                  && (numPlayoffRounds
                      - round1) == 1) {
                final int thirdPlaceDbLine = line2
                    / 2
                    + 2;
                insertStmt.setInt(1, Team.BYE_TEAM_NUMBER);
                insertStmt.setInt(5, thirdPlaceDbLine);
                insertStmt.execute();
                assignPlayoffTable(connection, division, tournament.getTournamentID(), round1
                    + 1, thirdPlaceDbLine);
              }
            } // allocate insertStmt
          }
        }
      } // allocate rs
    } // allocate selStmt
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
   * Get minimum (first) performance run number for playoff division.
   *
   * @param playoffDivision the division to check
   * @return performance round, -1 if there are no playoff rounds for this
   *         division
   * @param connection database connection
   * @param currentTournament tournament to work with
   * @throws SQLException on database error
   */
  public static int getMinPerformanceRound(final Connection connection,
                                           final int currentTournament,
                                           final String playoffDivision)
      throws SQLException {
    try (PreparedStatement maxPrep = connection.prepareStatement("SELECT MIN(run_number) FROM PlayoffData WHERE" //
        + " event_division = ? AND tournament = ?")) {

      maxPrep.setString(1, playoffDivision);
      maxPrep.setInt(2, currentTournament);

      try (ResultSet min = maxPrep.executeQuery()) {
        if (min.next()) {
          final int runNumber = min.getInt(1);
          return runNumber;
        } else {
          return -1;
        }
      }
    }
  }

  /**
   * Get max performance run number across all playoff brackets.
   *
   * @return performance round, -1 if there are no playoff rounds
   * @param connection database connection
   * @param currentTournament tournament to work with
   * @throws SQLException on database error
   */
  public static int getMaxPerformanceRound(final Connection connection,
                                           final int currentTournament)
      throws SQLException {
    try (PreparedStatement maxPrep = connection.prepareStatement("SELECT MAX(run_number) FROM PlayoffData WHERE" //
        + " tournament = ?")) {

      maxPrep.setInt(1, currentTournament);

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
   * Check if a performance run number is pat of an initialized playoff bracket.
   * 
   * @param connection database
   * @param tournament tournament
   * @param runNumber performance run to check
   * @return true if {code}runNumber{code} is part of an initialized playoff
   *         bracket
   */
  public static boolean isPlayoffRound(final Connection connection,
                                       final Tournament tournament,
                                       final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT run_number FROM PlayoffData WHERE" //
        + " tournament = ? and run_number = ? LIMIT 1")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
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

  private static int computePlayoffMatch(final int lineNumber) {
    final int match = (lineNumber
        / 2)
        + 1;
    return match;
  }

  /**
   * Find the playoff match run number for the specified division and
   * performance
   * run number in the tournament.
   *
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param division head to head bracket name
   * @param runNumber run number
   * @return the playoff match or -1 if not found
   * @throws SQLException on database error
   */
  public static int getPlayoffMatch(final Connection connection,
                                    final int tournament,
                                    final String division,
                                    final int runNumber)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT LineNumber FROM PlayoffData"
        + " WHERE Tournament = ?"
        + " AND event_division = ?"
        + " AND run_number = ?")) {
      prep.setInt(1, tournament);
      prep.setString(2, division);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int lineNumber = rs.getInt(1);
          return computePlayoffMatch(lineNumber);
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
   * @param runMetadataFactory run metadata cache
   * @param connection the database connection
   * @param datasource used to create background database connections
   * @param tournament the tournament that the bracket is in
   * @param bracketName the name of the head to head bracket
   * @param challenge the challenge description, used to get the goal names and
   *          their initial values.
   * @return true if the bracket is finished when the method returns, false if a
   *         tie is found
   * @throws SQLException if there is a problem talking to the database
   * @throws ParseException if there is an error parsing the score data
   */
  public static boolean finishBracket(final RunMetadataFactory runMetadataFactory,
                                      final Connection connection,
                                      final DataSource datasource,
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
      finishRound(runMetadataFactory, connection, datasource, challenge, tournament, simpleGoals, enumGoals,
                  bracketName, info);
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

  private static void finishRound(final RunMetadataFactory runMetadataFactory,
                                  final Connection connection,
                                  final DataSource datasource,
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
      final PerformanceTeamScore teamBscore = new DefaultPerformanceTeamScore(teamBteamNumber,
                                                                              performanceRunNumberToEnter, simpleGoals,
                                                                              enumGoals, PerformanceTeamScore.ALL_TABLE,
                                                                              true, false, true, LocalDateTime.now());
      Queries.insertPerformanceScore(runMetadataFactory, connection, datasource, description, tournament, true,
                                     teamBscore);

    } else if (teamBscoreExists) {
      final PerformanceTeamScore teamAscore = new DefaultPerformanceTeamScore(teamAteamNumber,
                                                                              performanceRunNumberToEnter, simpleGoals,
                                                                              enumGoals, PerformanceTeamScore.ALL_TABLE,
                                                                              true, false, true, LocalDateTime.now());
      Queries.insertPerformanceScore(runMetadataFactory, connection, datasource, description, tournament, true,
                                     teamAscore);
    } else {
      // initial value score
      final PerformanceTeamScore teamAscore = new DefaultPerformanceTeamScore(teamAteamNumber,
                                                                              performanceRunNumberToEnter, simpleGoals,
                                                                              enumGoals, PerformanceTeamScore.ALL_TABLE,
                                                                              false, false, true, LocalDateTime.now());
      Queries.insertPerformanceScore(runMetadataFactory, connection, datasource, description, tournament, true,
                                     teamAscore);

      // no show
      final PerformanceTeamScore teamBscore = new DefaultPerformanceTeamScore(teamBteamNumber,
                                                                              performanceRunNumberToEnter, simpleGoals,
                                                                              enumGoals, PerformanceTeamScore.ALL_TABLE,
                                                                              true, false, true, LocalDateTime.now());
      Queries.insertPerformanceScore(runMetadataFactory, connection, datasource, description, tournament, true,
                                     teamBscore);
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
   * @see #finishBracket(RunMetadataFactory, Connection, DataSource,
   *      ChallengeDescription, Tournament, String)
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
   * @see #finishBracket(RunMetadataFactory, Connection, DataSource,
   *      ChallengeDescription, Tournament, String)
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

  /**
   * Note that a performance score has changed and update the playoff table with
   * this new information.
   * 
   * @param connection database connection
   * @param currentTournament the id of the current tournament
   * @param winnerCriteria how to determine the winner
   * @param performanceElement the definition of the performance goals
   * @param tiebreakerElement how to break ties
   * @param teamNumber the team number
   * @param runNumber the run number
   * @param teamScore the score
   */
  public static void updatePlayoffScore(final Connection connection,
                                        final int currentTournament,
                                        final WinnerType winnerCriteria,
                                        final PerformanceScoreCategory performanceElement,
                                        final List<TiebreakerTest> tiebreakerElement,
                                        final int teamNumber,
                                        final int runNumber,
                                        final PerformanceTeamScore teamScore)
      throws SQLException, ParseException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Updating playoff score for team: "
          + teamNumber
          + " run: "
          + runNumber);
    }

    final Team team = Team.getTeamFromDatabase(connection, teamNumber);

    final int ptLine = Queries.getPlayoffTableLineNumber(connection, currentTournament, teamNumber, runNumber);

    final String division = Playoff.getPlayoffDivision(connection, currentTournament, teamNumber, runNumber);
    if (ptLine > 0) {
      // this makes sure that scores get pushed through to the displays
      H2HUpdateWebSocket.updateBracket(connection, performanceElement.getScoreType(), division, team, runNumber);

      final int siblingDbLine = getSiblingDbLine(ptLine);
      final int siblingTeam = Queries.getTeamNumberByPlayoffLine(connection, currentTournament, division, siblingDbLine,
                                                                 runNumber);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Sibling is: "
            + siblingTeam
            + " division: "
            + division);
      }

      // If sibling team is the NULL team, then updating this score is okay,
      // and no playoff meta data needs updating.
      if (Team.NULL_TEAM_NUMBER != siblingTeam) {
        // Sibling team is not null so we have to check if update can happen
        // anyway

        // See if the modification affects the result of the playoff match
        final int winnerDbLine = (ptLine
            + 1)
            / 2;
        final int winnerRunNumber = runNumber
            + 1;
        final int oldWinnerTeamNumber = Queries.getTeamNumberByPlayoffLine(connection, currentTournament, division,
                                                                           winnerDbLine, winnerRunNumber);

        final Team teamB = Team.getTeamFromDatabase(connection, siblingTeam);
        if (teamB == null) {
          throw new FLLRuntimeException("Unable to find team number in the database: "
              + teamNumber);
        }
        final Team newWinner = Playoff.pickWinner(connection, currentTournament, performanceElement, tiebreakerElement,
                                                  winnerCriteria, teamB, team, teamScore, runNumber);
        try (PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber FROM Performance" //
            + " WHERE TeamNumber = ?" //
            + " AND RunNumber > ?" //
            + " AND Tournament = ?")) {
          if (newWinner != null
              && oldWinnerTeamNumber != newWinner.getTeamNumber()) {
            // This score update changes the result of the match, so make sure
            // no other scores exist in later round for either of these 2 teams.
            if (Queries.getPlayoffTableLineNumber(connection, currentTournament, teamNumber, runNumber
                + 1) > 0) {
              prep.setInt(1, teamNumber);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new FLLRuntimeException("Unable to update score for team number "
                      + teamNumber
                      + " in performance run "
                      + runNumber
                      + " because that team has scores entered in subsequent playoff rounds which would become inconsistent. "
                      + "Delete those scores and then you may update this score.");
                }
              }
            }
            if (Queries.getPlayoffTableLineNumber(connection, currentTournament, siblingTeam, runNumber
                + 1) > 0) {
              prep.setInt(1, siblingTeam);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new FLLRuntimeException("Unable to update score for team number "
                      + teamNumber
                      + " in performance run "
                      + runNumber
                      + " because opponent team "
                      + siblingTeam
                      + " has scores in subsequent playoff rounds which would become inconsistent. "
                      + "Delete those scores and then you may update this score.");
                }
              }
            }
          } // winner of the match changed

        } // PreparedStatement

        // If the second-check flag is NO or the opposing team is not
        // verified, we set the match "winner" (possibly back) to NULL.
        if (!teamScore.isVerified()
            || !(Queries.performanceScoreExists(connection, currentTournament, teamB, runNumber)
                && Queries.isVerified(connection, currentTournament, teamB, runNumber))) {
          removePlayoffScore(connection, division, currentTournament, runNumber, ptLine);
        } else if (null != newWinner) {
          // have a winner to record
          final Team newLoser;
          if (newWinner.equals(team)) {
            newLoser = teamB;
          } else {
            newLoser = team;
          }
          updatePlayoffData(connection, division, currentTournament, runNumber, ptLine, newWinner, newLoser);

        } // verified score
      } // no sibling
    } else {
      throw new FLLRuntimeException("Team "
          + teamNumber
          + " could not be found in the playoff table for performance run "
          + runNumber);
    }
  }

  /**
   * Update a row in the playoff table. Assign the specified team and printed
   * flags for the row found by (event_division, Tournament, PlayoffRound,
   * LineNumber).
   */
  private static void updatePlayoffTable(final Connection connection,
                                         final Team team,
                                         final String division,
                                         final int currentTournament,
                                         final int runNumber,
                                         final int lineNumber)
      throws SQLException {

    final int playoffRound = Playoff.getPlayoffRound(connection, currentTournament, division, runNumber);

    try (PreparedStatement prep = connection.prepareStatement("UPDATE PlayoffData" //
        + " SET Team = ?" //
        + ", Printed = ?" //
        + " WHERE event_division = ?" //
        + " AND Tournament = ?" //
        + " AND PlayoffRound = ?" //
        + " AND LineNumber = ?")) {
      prep.setInt(1, team.getTeamNumber());
      prep.setBoolean(2, false);
      prep.setString(3, division);
      prep.setInt(4, currentTournament);
      prep.setInt(5, playoffRound);
      prep.setInt(6, lineNumber);
      prep.executeUpdate();
    }

    final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);
    final PerformanceScoreCategory performance = description.getPerformance();
    final ScoreType performanceScoreType = performance.getScoreType();

    H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, team, runNumber, lineNumber);
  }

  /**
   * Remove the playoff score for the next run.
   * 
   * @param connection database connection
   * @param division playoff bracket name
   * @param currentTournament id of the current tournament
   * @param runNumber the run number
   * @param ptLine the playoff database line number
   */
  public static void removePlayoffScore(final Connection connection,
                                        final String division,
                                        final int currentTournament,
                                        final int runNumber,
                                        final int ptLine)
      throws SQLException {
    // winner and loser are both null now
    updatePlayoffData(connection, division, currentTournament, runNumber, ptLine, Team.NULL, Team.NULL);
  }

  private static void updatePlayoffData(final Connection connection,
                                        final String bracketName,
                                        final int tournamentId,
                                        final int runNumber,
                                        final int dbLine,
                                        final Team winner,
                                        final Team loser)
      throws SQLException {
    final int nextRunNumber = runNumber
        + 1;
    final int nextDbLine = ((dbLine
        + 1)
        / 2);

    final int nextPlayoffRound = Playoff.getPlayoffRound(connection, tournamentId, bracketName, nextRunNumber);
    assignPlayoffTable(connection, bracketName, tournamentId, nextPlayoffRound, nextDbLine);

    updatePlayoffTable(connection, winner, bracketName, tournamentId, nextRunNumber, nextDbLine);

    final int semiFinalRound = Queries.getNumPlayoffRounds(connection, tournamentId, bracketName)
        - 1;
    final int playoffRun = Playoff.getPlayoffRound(connection, tournamentId, bracketName, runNumber);
    if (playoffRun == semiFinalRound
        && Playoff.isThirdPlaceEnabled(connection, tournamentId, bracketName)) {
      final int thirdPlaceDbLine = Playoff.computeThirdPlaceDbLine(dbLine);
      updatePlayoffTable(connection, loser, bracketName, tournamentId, nextRunNumber, thirdPlaceDbLine);
    }
  }

  /**
   * Assign a table to the bracket defined by the team, if it's not already
   * assigned.
   */
  private static void assignPlayoffTable(final Connection connection,
                                         final String bracketName,
                                         final int tournamentId,
                                         final int playoffRound,
                                         final int dbLine)
      throws SQLException {
    final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);

    LOGGER.trace("Assigning table label for bracket: {} tournament: {} playoffRound: {} dbLine: {}", bracketName,
                 tournamentId, playoffRound, dbLine);

    // check for Bye and skip table assignment if this is a bye
    // get the 2 teams involved in the playoffRound
    if (Team.BYE_TEAM_NUMBER == getPlayoffTeamNumber(connection, tournament, bracketName, playoffRound, dbLine)) {
      LOGGER.trace("Not assigning table to bye round (this team).");
      return;
    } else if (Team.BYE_TEAM_NUMBER == getPlayoffTeamNumber(connection, tournament, bracketName, playoffRound,
                                                            getSiblingDbLine(dbLine))) {
      LOGGER.trace("Not assigning table to bye round (sibling team).");
      return;
    }

    final boolean oldAutoCommit = connection.getAutoCommit();

    try (PreparedStatement prep = connection.prepareStatement("UPDATE PlayoffTableData" //
        + " SET AssignedTable = ?" //
        + " WHERE event_division = ?" //
        + " AND Tournament = ?" //
        + " AND PlayoffRound = ?" //
        + " AND LineNumber = ?" //
        + " AND AssignedTable IS NULL")) {
      prep.setString(2, bracketName);
      prep.setInt(3, tournamentId);
      prep.setInt(4, playoffRound);

      connection.setAutoCommit(false);

      // Setting both values in a transaction will ensure that the line isn't modified
      // elsewhere.

      final List<TableInformation> allTables = TableInformation.getTournamentTableInformation(connection, tournament);
      final List<TableInformation> tables = TableInformation.filterToTablesForBracket(allTables, connection, tournament,
                                                                                      bracketName);
      TableInformation.sortByTableUsage(tables, connection, tournament);

      final TableInformation tableInfo = tables.get(0);

      prep.setString(1, tableInfo.getSideA());
      prep.setInt(5, dbLine);
      prep.executeUpdate();

      prep.setString(1, tableInfo.getSideB());
      final int siblingDbLine = getSiblingDbLine(dbLine);
      prep.setInt(5, siblingDbLine);
      prep.executeUpdate();

      // commit and if there is an error rollback the changes under the assumption
      // that another thread is updating the assigned table
      try {
        connection.commit();
      } catch (final SQLException e) {
        LOGGER.debug("Got error writing assigned table information. Assuming this is due to someone else modifying the information at the same time",
                     e);
        connection.rollback();
      }

    } finally {
      connection.setAutoCommit(oldAutoCommit);
    }

  }

  private static int getSiblingDbLine(final int ptLine) {
    final int siblingDbLine = ptLine
        % 2 == 0 ? ptLine
            - 1
            : ptLine
                + 1;
    return siblingDbLine;
  }

  /**
   * @param connection the database connection
   * @param tournament the tournament
   * @param division the head to head bracket
   * @return true if third place is enabled for the specified bracket
   * @throws SQLException on a database error
   */
  public static boolean isThirdPlaceEnabled(final Connection connection,
                                            final int tournament,
                                            final String division)
      throws SQLException {
    final int finalRound = Queries.getNumPlayoffRounds(connection, tournament, division);

    try (PreparedStatement prep = connection.prepareStatement("SELECT count(*) FROM PlayoffData" //
        + " WHERE Tournament= ?" //
        + " AND event_division= ?" //
        + " AND PlayoffRound= ?")) {
      prep.setInt(1, tournament);
      prep.setString(2, division);
      prep.setInt(3, finalRound);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) == 4;
        } else {
          return false;
        }
      }
    }
  }

  /**
   * Get table information for a team in the playoffs.
   * 
   * @param connection database connection
   * @param tournament the tournament
   * @param teamNumber number of the team to find
   * @param runNumber the performance run number to find
   * @return the table information or the empty string if not found
   * @throws SQLException on a database error
   */

  public static String getTableForRun(final Connection connection,
                                      final Tournament tournament,
                                      final int teamNumber,
                                      final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT assignedtable FROM playoffdata, playofftabledata" //
        + " WHERE playoffdata.team = ?" //
        + " AND playoffdata.run_number = ?" //
        + " AND playoffdata.tournament = ?" //
        + " AND playoffdata.tournament = playofftabledata.tournament" //
        + " AND playoffdata.playoffround = playofftabledata.playoffround" //
        + " AND playoffdata.event_division = playofftabledata.event_division" //
        + " AND playoffdata.linenumber = playofftabledata.linenumber" //
    )) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, runNumber);
      prep.setInt(3, tournament.getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String table = rs.getString(1);
          if (null == table) {
            return "";
          } else {
            return table;
          }
        } else {
          return "";
        }
      }
    }
  }

  /**
   * Find the team number that is at a particular position in a playoff bracket.
   * 
   * @param connection database connection
   * @param tournament tournament
   * @param bracketName name of the playoff bracket
   * @param playoffRound round in the playoff bracket
   * @param lineNumber the line number in the round
   * @return the team number on this line, {@link Team#NULL_TEAM_NUMBER} if not
   *         found
   * @throws SQLException on a database error
   */
  public static int getPlayoffTeamNumber(final Connection connection,
                                         final Tournament tournament,
                                         final String bracketName,
                                         final int playoffRound,
                                         final int lineNumber)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT team FROM PlayoffData" //
        + "  WHERE tournament = ?" //
        + "    AND event_division = ?" //
        + "    AND playoffround = ?" //
        + "    AND linenumber = ?" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, bracketName);
      prep.setInt(3, playoffRound);
      prep.setInt(4, lineNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int teamNumber = rs.getInt(1);
          return teamNumber;
        } else {
          return Team.NULL_TEAM_NUMBER;
        }
      }
    }
  }
}
