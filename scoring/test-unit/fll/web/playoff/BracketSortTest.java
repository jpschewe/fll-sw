/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.Team;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.xml.BracketSortType;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.WinnerType;

/**
 * Test the various playoff bracket sort methods.
 * 
 * @author jpschewe
 */
public class BracketSortTest {

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

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
  public void testAlphaTeam() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, UnsupportedEncodingException {
    final String divisionStr = "1";
    final String[] teamNames = new String[] { "A", "B", "C", "D", "E", "F" };
    Connection connection = null;
    try {
      // load in data/alpha-team-sort.xml
      final InputStream challengeDocIS = BracketSortTest.class.getResourceAsStream("data/alpha-team-sort.xml");
      Assert.assertNotNull(challengeDocIS);
      final Document document = ChallengeParser.parse(new InputStreamReader(challengeDocIS));
      Assert.assertNotNull(document);

      final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());
      
      // create in memory test database instance
      Class.forName("org.hsqldb.jdbcDriver").newInstance();
      connection = DriverManager.getConnection("jdbc:hsqldb:mem:flldb-testAlphaTeam");
      GenerateDB.generateDB(document, connection, true);

      // put some teams in the database
      // final Map<Integer, Team> tournamentTeams = new HashMap<Integer,
      // Team>();
      for (int i = 0; i < teamNames.length; ++i) {
        final String otherTeam = Queries.addTeam(connection, teamNames.length
            - i, teamNames[i], null, divisionStr);
        Assert.assertNull(otherTeam);
        // final Team team = new Team();
        // team.setDivision(divisionStr);
        // team.setTeamName(teamNames[i]);
        // team.setTeamNumber(teamNames.length
        // - i);
        // tournamentTeams.put(team.getTeamNumber(), team);
      }

      final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);

      final BracketSortType bracketSort = description.getBracketSort();
      final WinnerType winnerCriteria = description.getWinner();

      final List<Team> teams = new ArrayList<Team>(tournamentTeams.values());
      Team.filterTeamsToEventDivision(connection, teams, divisionStr);

      final List<Team> order = Playoff.buildInitialBracketOrder(connection, bracketSort, winnerCriteria, teams);
      Assert.assertEquals("A", order.get(0).getTeamName());
      Assert.assertEquals(Team.BYE, order.get(1));
      Assert.assertEquals("D", order.get(2).getTeamName());
      Assert.assertEquals("E", order.get(3).getTeamName());
      Assert.assertEquals("C", order.get(4).getTeamName());
      Assert.assertEquals("F", order.get(5).getTeamName());
      Assert.assertEquals(Team.BYE, order.get(6));
      Assert.assertEquals("B", order.get(7).getTeamName());

    } finally {
      SQLFunctions.close(connection);
    }
  }
}
