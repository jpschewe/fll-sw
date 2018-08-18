/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.zip.ZipInputStream;

import org.junit.Test;

import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.db.ImportDB;

/**
 * Some tests for {@link Playoff}.
 */
public class PlayoffTest {

  /**
   * Check that a playoff bracket that ends in a tie is considered unfinished.
   * 
   * @throws IOException internal test error
   * @throws SQLException internal test error
   */
  @Test
  public void testFinalTieUnfinished() throws IOException, SQLException {
    final InputStream dumpFileIS = PlayoffTest.class.getResourceAsStream("data/tie-playoff-bracket.flldb");
    assertThat(dumpFileIS, notNullValue());

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    try (Connection connection = Utilities.createFileDataSource(database).getConnection()) {
      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), connection);

      final String bracketName = "Lakes"; // must match data in the database
      final String tournamentName = "Sample8"; // must match data in the database
      final Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);

      final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
      assertThat("Final tie should leave bracket unfinished", result, is(true));
    } finally {
      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
    }
    TestUtils.deleteDatabase(database);
  }

  /**
   * Check that a a playoff bracket that isn't finished is noted as such.
   * 
   * @throws IOException internal test error
   * @throws SQLException internal test error
   */
  @Test
  public void testUnfinished() throws IOException, SQLException {
    final InputStream dumpFileIS = PlayoffTest.class.getResourceAsStream("data/partial-playoff-bracket.flldb");
    assertThat(dumpFileIS, notNullValue());

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    try (Connection connection = Utilities.createFileDataSource(database).getConnection()) {
      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), connection);

      final String bracketName = "Lakes"; // must match data in the database
      final String tournamentName = "Sample8"; // must match data in the database
      final Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);

      final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
      assertThat("Bracket is unfinished and should be noted as such", result, is(true));
    } finally {
      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
    }
    TestUtils.deleteDatabase(database);
  }

}
