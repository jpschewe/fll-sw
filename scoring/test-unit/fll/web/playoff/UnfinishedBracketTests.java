/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.Matchers.contains;
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
import java.util.List;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fll.Team;
import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.db.ImportDB;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Some tests for unfinished playoff brackets.
 */
public class UnfinishedBracketTests {

  private static final int unfinishedTeamNumber = 101;

  private static final String finishedBracketName = "Lakes";

  private static final String unfinishedBracketName = "unfinished";

  private static final String tieBracketName = "tie";

  private static final String tournamentName = "Sample8";

  private File tempFile;

  private String database;

  private Connection connection;

  private Tournament tournament;

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

  /**
   * Check that a playoff bracket that ends in a tie is considered unfinished.
   * 
   * @throws SQLException internal test error
   */
  @Test
  public void testFinalTieUnfinished() throws SQLException {
    final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), tieBracketName);
    assertThat("Final tie should leave bracket unfinished", result, is(true));
  }

  /**
   * Check that
   * {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)} shows
   * unfinished for the unfinished bracket.
   * 
   * @throws SQLException internal test error
   */
  @Test
  public void testUnfinished() throws SQLException {
    final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(),
                                                              unfinishedBracketName);
    assertThat(result, is(true));
  }

  /**
   * Check that
   * {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)} shows
   * finished for the completed bracket.
   * 
   * @throws SQLException internal test error
   */
  @Test
  public void testFinished() throws SQLException {
    final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(),
                                                              finishedBracketName);
    assertThat(result, is(false));
  }

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

    assertThat(actual, hasSize(2));
    assertThat(actual, hasItem(unfinishedBracketName));
    assertThat(actual, hasItem(tieBracketName));
  }

}
