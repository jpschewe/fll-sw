/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import fll.Queries;
import fll.Team;
import fll.Utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import fll.xml.ChallengeParser;

import java.text.ParseException;

/**
 * Add class comment here!
 *
 * @version $Revision$
 */
final public class Playoff {

  /**
   * Just for debugging.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    try {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));

      if(null == challengeDocument) {
        throw new RuntimeException("Error parsing challenge.xml");
      }
      final Connection connection = Utilities.createDBConnection("disk", "fll_admin", "fll_admin", "fll");
//       final Element performanceElement = (Element)challengeDocument.getDocumentElement().getElementsByTagName("Performance").item(0);
//       final Element tiebreakerElement = (Element)performanceElement.getElementsByTagName("tiebreaker").item(0);
//       Node child = tiebreakerElement.getFirstChild();
//       while(null != child) {
//         if(child instanceof Element) {
//           final Element element = (Element)child;
//           if("test".equals(element.getTagName())) {
//             System.out.println("goal: " + element.getAttribute("goal"));
//             System.out.println("winner: " + element.getAttribute("winner"));
//           }
//         }
//         child = child.getNextSibling();
//       }
      System.out.println(buildInitialBracketOrder(connection,
                                                  Queries.getCurrentTournament(connection),
                                                  "1",
                                                  Queries.getTournamentTeams(connection)));
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private Playoff() {
     
  }

  /**
   * Build the list of teams ordered from top to bottom (visually) of a single
   * elimination bracket.
   *
   * @param connection connection to the database
   * @param currentTournament the current tournament
   * @param divisionStr the division to generate brackets for, as a String
   * @param tournamentTeams keyed by team number
   * @return a List of teams
   * @throws SQLException on a database error
   */
  public static List buildInitialBracketOrder(final Connection connection,
                                              final String currentTournament,
                                              final String divisionStr,
                                              final Map tournamentTeams)
    throws SQLException {

    final List seedingOrder = Queries.getPlayoffSeedingOrder(connection, currentTournament, divisionStr, tournamentTeams);
    if(seedingOrder.size() > 128) {
      //TODO one of these days I need to compute this rather than using an array
      throw new RuntimeException("More than 128 teams sent to playoff brackets!  System overload");
    }
    int bracketIndex = 0;
    while(SEED_ARRAY[bracketIndex].length < seedingOrder.size()) {
      bracketIndex++;
    }
    
    final List list = new LinkedList();
    for(int i=0; i<SEED_ARRAY[bracketIndex].length; i++) {
      if(SEED_ARRAY[bracketIndex][i] > seedingOrder.size()) {
        list.add(Team.BYE);
      } else {
        final Team team = (Team)seedingOrder.get(SEED_ARRAY[bracketIndex][i]-1);
        list.add(team);
      }
    }
    return list;
  }
  
