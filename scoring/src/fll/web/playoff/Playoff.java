/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.db.Queries;
import fll.util.FP;
import fll.util.ScoreUtils;
import fll.xml.BracketSortType;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

/**
 * Add class comment here!
 * 
 * @version $Revision$
 */
public final class Playoff {

  private static final Logger LOGGER = Logger.getLogger(Playoff.class);

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
   * @param divisionStr the division to generate brackets for, as a String
   * @param tournamentTeams keyed by team number
   * @return a List of teams
   * @throws SQLException on a database error
   */
  public static List<Team> buildInitialBracketOrder(final Connection connection,
                                                    final BracketSortType bracketSort,
                                                    final WinnerType winnerCriteria,
                                                    final String divisionStr,
                                                    final Map<Integer, Team> tournamentTeams) throws SQLException {
    if (null == connection) {
      throw new NullPointerException("Connection cannot be null");
    }

    if (BracketSortType.ALPHA_TEAM == bracketSort) {
      final List<Team> teams = new ArrayList<Team>(tournamentTeams.values());
      filterTeamsToDivision(connection, teams, divisionStr);
      // sort by team name
      Collections.sort(teams, new Comparator<Team>() {
        public int compare(final Team one, final Team two) {
          return one.getTeamName().compareTo(two.getTeamName());
        }
      });

      final int[] seeding = computeInitialBrackets(teams.size());
      final int byesNeeded = seeding.length
          - teams.size();

      // give byes to the last byesNeeded teams.
      final List<Team> list = new LinkedList<Team>();
      for (int teamIdx = 0; teamIdx < teams.size(); ++teamIdx) {
        list.add(teams.get(teamIdx));
        if (teamIdx >= teams.size()
            - byesNeeded) {
          list.add(Team.BYE);
        }
      }
      if (list.size() != seeding.length) {
        throw new InternalError("Programming error, list size should be the same as seeding length");
      }
      return list;
    } else {
      final List<Team> seedingOrder;
      if (BracketSortType.RANDOM == bracketSort) {
        seedingOrder = new ArrayList<Team>(tournamentTeams.values());
        filterTeamsToDivision(connection, seedingOrder, divisionStr);
        // assign a random number to each team
        final double[] randoms = new double[seedingOrder.size()];
        final Random generator = new Random();
        for (int i = 0; i < randoms.length; ++i) {
          randoms[i] = generator.nextDouble();
        }
        Collections.sort(seedingOrder, new Comparator<Team>() {
          public int compare(final Team one, final Team two) {
            final int oneIdx = seedingOrder.indexOf(one);
            final int twoIdx = seedingOrder.indexOf(two);
            return Double.compare(randoms[oneIdx], randoms[twoIdx]);
          }
        });
      } else {
        // standard seeding
        seedingOrder = Queries.getPlayoffSeedingOrder(connection, winnerCriteria, divisionStr, tournamentTeams);
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("seedingOrder: "
            + seedingOrder);
      }
      final int[] seeding = computeInitialBrackets(seedingOrder.size());
      final List<Team> list = new LinkedList<Team>();
      for (final int element : seeding) {
        if (element > seedingOrder.size()) {
          list.add(Team.BYE);
        } else {
          final Team team = (Team) seedingOrder.get(element - 1);
          list.add(team);
        }
      }
      return list;
    }

  }

