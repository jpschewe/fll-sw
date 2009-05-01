/*
 * Copyright (c) 2008
 *      Jon Schewe.  All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * I'd appreciate comments/suggestions on the code jpschewe@mtu.net
 */
package fll.web.playoff;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.Team;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.xml.BracketSortType;
import fll.xml.ChallengeParser;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

/**
 * Test the various playoff bracket sort methods.
 * 
 * @author jpschewe
 */
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
  public void testAlphaTeam() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, UnsupportedEncodingException {
    final String divisionStr = "1";
    final String[] teamNames = new String[] { "A", "B", "C", "D", "E", "F" };

    Connection connection = null;
    try {
      // load in data/alpha-team-sort.xml
      final InputStream challengeDocIS = BracketSortTest.class.getResourceAsStream("data/alpha-team-sort.xml");
      final Document document = ChallengeParser.parse(new InputStreamReader(challengeDocIS));

      // create in memory test database instance
      Class.forName("org.hsqldb.jdbcDriver").newInstance();
      connection = DriverManager.getConnection("jdbc:hsqldb:mem:flldb-testAlphaTeam");
      GenerateDB.generateDB(document, connection, true);

      // put some teams in the database
      for (int i = 0; i < teamNames.length; ++i) {
        final String otherTeam = Queries.addTeam(connection, teamNames.length
            - i, teamNames[i], null, "DUMMY", divisionStr);
        Assert.assertNull(otherTeam);
      }
      final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);

      final BracketSortType bracketSort = XMLUtils.getBracketSort(document);
      final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(document);

      final List<Team> order = Playoff.buildInitialBracketOrder(connection, bracketSort, winnerCriteria, divisionStr, tournamentTeams);
      Assert.assertEquals("A", order.get(0).getTeamName());
      Assert.assertEquals("B", order.get(1).getTeamName());
      Assert.assertEquals("C", order.get(2).getTeamName());
      Assert.assertEquals("D", order.get(3).getTeamName());
      Assert.assertEquals("E", order.get(4).getTeamName());
      Assert.assertEquals(Team.BYE, order.get(5));
      Assert.assertEquals("F", order.get(6).getTeamName());
      Assert.assertEquals(Team.BYE, order.get(7));

    } finally {
      SQLFunctions.closeConnection(connection);
    }
  }

}