  /**
   * Decide who is the winner of runNumber.  Calls Queries.updateScoreTotals()
   * to ensure the ComputedScore column is up to date
   *
   * @param connection database connection with write access to Performance table
   * @param document XML document description of tournament
   * @param tournament current tournament
   * @param teamA first team to check
   * @param teamB second team to check
   * @param runNumber what run to compare scores for
   * @return the team that is the winner.  Team.TIE is returned in the case of a
   * tie and null when the scores have not yet been entered
   * @see Team#TIE
   * @see fll.Queries#updateScoreTotals(Document, Connection, String)
   * @throws SQLException on a database error
   * @throws ParseException if the XML document is invalid
   */
  public static Team pickWinner(final Connection connection,
                                final Document document,
                                final String tournament,
                                final Team teamA,
                                final Team teamB,
                                final int runNumber)
    throws SQLException, ParseException {
    if(Team.BYE.equals(teamA)) {
      if(!performanceScoreExists(connection, tournament, teamB, runNumber)) {
        insertBye(connection, tournament, teamB, runNumber);
      }
      return teamB;
    } else if(Team.BYE.equals(teamB)) {
      if(!performanceScoreExists(connection, tournament, teamA, runNumber)) {
        insertBye(connection, tournament, teamA, runNumber);
      }
      return teamA;
    } else if(Team.TIE.equals(teamA) || Team.TIE.equals(teamB)) {
      return null;
    } else {
      //make sure scores are up to date
      Queries.updateScoreTotals(document, connection, tournament);
      
      if(performanceScoreExists(connection, tournament, teamA, runNumber)
         && performanceScoreExists(connection, tournament, teamB, runNumber)) {
        final boolean noshowA = isNoShow(connection, tournament, teamA, runNumber);
        final boolean noshowB = isNoShow(connection, tournament, teamB, runNumber);
        if(noshowA && !noshowB) {
          return teamB;
        } else if(!noshowA && noshowB) {
          return teamA;
        } else {
          final int scoreA = getPerformanceScore(connection, tournament, teamA, runNumber); 
          final int scoreB = getPerformanceScore(connection, tournament, teamB, runNumber);
          if(scoreA < scoreB) {
            return teamB;
          } else if(scoreB < scoreA) {
            return teamA;
          } else {
            final Element performanceElement = (Element)document.getDocumentElement().getElementsByTagName("Performance").item(0);
            final Element tiebreakerElement = (Element)performanceElement.getElementsByTagName("tiebreaker").item(0);

            Statement stmtA = null;
            Statement stmtB = null;
            ResultSet rsA = null;
            ResultSet rsB = null;
            try {
              stmtA = connection.createStatement();
              stmtB = connection.createStatement();
              rsA = stmtA.executeQuery("SELECT * FROM Performance WHERE Tournament = '" + tournament + "' AND RunNumber = " + runNumber + " and TeamNumber = " + teamA.getTeamNumber());
              rsB = stmtB.executeQuery("SELECT * FROM Performance WHERE Tournament = '" + tournament + "' AND RunNumber = " + runNumber + " and TeamNumber = " + teamB.getTeamNumber());
              if(rsA.next() && rsB.next()) {
                //walk test elements in tiebreaker to decide who wins
              
                Node child = tiebreakerElement.getFirstChild();
                while(null != child) {
                  if(child instanceof Element) {
                    final Element element = (Element)child;
                    if("test".equals(element.getTagName())) {
                      final int valueA = rsA.getInt(element.getAttribute("goal"));
                      final int valueB = rsB.getInt(element.getAttribute("goal"));
                      final String highlow = element.getAttribute("winner");
                      if(valueA > valueB) {
                        return ("high".equals(highlow) ? teamA : teamB);
                      } else if(valueA < valueB) {
                        return ("high".equals(highlow) ? teamB : teamA);
                      }
                    }
                  }
                  child = child.getNextSibling();
                }
              } else {
                throw new RuntimeException("Missing performance scores for teams: " + teamA.getTeamNumber() + " or " + teamB.getTeamNumber() + " for run: " + runNumber + " tournament: " + tournament);
              }
            } finally {
              Utilities.closeResultSet(rsA);
              Utilities.closeResultSet(rsB);
              Utilities.closeStatement(stmtA);
              Utilities.closeStatement(stmtB);
            }
            return Team.TIE;
          }
        }
      } else {
        return null;
      }
    }
  }

  /**
   * Insert a by run for a given team, tournament, run number in the
   * performance table.
   *
   * @throws SQLException on a database error
   */
  public static void insertBye(final Connection connection,
                               final String tournament,
                               final Team team,
                               final int runNumber)
    throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("INSERT INTO Performance(TeamNumber, Tournament, RunNumber, Bye) VALUES( " + team.getTeamNumber() + ", '" + tournament + "', " + runNumber + ", 1)");
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Test if a performance score exists for the given team, tournament and
   * run number
   *
   * @throws SQLException on a database error
   */
  public static boolean performanceScoreExists(final Connection connection,
                                               final String tournament,
                                               final Team team,
                                               final int runNumber)
    throws SQLException {
    if(null == team) {
      return false;
    } else {
      Statement stmt = null;
      ResultSet rs = null;
      try {
        stmt = connection.createStatement();
        rs = stmt.executeQuery("SELECT ComputedTotal FROM Performance"
                               + " WHERE TeamNumber = " + team.getTeamNumber()
                               + " AND Tournament = '" + tournament + "'"
                               + " AND RunNumber = " + runNumber);
        return rs.next();
      } finally {
        Utilities.closeResultSet(rs);
        Utilities.closeStatement(stmt);
      }
    }
  }

