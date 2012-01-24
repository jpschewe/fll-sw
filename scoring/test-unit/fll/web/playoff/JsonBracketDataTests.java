/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.Gson;

import fll.Team;
import fll.Tournament;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.playoff.JsonBracketData.BracketLeafResultSet;
import fll.xml.ChallengeParser;

/**
 * Basic tests on the JsonBracketData object.
 * 
 * @author jjkoletar
 */
public class JsonBracketDataTests {
  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  @Test
  public void testRoundLimits() throws SQLException, ParseException, IOException, InstantiationException,
      ClassNotFoundException, IllegalAccessException {
    final PlayoffContainer playoff = makePlayoffs();
    final JsonBracketData jsonBrackets = new JsonBracketData(playoff.getBracketData());

    // shouldn't be able to access out of context rounds
    Map<Integer, Integer> query = new HashMap<Integer, Integer>();
    query.put(4, 1);
    Assert.assertEquals("{\"refresh\":\"true\"}",
                        jsonBrackets.getMultipleBracketLocationsJson(query,
                                                                     playoff.getConnection(),
                                                                     (Element) playoff.getChallengeDoc()
                                                                                      .getDocumentElement()
                                                                                      .getElementsByTagName("Performance")
                                                                                      .item(0)));
    // done
    SQLFunctions.close(playoff.getConnection());
  }

  /**
   * Test score repression, aka not showing unverified scores and not showing
   * finals info.
   */
  @Test
  public void testScoreRepression() throws SQLException, ParseException, IOException, InstantiationException,
      ClassNotFoundException, IllegalAccessException {
    final PlayoffContainer playoff = makePlayoffs();
    final JsonBracketData jsonBrackets = new JsonBracketData(playoff.getBracketData());

    // Start with adding some scores
    insertScore(playoff.getConnection(), 1, 1, false, 5D);
    // See what json tells us
    Map<Integer, Integer> query = new HashMap<Integer, Integer>();
    query.put(1, 1);
    final Gson gson = new Gson();
    final Element scoreElement = (Element) playoff.getChallengeDoc().getDocumentElement()
                                                  .getElementsByTagName("Performance").item(0);
    String jsonOut = jsonBrackets.getMultipleBracketLocationsJson(query, playoff.getConnection(), scoreElement);
    BracketLeafResultSet[] result = gson.fromJson(jsonOut, BracketLeafResultSet[].class);
    Assert.assertEquals(result[0].score, -1.0D, 0.0);
    // check unverified

    // give opponent a score
    insertScore(playoff.getConnection(), 2, 1, false, 20D);
    query.clear();
    query.put(3, 2);
    jsonOut = jsonBrackets.getMultipleBracketLocationsJson(query, playoff.getConnection(), scoreElement);
    result = gson.fromJson(jsonOut, BracketLeafResultSet[].class);
    Assert.assertEquals(result[0].leaf.getTeam().getTeamNumber(), Team.NULL_TEAM_NUMBER);

    // TODO: verify a score that has been entered as unverified and make sure we
    // get data from it

    // advance 1 team all the way to finals
    insertScore(playoff.getConnection(), 5, 2, true, 50D);
    insertScore(playoff.getConnection(), 6, 2, true, 10D);
    // score finals bit
    insertScore(playoff.getConnection(), 5, 3, true, 99D);
    // json shouldn't tell us the score for the finals round
    query.clear();
    query.put(23, 3);
    jsonOut = jsonBrackets.getMultipleBracketLocationsJson(query, playoff.getConnection(), scoreElement);
    result = gson.fromJson(jsonOut, BracketLeafResultSet[].class);
    Assert.assertEquals(result[0].score, -1.0D, 0.0);

    SQLFunctions.close(playoff.getConnection());
  }

  private void insertScore(Connection connection,
                           int team,
                           int run,
                           boolean verified,
                           double score) throws SQLException {
    PreparedStatement ps = null;
    try {
      ps = connection.prepareStatement("INSERT INTO Performance (TeamNumber, Tournament, RunNumber, NoShow, Bye, Verified, Score, ComputedTotal)"
          + " VALUES (?, 2, ?, false, false, ?, ?, ?)");
      ps.setInt(1, team);
      ps.setInt(2, run);
      ps.setBoolean(3, verified);
      ps.setDouble(4, score);
      ps.setDouble(5, score);
      Assert.assertEquals(1, ps.executeUpdate());
    } finally {
      SQLFunctions.close(ps);
    }
  }

  private class PlayoffContainer {
    private Connection c = null;

    private BracketData bd = null;

    private Document challengeDoc = null;

    public PlayoffContainer(Connection con,
                            BracketData brackets,
                            Document doc) {
      c = con;
      bd = brackets;
      challengeDoc = doc;
    }

    public Connection getConnection() {
      return c;
    }

    public BracketData getBracketData() {
      return bd;
    }

    public Document getChallengeDoc() {
      return challengeDoc;
    }

  }

  private PlayoffContainer makePlayoffs() throws IllegalAccessException, SQLException, ClassNotFoundException,
      InstantiationException, ParseException, IOException {
    final String div = "1";
    final String[] teamNames = new String[] { "A", "B", "C", "D", "E", "F" };
    Connection connection = null;
    // load up basic descriptor
    final InputStream challengeDocIS = JsonBracketDataTests.class.getResourceAsStream("data/basic-brackets-json.xml");
    Assert.assertNotNull(challengeDocIS);
    final Document document = ChallengeParser.parse(new InputStreamReader(challengeDocIS));
    Assert.assertNotNull(document);

    // in-memory db instance
    Class.forName("org.hsqldb.jdbcDriver").newInstance();
    connection = DriverManager.getConnection("jdbc:hsqldb:mem:flldb-testJsonBrackets");
    GenerateDB.generateDB(document, connection, true);

    Tournament.createTournament(connection, "Playoff Test Tournament", "Test");
    Queries.setCurrentTournament(connection, 2); // 2 is tournament ID
    Queries.setNumSeedingRounds(connection, 2, 0);
    // make teams
    for (int i = 0; i < teamNames.length; ++i) {
      Assert.assertNull(Queries.addTeam(connection, i + 1, teamNames[i], "htk", "mn", div, 2));
    }
    Playoff.initializeBrackets(connection, document, div, false);

    final int firstRound = 1;
    final int lastRound = 3;
    final int rowsPerTeam = 4;
    final boolean showFinals = false;
    final boolean onlyVerifiedScores = true;
    return new PlayoffContainer(connection, new BracketData(connection, div, firstRound, lastRound, rowsPerTeam,
                                                            showFinals, onlyVerifiedScores), document);

  }
}
