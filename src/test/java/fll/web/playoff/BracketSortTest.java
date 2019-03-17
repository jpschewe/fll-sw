/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;

import fll.Team;
import fll.TestUtils;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.xml.BracketSortType;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.WinnerType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Test the various playoff bracket sort methods.
 * 
 * @author jpschewe
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class BracketSortTest {

  /**
   * Test sorting alphabetically by team name.
   * 
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws SQLException
   * @throws UnsupportedEncodingException
   */
  @Test
  public void testAlphaTeam() throws InstantiationException, IllegalAccessException, ClassNotFoundException,
      SQLException, UnsupportedEncodingException {
    final String divisionStr = "1";
    final String[] teamNames = new String[] { "A", "B", "C", "D", "E", "F" };
    Connection connection = null;
    try {
      // load in data/alpha-team-sort.xml
      final InputStream challengeDocIS = BracketSortTest.class.getResourceAsStream("data/alpha-team-sort.xml");
      assertNotNull(challengeDocIS);
      final Document document = ChallengeParser.parse(new InputStreamReader(challengeDocIS, Utilities.DEFAULT_CHARSET));
      assertNotNull(document);

      final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());

      // create in memory test database instance
      Class.forName("org.hsqldb.jdbcDriver").newInstance();
      connection = DriverManager.getConnection("jdbc:hsqldb:mem:flldb-testAlphaTeam");
      GenerateDB.generateDB(document, connection);

      final int tournament = Queries.getCurrentTournament(connection);

      // put some teams in the database
      // final Map<Integer, Team> tournamentTeams = new HashMap<Integer,
      // Team>();
      for (int i = 0; i < teamNames.length; ++i) {
        final String otherTeam = Queries.addTeam(connection, teamNames.length
            - i, teamNames[i], null);
        assertNull(otherTeam);
        Queries.addTeamToTournament(connection, teamNames.length
            - i, tournament, divisionStr, divisionStr);
        // final Team team = new Team();
        // team.setDivision(divisionStr);
        // team.setTeamName(teamNames[i]);
        // team.setTeamNumber(teamNames.length
        // - i);
        // tournamentTeams.put(team.getTeamNumber(), team);
      }

      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection);

      final BracketSortType bracketSort = BracketSortType.ALPHA_TEAM;
      final WinnerType winnerCriteria = description.getWinner();

      final List<TournamentTeam> teams = new ArrayList<TournamentTeam>(tournamentTeams.values());
      TournamentTeam.filterTeamsToEventDivision(teams, divisionStr);

      final List<Team> order = Playoff.buildInitialBracketOrder(connection, bracketSort, winnerCriteria, teams);
      assertEquals("A", order.get(0).getTeamName());
      assertEquals(Team.BYE, order.get(1));
      assertEquals("D", order.get(2).getTeamName());
      assertEquals("E", order.get(3).getTeamName());
      assertEquals("C", order.get(4).getTeamName());
      assertEquals("F", order.get(5).getTeamName());
      assertEquals(Team.BYE, order.get(6));
      assertEquals("B", order.get(7).getTeamName());

    } finally {
      SQLFunctions.close(connection);
    }
  }
}