  /**
   * Get the performance score for the given team, tournament and run number
   *
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if no score exists
   */
  public static int getPerformanceScore(final Connection connection,
                                        final String tournament,
                                        final Team team,
                                        final int runNumber)
    throws SQLException, IllegalArgumentException {
    if(null == team) {
      throw new IllegalArgumentException("Cannot get score for null team");
    } else {
      Statement stmt = null;
      ResultSet rs = null;
      try {
        stmt = connection.createStatement();
        rs = stmt.executeQuery("SELECT ComputedTotal FROM Performance"
                               + " WHERE TeamNumber = " + team.getTeamNumber()
                               + " AND Tournament = '" + tournament + "'"
                               + " AND RunNumber = " + runNumber);
        if(rs.next()) {
          return rs.getInt(1);
        } else {
          throw new IllegalArgumentException("No score exists for tournament: " + tournament
                                             + " teamNumber: " + team.getTeamNumber()
                                             + " runNumber: " + runNumber);
        }
      } finally {
        Utilities.closeResultSet(rs);
        Utilities.closeStatement(stmt);
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
                                 final String tournament,
                                 final Team team,
                                 final int runNumber)
    throws SQLException, IllegalArgumentException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT NoShow FROM Performance"
                             + " WHERE TeamNumber = " + team.getTeamNumber()
                             + " AND Tournament = '" + tournament + "'"
                             + " AND RunNumber = " + runNumber);
      if(rs.next()) {
        return rs.getBoolean(1);
      } else {
        throw new RuntimeException("No score exists for tournament: " + tournament
                                   + " teamNumber: " + team.getTeamNumber()
                                   + " runNumber: " + runNumber);
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }  
  /**
   * What to display given a team number, handles TIE, null and BYE
   *
   * @param connection connection to the database
   * @param currentTournament the current tournament
   * @param runNumber the current run, used to get the score
   * @param team team to get display string for
   * @throws IllegalArgumentException if teamNumber is invalid
   * @throws SQLException on a database error
   * @see fll.Queries#getTournamentTeams(Connection, String)
   */
  public static String getDisplayString(final Connection connection,
                                        final String currentTournament,
                                        final int runNumber,
                                        final Team team)
    throws IllegalArgumentException, SQLException {
    if(Team.BYE.equals(team)) {
      return "<font class='TeamName'>BYE</font>";
    } else if(Team.TIE.equals(team)) {
      return "<font class='TIE'>TIE</font>";
    } else if(null == team) {
      return "&nbsp;";
    } else {
      final StringBuffer sb = new StringBuffer();
      sb.append("<font class='TeamNumber'>#");
      sb.append(team.getTeamNumber());
      sb.append("</font>&nbsp;<font class='TeamName'>");
      sb.append(team.getTeamName());
      sb.append("</font>");
      if(performanceScoreExists(connection, currentTournament, team, runNumber)) {
        sb.append("<font class='TeamScore'>&nbsp;Score: ");
        if(isNoShow(connection, currentTournament, team, runNumber)) {
          sb.append("No Show");
        } else {
          sb.append(getPerformanceScore(connection, currentTournament, team, runNumber));
        }
        sb.append("</font>");
      }
      return sb.toString();
    }
  }

  /**
   * Array of indicies used to determine who plays who in a single elimination
   * playoff bracket system
   */
  private static final int[][] SEED_ARRAY = new int[][] {
    {1,2}, // 1 team, just put the team with a bye
    {1,2}, // 2 teams
    {1,4,3,2}, // 4 teams
    {1,8,5,4,3,6,7,2}, // 8 teams
    {1,16,9,8,5,12,13,4,3,14,11,6,7,10,15,2}, // 16 teams
    {1,32,17,16,9,24,25,8,5,28,21,12,13,20,29,4,3,30,19,14,11,22,27,6,7,26,23,10,15,18,31,2}, // 32 teams
    {1,64,33,32,17,48,49,16,9,56,41,24,25,40,57,8,5,60,37,28,21,44,53,12,13,52,45,20,29,36,61,4,3,62,35,30,19,46,51,14,11,54,43,22,27,38,59,6,7,58,39,26,23,42,55,10,15,50,47,18,31,34,63,2}, // 64 teams
    {1,128,65,64,33,96,97,32,17,112,81,48,49,80,113,16,9,120,73,56,41,88,105,24,25,104,89,40,57,72,121,8,5,124,69,60,37,92,101,28,21,108,85,44,53,76,117,12,13,116,77,52,45,84,109,20,29,100,93,36,61,68,125,4,3,126,67,62,35,94,99,30,19,110,83,46,51,78,115,14,11,118,75,54,43,86,107,22,27,102,91,38,58,71,123,6,7,122,71,58,39,90,103,26,23,106,87,42,55,74,119,10,15,114,79,50,47,82,111,18,31,98,95,34,63,66,127,2} // 128 teams
  };
    
}