  /**
   * Filter the specified list to just the teams in the specified event
   * division.
   * 
   * @param teams list that is modified
   * @param divisionStr the division to keep
   * @throws RuntimeException
   * @throws SQLException
   */
  private static void filterTeamsToDivision(final Connection connection, final List<Team> teams, final String divisionStr) throws SQLException,
      RuntimeException {
    final Iterator<Team> iter = teams.iterator();
    while (iter.hasNext()) {
      final Team t = iter.next();
      final String eventDivision = Queries.getEventDivision(connection, t.getTeamNumber());
      if (!eventDivision.equals(divisionStr)) {
        iter.remove();
      }
    }
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
   * @param request The servlet request object containing the form data from a
   *          scoresheet which is to be tested against the score for teamA and
   *          the given runNumber in the database.
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
                                final Element performanceElement,
                                final Element tiebreakerElement,
                                final WinnerType winnerCriteria,
                                final Team teamA,
                                final HttpServletRequest request,
                                final int runNumber) throws SQLException, ParseException {
    final TeamScore teamAScore = new DatabaseTeamScore(performanceElement, teamA.getTeamNumber(), runNumber, connection);
    final Team teamB = Team.getTeamFromDatabase(connection, Integer.parseInt(request.getParameter("TeamNumber")));
    final TeamScore teamBScore = new HttpTeamScore(performanceElement, teamB.getTeamNumber(), runNumber, request);
    final Team retval = pickWinner(tiebreakerElement, winnerCriteria, teamA, teamAScore, teamB, teamBScore);
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
                                final Element performanceElement,
                                final Element tiebreakerElement,
                                final WinnerType winnerCriteria,
                                final Team teamA,
                                final Team teamB,
                                final int runNumber) throws SQLException, ParseException {
    final TeamScore teamAScore = new DatabaseTeamScore(performanceElement, teamA.getTeamNumber(), runNumber, connection);
    final TeamScore teamBScore = new DatabaseTeamScore(performanceElement, teamB.getTeamNumber(), runNumber, connection);
    final Team retval = pickWinner(tiebreakerElement, winnerCriteria, teamA, teamAScore, teamB, teamBScore);
    teamAScore.cleanup();
    teamBScore.cleanup();
    return retval;
  }

