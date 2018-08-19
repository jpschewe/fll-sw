/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import fll.Team;
import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.ImportDB;
import fll.util.DummyTeamScore;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Some tests for unfinished playoff brackets.
 */
public class UnfinishedBracketTests {

  private static final int unfinishedTeamNumber = 352;

  private static final String[] finishedBracketNames = { "Lakes", "Woods" };

  // 352, 405, 407, 408
  private static final String unfinishedBracketName = "unfinished";

  // 1154, 3135, 3698, 3811
  private static final String unfinished3rdBracketName = "unfinished-3rd";

  // 4916, 4918, 5280, 7391
  private static final String tieBracketName = "tie";

  // 7393, 7684, 8330, 9975
  private static final String tie3rdBracketName = "tie-3rd";

  // 10484, 10486, 10719, 10721
  private static final String tieMiddleBracketName = "tie-middle";

  // 11221, 11228, 11229, 12911
  private static final String unfinished1st3rdBracketName = "unfinished-1st-3rd";

  // 14446, 16627, 17521, 18420
  private static final String tie1st3rdBracketName = "tie-1st-3rd";

  private static final String[] unfinishedBracketNames = { tie1st3rdBracketName, tie3rdBracketName, tieBracketName,
                                                           tieMiddleBracketName, unfinishedBracketName,
                                                           unfinished3rdBracketName, unfinished1st3rdBracketName };

  private static final String tournamentName = "12/13/15 - Rochester";

  /**
   * Common setup and tear down for the unfinished bracket tests.
   */
  public static class BaseTest {

    private File tempFile;

    private String database;

    protected Connection connection;

    protected Tournament tournament;

    @Before
    public void setup() throws IOException, SQLException {
      tempFile = File.createTempFile("flltest", null);
      database = tempFile.getAbsolutePath();
      connection = Utilities.createFileDataSource(database).getConnection();

      final InputStream dumpFileIS = UnfinishedBracketTests.class.getResourceAsStream("data/unfinished-bracket-tests.flldb");
      assertThat(dumpFileIS, notNullValue());

      final ImportDB.ImportResult importResult = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS),
                                                                                connection);
      TestUtils.deleteImportData(importResult);

