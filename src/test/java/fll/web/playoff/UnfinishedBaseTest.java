/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.db.ImportDB;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Common setup and tear down for the unfinished bracket tests.
 */
public abstract class UnfinishedBaseTest {

  private File tempFile;

  private String database;

  protected Connection connection;

  protected Tournament tournament;

  /** bracket that has multiple missing entries */
  // 19689, 20806, 21169, 21940
  /* package */ static final String unfinishedLarge = "unfinished-large";

  // 14446, 16627, 17521, 18420
  /* package */ static final String tie1st3rdBracketName = "tie-1st-3rd";

  // 11221, 11228, 11229, 12911
  /* package */ static final String unfinished1st3rdBracketName = "unfinished-1st-3rd";

  // 10484, 10486, 10719, 10721
  /* package */ static final String tieMiddleBracketName = "tie-middle";

  // 7393, 7684, 8330, 9975
  /* package */ static final String tie3rdBracketName = "tie-3rd";

  // 4916, 4918, 5280, 7391
  /* package */ static final String tieBracketName = "tie";

  // 1154, 3135, 3698, 3811
  /* package */ static final String unfinished3rdBracketName = "unfinished-3rd";

  // 352, 405, 407, 408
  /* package */ static final String unfinishedBracketName = "unfinished";

  /* package */ static final String[] finishedBracketNames = { "Lakes", "Woods" };

  /* package */ static final int unfinishedTeamNumber = 352;

  /* package */ static final String tournamentName = "12/13/15 - Rochester";

  /* package */ static final String[] unfinishedBracketNames = { tie1st3rdBracketName, tie3rdBracketName,
                                                                 tieBracketName, tieMiddleBracketName,
                                                                 unfinishedBracketName, unfinished3rdBracketName,
                                                                 unfinished1st3rdBracketName, unfinishedLarge };

  @BeforeEach
  public void setUp() throws IOException, SQLException {
    tempFile = File.createTempFile("flltest", null);
    database = tempFile.getAbsolutePath();
    connection = Utilities.createFileDataSource(database).getConnection();

    final InputStream dumpFileIS = UnfinishedBaseTest.class.getResourceAsStream("data/unfinished-bracket-tests.flldb");
    assertThat(dumpFileIS, notNullValue());

    final ImportDB.ImportResult importResult = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS),
                                                                              connection);
    TestUtils.deleteImportData(importResult);

    tournament = Tournament.findTournamentByName(connection, UnfinishedBaseTest.tournamentName);
    assertThat(tournament, notNullValue());
  }

  @AfterEach
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