  /**
   * Pick the winner between the scores of two teams
   * 
   * @return the winner, null on a tie or a missing score
   */
  private static Team pickWinner(final Element tiebreakerElement,
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
          final double scoreA = ScoreUtils.computeTotalScore(teamAScore);
          final double scoreB = ScoreUtils.computeTotalScore(teamBScore);
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
  private static Team evaluateTiebreaker(final Element tiebreakerElement,
                                         final Team teamA,
                                         final TeamScore teamAScore,
                                         final Team teamB,
                                         final TeamScore teamBScore) throws ParseException {

    // walk test elements in tiebreaker to decide who wins
    for (final Element testElement : XMLUtils.filterToElements(tiebreakerElement.getChildNodes())) {
      if ("test".equals(testElement.getTagName())) {
        final Map<String, Double> variableValues = Collections.emptyMap();
        final double sumA = ScoreUtils.evalPoly(testElement, teamAScore, variableValues);
        final double sumB = ScoreUtils.evalPoly(testElement, teamBScore, variableValues);
        final WinnerType highlow = XMLUtils.getWinnerCriteria(testElement);
        if (sumA > sumB) {
          return (WinnerType.HIGH == highlow ? teamA : teamB);
        } else if (sumA < sumB) {
          return (WinnerType.HIGH == highlow ? teamB : teamA);
        }
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
  public static void insertBye(final Connection connection, final Team team, final int runNumber) throws SQLException {
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
      SQLFunctions.closePreparedStatement(prep);
    }
  }

  /**
   * If team is not null, calls performanceScoreExists(connection,
   * team.getTeamNumber(), runNumber), otherwise returns false.
   * 
   * @see #performanceScoreExists(Connection, int, int)
   */
  public static boolean performanceScoreExists(final Connection connection, final Team team, final int runNumber) throws SQLException {
    if (null == team) {
      return false;
    } else {
      return performanceScoreExists(connection, team.getTeamNumber(), runNumber);
    }
  }

  /**
   * Test if a performance score exists for the given team, tournament and run
   * number
   * 
   * @throws SQLException on a database error
   */
  public static boolean performanceScoreExists(final Connection connection, final int teamNumber, final int runNumber) throws SQLException {
    final int tournament = Queries.getCurrentTournament(connection);

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT ComputedTotal FROM Performance"
          + " WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      return rs.next();
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
    }
  }

  /**
   * Get the performance score for the given team, tournament and run number
   * 
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static double getPerformanceScore(final Connection connection, final int tournament, final Team team, final int runNumber) throws SQLException,
      IllegalArgumentException {
    if (null == team) {
      throw new IllegalArgumentException("Cannot get score for null team");
    } else {
      Statement stmt = null;
      ResultSet rs = null;
      try {
        stmt = connection.createStatement();
        rs = stmt.executeQuery("SELECT ComputedTotal FROM Performance WHERE TeamNumber = "
            + team.getTeamNumber() + " AND Tournament = " + tournament + " AND RunNumber = " + runNumber);
        if (rs.next()) {
          return rs.getDouble(1);
        } else {
          throw new IllegalArgumentException("No score exists for tournament: "
              + tournament + " teamNumber: " + team.getTeamNumber() + " runNumber: " + runNumber);
        }
      } finally {
        SQLFunctions.closeResultSet(rs);
        SQLFunctions.closeStatement(stmt);
      }
    }
  }

  /**
   * Get the value of NoShow for the given team, tournament and run number
   * 
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static boolean isNoShow(final Connection connection, final int tournament, final Team team, final int runNumber) throws SQLException {
    return Queries.isNoShow(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * Get the value of Bye for the given team, tournament and run number
   * 
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static boolean isBye(final Connection connection, final int tournament, final Team team, final int runNumber) throws SQLException {
    return Queries.isBye(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * Returns true if the score has been verified, i.e. double-checked.
   */
  public static boolean isVerified(final Connection connection, final int tournament, final Team team, final int runNumber) throws SQLException {
    return Queries.isVerified(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * Pulls bracketSort and winnerCriteria out of challenge descriptor.
   */
  public static void displayPrintableBrackets(final Connection connection, final Document challengeDocument, final String division, final JspWriter out)
      throws IOException, SQLException, ParseException {
    final BracketSortType bracketSort = XMLUtils.getBracketSort(challengeDocument);
    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);

    final Element performanceElement = (Element) challengeDocument.getDocumentElement().getElementsByTagName("Performance").item(0);
    final Element tiebreakerElement = (Element) performanceElement.getElementsByTagName("tiebreaker").item(0);

    displayPrintableBrackets(connection, performanceElement, tiebreakerElement, bracketSort, winnerCriteria, division, out);
  }

  /**
   * Output the table for the printable brackets to out
   */
  public static void displayPrintableBrackets(final Connection connection,
                                              final Element performanceElement,
                                              final Element tiebreakerElement,
                                              final BracketSortType bracketSort,
                                              final WinnerType winnerCriteria,
                                              final String division,
                                              final JspWriter out) throws IOException, SQLException, ParseException {

    final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
    final int currentTournament = Queries.getCurrentTournament(connection);

    final int numSeedingRounds = Queries.getNumSeedingRounds(connection);

    // initialize currentRound to contain a full bracket setup
    List<Team> tempCurrentRound = buildInitialBracketOrder(connection, bracketSort, winnerCriteria, division, tournamentTeams);
    if (tempCurrentRound.size() > 1) {
      out.println("<table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>");

      // compute number of runs needed to complete the playoffs
      out.println("<tr>");
      final int initialBracketSize = tempCurrentRound.size();
      int numRuns = 1;
      while (Math.pow(2, numRuns) < initialBracketSize) {
        out.println("<th colspan='2'>Playoff Round "
            + numRuns + "</th>");
        numRuns++;
      }
      out.println("<th colspan='2'>Playoff Round "
          + numRuns + "</th>");
      numRuns++;
      out.println("<th colspan='2'>Playoff Round "
          + numRuns + "</th>");
      out.println("</tr>");

      // the row at which the last team was displayed
      final int[] lastTeam = new int[numRuns];
      Arrays.fill(lastTeam, 0);
      // the teams for a given round
      @SuppressWarnings("unchecked")
      final// until generic array creation is allowed
      Iterator<Team>[] currentRoundTeams = new Iterator[numRuns];
      // the bracket that will be displayed next
      final int[] bracketIndex = new int[numRuns];
      Arrays.fill(bracketIndex, 1);
      // the team that will be displayed next
      final int[] teamIndex = new int[numRuns];
      Arrays.fill(teamIndex, 1);

      for (int tempRunNumber = 0; tempRunNumber < numRuns; tempRunNumber++) {
        // save off the team order for later
        currentRoundTeams[tempRunNumber] = tempCurrentRound.iterator();

        final List<Team> newCurrentRound = new LinkedList<Team>();
        final Iterator<Team> prevIter = tempCurrentRound.iterator();
        while (prevIter.hasNext()) {
          final Team teamA = prevIter.next();
          if (prevIter.hasNext()) {
            final Team teamB = prevIter.next();
            final Team winner = pickWinner(connection, performanceElement, tiebreakerElement, winnerCriteria, teamA, teamB, tempRunNumber
                + numSeedingRounds + 1);
            newCurrentRound.add(winner);
          } else {
            // handle finals
            newCurrentRound.add(teamA);
          }
        }
        tempCurrentRound = newCurrentRound;
      }

      // Now create rows, 4 per initial bracket, two columns for each playoff
      // round
      // row is 1 based
      // playoffRunNumber is 0 based for array indexing
      for (int row = 1; row <= initialBracketSize * 4; row++) {
        out.println("<tr>");
        for (int playoffRunNumber = 0; playoffRunNumber < numRuns; playoffRunNumber++) {
          final boolean evenTeamIndex = (teamIndex[playoffRunNumber] % 2 == 0);
          if ((0 == lastTeam[playoffRunNumber] // haven't seen a team yet
          && ((row == ((1 << (playoffRunNumber + 2)) / 2) - 1) // first
          // team in a
          // later
          // bracket
          || (row == 1 && playoffRunNumber == 0))) // first team first
              // round
              || ((row - lastTeam[playoffRunNumber]) == (1 << (playoffRunNumber + 2)))) { // later
            // teams

            // keep track of where we last output a team
            lastTeam[playoffRunNumber] = row;
            // team information
            final Team team = currentRoundTeams[playoffRunNumber].next();
            out.println("<td class='Leaf' width='200'>");
            out.println(BracketData.getDisplayString(connection, currentTournament, (playoffRunNumber
                + numSeedingRounds + 1), team, false, false));

            out.println("</td>");

            if (evenTeamIndex) {
              // skip for bridge
              out.println("<!-- skip column for bridge -->");
            } else {
              // bridgetop
              out.println("<td class='BridgeTop' width='10'>&nbsp;</td>");
            }

            teamIndex[playoffRunNumber]++;
          } else if ((row - lastTeam[playoffRunNumber]) == 1
              && 0 != lastTeam[playoffRunNumber]) {
            // blank
            out.println("<td width='200'>&nbsp;</td>");
            if (!evenTeamIndex) {
              // blank
              out.println("<td width='10'>&nbsp;</td>");
            } else {
              if (playoffRunNumber != numRuns - 1) {
                // bridge of size 2^(playoffRunNumber+2) =
                // 1<<(playoffRunNumber+2)
                out.println("<td class='Bridge' rowspan='"
                    + (1 << (playoffRunNumber + 2)) + "'>&nbsp;</td>");
              } else {
                out.println("<td rowspan='"
                    + (1 << (playoffRunNumber + 2)) + "'>&nbsp;</td>");
              }
            }
          } else if ((row - lastTeam[playoffRunNumber]) == (1 << (playoffRunNumber + 2)) / 2
              && 0 != lastTeam[playoffRunNumber]) {
            // bracket number
            if (!evenTeamIndex) {
              // blank
              out.println("<td width='200'>&nbsp;</td>");
              out.println("<td width='10'>&nbsp;</td>");
            } else {
              if (playoffRunNumber != numRuns - 1) {
                out.println("<td width='200'><font size='4'>Bracket "
                    + bracketIndex[playoffRunNumber] + "</font><br /></td>");
                bracketIndex[playoffRunNumber]++;
              } else {
                out.println("<td width='200'>&nbsp;</td>");
              }
              // skip for bridge
              out.println("<!-- skip column for bridge -->");
            }
          } else {
            // blank
            out.println("<td width='200'>&nbsp;</td>");
            if (!evenTeamIndex
                || 0 == lastTeam[playoffRunNumber]) {
              // blank
              out.println("<td width='10'>&nbsp;</td>");
            } else {
              // skip for bridge
              out.println("<!-- skip column for bridge -->");
            }
          }
        }
        out.println("</tr>");
      }

      out.println("</table");
    } else {
      out.println("<p>Not enough teams for a playoff</p>");
    }
  }

  private static void genScoresheetForm(final JspWriter out,
                                        final Team teamA,
                                        final Team teamB,
                                        final String tableA,
                                        final String tableB,
                                        final List<String[]> tables,
                                        final String round) throws IOException {
    final String formName = "genScoresheet_"
        + teamA.getTeamNumber() + "_" + teamB.getTeamNumber();
    out.println("<form name='"
        + formName + "' action='ScoresheetGenerator' method='POST' target='_new'>");

    out.println("  <input type='hidden' name='numTeams' value='2'>");

    out.println("  <input type='hidden' name='TeamName1' value='"
        + teamA.getTeamName() + "'>");
    out.println("  <input type='hidden' name='TeamName2' value='"
        + teamB.getTeamName() + "'>");

    out.println("  <input type='hidden' name='TeamNumber1' value='"
        + teamA.getTeamNumber() + "'>");
    out.println("  <input type='hidden' name='TeamNumber2' value='"
        + teamB.getTeamNumber() + "'>");

    out.println("  1st side:<select name='Table1'>");
    Iterator<String[]> i = tables.iterator();
    while (i.hasNext()) {
      final String[] t = i.next();

      out.print("    <option value='"
          + t[0] + "'");
      if (t[0].equals(tableA)) {
        out.print(" selected");
      }
      out.println(">"
          + t[0] + "</option>");
      out.print("    <option value='"
          + t[1] + "'");
      if (t[1].equals(tableA)) {
        out.print(" selected>");
      }
      out.println(">"
          + t[1] + "</option>");
    }
    out.println("  </select><br/>");

    out.println("  2nd side:<select name='Table2'>");
    i = tables.iterator();
    while (i.hasNext()) {
      final String[] t = (String[]) i.next();

      out.print("    <option value='"
          + t[0] + "'");
      if (t[0].equals(tableB)) {
        out.print(" selected");
      }
      out.println(">"
          + t[0] + "</option>");

      out.print("    <option value='"
          + t[1] + "'");
      if (t[1].equals(tableB)) {
        out.print(" selected");
      }
      out.println(">"
          + t[1] + "</option>");
    }
    out.println("  </select><br/>");

    out.println("  <input type='hidden' name='RoundNumber1' value='"
        + round + "'>");
    out.println("  <input type='hidden' name='RoundNumber2' value='"
        + round + "'>");

    out.println("  <input type='submit' value='Get Scoresheets'>");

    out.println("</form>");
  }

  /**
   * Output the table for the printable brackets to out
   */
  @SuppressWarnings("unchecked")
  public static void displayScoresheetGenerationBrackets(final Connection connection,
                                                         final Document challengeDocument,
                                                         final String division,
                                                         final JspWriter out) throws IOException, SQLException, ParseException {
    final Element performanceElement = (Element) challengeDocument.getDocumentElement().getElementsByTagName("Performance").item(0);
    final Element tiebreakerElement = (Element) performanceElement.getElementsByTagName("tiebreaker").item(0);

    final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
    final int currentTournament = Queries.getCurrentTournament(connection);
    final int numSeedingRounds = Queries.getNumSeedingRounds(connection);
    final List<String[]> tournamentTables = Queries.getTournamentTables(connection);
    if (tournamentTables.size() == 0) {
      tournamentTables.add(new String[] { "", "" });
    }

    final BracketSortType bracketSort = XMLUtils.getBracketSort(challengeDocument);
    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);

    // initialize currentRound to contain a full bracket setup
    List<Team> tempCurrentRound = buildInitialBracketOrder(connection, bracketSort, winnerCriteria, division, tournamentTeams);
    if (tempCurrentRound.size() > 1) {
      out.println("<table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>");

      // compute number of runs needed to complete the playoffs
      out.println("<tr><th></th>");
      final int initialBracketSize = tempCurrentRound.size();
      int numRuns = 1;
      while (Math.pow(2, numRuns) < initialBracketSize) {
        out.println("<th colspan='2'>Playoff Round "
            + numRuns + "</th>");
        numRuns++;
      }
      out.println("<th colspan='2'>Playoff Round "
          + numRuns + "</th>");
      numRuns++;
      out.println("<th colspan='2'>Playoff Round "
          + numRuns + "</th>");
      out.println("</tr>");

      // the row at which the last team was displayed
      final int[] lastTeam = new int[numRuns];
      Arrays.fill(lastTeam, 0);
      // the teams for a given round
      final Iterator<Team>[] currentRoundTeams = new Iterator[numRuns];
      // the bracket that will be displayed next
      final int[] bracketIndex = new int[numRuns];
      Arrays.fill(bracketIndex, 1);
      // the team that will be displayed next
      final int[] teamIndex = new int[numRuns];
      Arrays.fill(teamIndex, 1);
      // the table that will be assigned next
      final Iterator<String[]>[] currentRoundTable = new Iterator[numRuns];
      // team iterator for scoresheet generation form
      final Iterator<Team>[] scoreGenTeams = new Iterator[numRuns];
      // match counts per round
      final int[] matchCounts = new int[numRuns];
      Arrays.fill(matchCounts, 0);

      for (int tempRunNumber = 0; tempRunNumber < numRuns; tempRunNumber++) {
        // save off the team order for later
        currentRoundTeams[tempRunNumber] = tempCurrentRound.iterator();
        scoreGenTeams[tempRunNumber] = tempCurrentRound.iterator();
        if (tempRunNumber > 0) {
          matchCounts[tempRunNumber] = matchCounts[tempRunNumber - 1];
        }

        final List<Team> newCurrentRound = new LinkedList<Team>();
        final Iterator<Team> prevIter = tempCurrentRound.iterator();
        while (prevIter.hasNext()) {
          final Team teamA = (Team) prevIter.next();
          if (prevIter.hasNext()) {
            final Team teamB = (Team) prevIter.next();
            final Team winner = pickWinner(connection, performanceElement, tiebreakerElement, winnerCriteria, teamA, teamB, tempRunNumber
                + numSeedingRounds + 1);
            newCurrentRound.add(winner);
            if (!((teamA != null && teamA.equals(Team.BYE)) || (teamB != null && teamB.equals(Team.BYE)))) {
              matchCounts[tempRunNumber] += 1;
            }
          } else {
            // handle finals
            newCurrentRound.add(teamA);
          }
        }
        currentRoundTable[tempRunNumber] = tournamentTables.iterator();
        if (tempRunNumber > 0) {
          final int iters = matchCounts[tempRunNumber - 1]
              % tournamentTables.size();
          for (int j = 0; j < iters; j++) {
            currentRoundTable[tempRunNumber].next();
          }
        }
        tempCurrentRound = newCurrentRound;
      }

      // Now create rows, 4 per initial bracket, two columns for each playoff
      // round
      // row is 1 based
      // playoffRunNumber is 0 based for array indexing
      // (1 << (playoffRunNumber+2)) == number of rows per bracket for the given
      // run
      for (int row = 1; row <= initialBracketSize * 4; row++) {
        out.println("<tr><td width='1' cellpadding='0'>&nbsp;</td>");
        for (int playoffRunNumber = 0; playoffRunNumber < numRuns; playoffRunNumber++) {
          final boolean evenTeamIndex = (teamIndex[playoffRunNumber] % 2 == 0);
          if ((0 == lastTeam[playoffRunNumber] // haven't seen a team yet
          && ((row == ((1 << (playoffRunNumber + 2)) / 2) - 1) // first
          // team in a
          // later
          // bracket
          || (row == 1 && playoffRunNumber == 0))) // first team first
              // round
              || ((row - lastTeam[playoffRunNumber]) == (1 << (playoffRunNumber + 2)))) { // later
            // teams

            // keep track of where we last output a team
            lastTeam[playoffRunNumber] = row;
            // team information
            final Team team = (Team) currentRoundTeams[playoffRunNumber].next();
            out.println("<td class='Leaf' width='200'>");
            out.println(BracketData.getDisplayString(connection, currentTournament, (playoffRunNumber
                + numSeedingRounds + 1), team, false, false));

            out.println("</td>");

            if (evenTeamIndex) {
              // skip for bridge
              out.println("<!-- skip column for bridge -->");
            } else {
              // bridgetop
              out.println("<td class='BridgeTop' width='10'>&nbsp;</td>");
            }

            teamIndex[playoffRunNumber]++;
          } else if ((row - lastTeam[playoffRunNumber]) == 1
              && 0 != lastTeam[playoffRunNumber]) {
            // bracket number
            if (!evenTeamIndex) {
              out.println("<td width='200'>&nbsp;</td>");
              out.println("<td width='10'>&nbsp;</td>");
            } else {
              if (playoffRunNumber != numRuns - 1) {
                out.println("<td width='200' valign='middle' rowspan='"
                    + ((1 << (playoffRunNumber + 2)) - 1) + "'><font size='4'>Bracket " + bracketIndex[playoffRunNumber] + "</font><br />");
                // TODO Test that this section isn't broken when only some
                // scores are entered.
                final Team teamA = (Team) scoreGenTeams[playoffRunNumber].next();
                final Team teamB = (Team) scoreGenTeams[playoffRunNumber].next();
                if (teamA != null
                    && teamB != null && !(teamA.equals(Team.BYE) || teamB.equals(Team.BYE))) {
                  if (!currentRoundTable[playoffRunNumber].hasNext()) {
                    currentRoundTable[playoffRunNumber] = tournamentTables.iterator();
                  }
                  final String[] tables = (String[]) currentRoundTable[playoffRunNumber].next();
                  genScoresheetForm(out, teamA, teamB, tables[0], tables[1], tournamentTables, "Playoff Round "
                      + (playoffRunNumber + 1));
                } else if (teamA == null
                    || teamB == null) {
                  if (!currentRoundTable[playoffRunNumber].hasNext()) {
                    currentRoundTable[playoffRunNumber] = tournamentTables.iterator();
                  }
                  currentRoundTable[playoffRunNumber].next();
                }
                out.println("</td>");
                bracketIndex[playoffRunNumber]++;
              } else {
                out.println("<td rowspan='"
                    + (1 << (playoffRunNumber + 2)) + "width='200'>&nbsp;</td>");
              }
              // skip for bridge
              out.println("<!-- skip column for bridge -->");

              if (playoffRunNumber != numRuns - 1) {
                // bridge of size 2^(playoffRunNumber+2) =
                // 1<<(playoffRunNumber+2)
                out.println("<td class='Bridge' rowspan='"
                    + (1 << (playoffRunNumber + 2)) + "'>&nbsp;</td>");
              } else {
                out.println("<td rowspan='"
                    + (1 << (playoffRunNumber + 2)) + "'>&nbsp;</td>");
              }
            }
          } else {
            if (!evenTeamIndex
                || 0 == lastTeam[playoffRunNumber]) {
              // blank
              out.println("<td width='200'>&nbsp;</td>");
              out.println("<td width='10'>&nbsp;</td>");
            } else {
              // skip for bridge
              out.println("<!-- skip column for bridge -->");
            }
          }
        }
        out.println("</tr>");
      }
      out.println("</table");
    } else {
      out.println("<p>Not enough teams for a playoff</p>");
    }
  }

  public static void initializeBrackets(final Connection connection, final Document challengeDocument, final String division, final boolean enableThird)
      throws IOException, SQLException, ParseException {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initializing brackets for division: "
          + division + " enableThird: " + enableThird);
    }

    final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
    final int currentTournament = Queries.getCurrentTournament(connection);
    final List<String[]> tournamentTables = Queries.getTournamentTables(connection);

    final BracketSortType bracketSort = XMLUtils.getBracketSort(challengeDocument);
    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);