      tournament = Tournament.findTournamentByName(connection, tournamentName);
      assertThat(tournament, notNullValue());
    }

    @After
    public void tearDown() {
      SQLFunctions.close(connection);

      if (null != tempFile
          && !tempFile.delete()) {
        tempFile.deleteOnExit();
      }
      if (null != database) {
        TestUtils.deleteDatabase(database);
      }
    }
  }

  /**
   * Test that {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)}
   * properly identifies unfinished brackets.
   */
  @RunWith(Theories.class)
  public static final class UnfinishedBrackets extends BaseTest {
    @DataPoints
    public static String[] names() {
      return unfinishedBracketNames;
    }

    /**
     * Test that the specified bracket is unfinished.
     * 
     * @param bracketName the bracket to check
     * @throws SQLException internal test error
     */
    @Theory
    public void test(final String bracketName) throws SQLException {
      final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
      assertThat(result, is(true));
    }
  }

  /**
   * Test that {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)}
   * properly identifies finished brackets.
   */
  @RunWith(Theories.class)
  public static final class FinishedBrackets extends BaseTest {
    @DataPoints
    public static String[] names() {
      return finishedBracketNames;
    }

    /**
     * Test that the specified bracket is finished.
     * 
     * @param bracketName the bracket to check
     * @throws SQLException internal test error
     */
    @Theory
    public void test(final String bracketName) throws SQLException {
      final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
      assertThat(result, is(false));
    }
  }

  /**
   * Test that {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)}
   * properly identifies ties.
   */
  @RunWith(Theories.class)
  public static final class TieBrackets extends BaseTest {
    @DataPoints
    public static String[] names() {
      return new String[] { tie1st3rdBracketName, tie3rdBracketName, tieBracketName, tieMiddleBracketName };
    }

    /**
     * Test that the specified bracket is noted as a tie.
     * 
     * @param bracketName the bracket to check
     * @throws SQLException internal test error
     */
    @Theory
    public void test(final String bracketName) throws SQLException {
      final Document document = GlobalParameters.getChallengeDocument(connection);
      assertThat(document, notNullValue());

      final ChallengeDescription challenge = new ChallengeDescription(document.getDocumentElement());

      // should get false for all ties
      final boolean result = Playoff.finishBracket(connection, challenge, tournament.getTournamentID(), bracketName);
      assertThat(result, is(false));
    }
  }

  /**
   * Test that
   * {@link Playoff#finishBracket(Connection, ChallengeDescription, int, String)}
   * will finish the unfinished brackets.
   */
  @RunWith(Theories.class)
  public static final class TestFinish extends BaseTest {
    @DataPoints
    public static String[] names() {
      return new String[] { unfinished3rdBracketName, unfinished1st3rdBracketName, unfinishedBracketName };
    }

    /**
     * Test that the specified bracket can be finished.
     * 
     * @param bracketName the bracket to check
     * @throws SQLException internal test error
     */
    @Theory
    public void test(final String bracketName) throws SQLException {
      final Document document = GlobalParameters.getChallengeDocument(connection);
      assertThat(document, notNullValue());

      final ChallengeDescription challenge = new ChallengeDescription(document.getDocumentElement());

      final boolean before = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
      assertThat(before, is(true));

      Playoff.finishBracket(connection, challenge, tournament.getTournamentID(), bracketName);

      final boolean after = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
      assertThat(after, is(false));
    }
  }

  /**
   * Non-parameterized tests.
   */
  public static final class StandardTests extends BaseTest {

    /**
     * Check that a team in an unfinished playoff bracket is properly noted.
     * 
     * @throws SQLException internal test error
     */
    @Test
    public void testTeamInUnfinished() throws SQLException {
      final Team team = Team.getTeamFromDatabase(connection, unfinishedTeamNumber);
      assertThat(team, notNullValue());

      final List<Integer> teamNumbers = Collections.singletonList(team.getTeamNumber());
      final String errors = Playoff.involvedInUnfinishedPlayoff(connection, tournament.getTournamentID(), teamNumbers);
      assertThat(errors, notNullValue());
    }

    /**
     * Check that {@link Playoff#getUnfinishedPlayoffBrackets(Connection, int)}
     * lists only the unfinished brackets.
     * 
     * @throws SQLException internal test error
     */
    @Test
    public void testUnfinishedByName() throws SQLException {
      final List<String> actual = Playoff.getUnfinishedPlayoffBrackets(connection, tournament.getTournamentID());

      assertThat(actual, hasSize(unfinishedBracketNames.length));

      for (final String bracketName : unfinishedBracketNames) {
        assertThat(bracketName, actual, hasItem(bracketName));
      }
    }

    /**
     * Check that the dummy score created by
     * {@link Playoff#populateInitialScoreMaps(ChallengeDescription, Map, Map)} can
     * be evaluated without error.
     * 
     * @throws RuntimeException internal test error
     * @throws SQLException internal test error
     */
    @Test
    public void testDummyScore() throws SQLException, RuntimeException {
      final Document document = GlobalParameters.getChallengeDocument(connection);
      assertThat(document, notNullValue());

      final ChallengeDescription challenge = new ChallengeDescription(document.getDocumentElement());

      final Map<String, Double> simpleGoals = new HashMap<>();
      final Map<String, String> enumGoals = new HashMap<>();

      Playoff.populateInitialScoreMaps(challenge, simpleGoals, enumGoals);

      final TeamScore teamScore = new DummyTeamScore(unfinishedTeamNumber, 1, simpleGoals, enumGoals);

      challenge.getPerformance().evaluate(teamScore);
    }

  }

}