    // Work-around for if they didn't initialize tournament table labels.
    if (tournamentTables.size() == 0) {
      tournamentTables.add(new String[] { "", "" });
    }

    // Iterator over table name pairs.
    // Iterator<String[]> tableIt = tournamentTables.iterator();

    // Initialize currentRound to contain a full bracket setup (i.e. playoff
    // round 1 teams)
    // Note: Our math will rely on the length of the list returned by
    // buildInitialBracketOrder to be a power of 2. It always should be.
    final List<Team> firstRound = buildInitialBracketOrder(connection, bracketSort, winnerCriteria, division, tournamentTeams);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initial bracket order: "
          + firstRound);
    }

    PreparedStatement insertStmt = null;
    insertStmt = connection.prepareStatement("INSERT INTO PlayoffData"
        + " (Tournament, event_division, PlayoffRound, LineNumber, Team)" + " VALUES (?, ?, ?, ?, ?)");

    // Insert those teams into the database.
    // At this time we let the table assignment field default to NULL.
    final Iterator<Team> it = firstRound.iterator();
    insertStmt.setInt(1, currentTournament);
    insertStmt.setString(2, division);
    insertStmt.setInt(3, 1);
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
    SQLFunctions.closePreparedStatement(insertStmt);
    insertStmt = null;

    // Now get all entries, ordered by PlayoffRound and LineNumber, and do table
    // assignments in order of their occurrence in the database. Additionally,
    // for
    // any byes in the first round, populate the "winner" in the second round,
    // and
    // enter a BYE in the Performance table (I think score entry depends on a
    // "score"
    // being present for every round.)
    PreparedStatement selStmt = null;
    ResultSet rs = null;
    // Number of rounds is the log base 2 of the number of teams in round1
    // (including "bye" teams)
    final int numPlayoffRounds = (int) Math.round(Math.log(firstRound.size())
        / Math.log(2));
    final int numSeedingRounds = Queries.getNumSeedingRounds(connection);
    try {
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

          insertBye(connection, Team.getTeamFromDatabase(connection, teamToAdvance), numSeedingRounds + 1);

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
          SQLFunctions.closePreparedStatement(insertStmt);
          insertStmt = null;
        }
      }
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(selStmt);
      SQLFunctions.closePreparedStatement(insertStmt);
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
      throw new IllegalArgumentException("numTeams must be greater than or equal to 1: "
          + numTeams);
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
}
