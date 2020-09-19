/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.web.playoff.DatabaseTeamScore;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 *
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class TestComputedScores {

  /**
   * Check the score computation that was a problem in 2009 where 0 != 0.
   *
   * @throws SQLException test error
   * @throws ParseException test error
   * @throws IOException test error
   */
  @Test
  public void testFPComputation() throws SQLException, ParseException, IOException {
    final int teamNumber = 327;
    final int runNumber = 1;
    final String tournamentName = "11-21 Plymouth Middle";
    final double expectedTotal = 295;

    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    Connection connection = null;

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    try {
      final InputStream dumpFileIS = TestComputedScores.class.getResourceAsStream("data/plymouth-2009-11-21.zip");
      assertNotNull(dumpFileIS, "Cannot find test data");

      connection = Utilities.createFileDataSource(database).getConnection();

      final ImportDB.ImportResult importResult = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS),
                                                                                connection);
      TestUtils.deleteImportData(importResult);

      final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);
      final PerformanceScoreCategory performanceElement = description.getPerformance();

      final Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);
      final int tournamentID = tournament.getTournamentID();
      selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? and TeamNumber = ? AND RunNumber = ?");
      selectPrep.setInt(1, tournamentID);
      selectPrep.setInt(2, teamNumber);
      selectPrep.setInt(3, runNumber);
      rs = selectPrep.executeQuery();
      assertNotNull(rs, "Error getting performance scores");
      assertTrue(rs.next(), "No scores found");

      try (DatabaseTeamScore score = new DatabaseTeamScore(teamNumber, runNumber, rs)) {
        final double computedTotal = performanceElement.evaluate(score);
        assertEquals(expectedTotal, computedTotal, 0D);
      }

    } finally {
      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
      SQLFunctions.close(rs);
      SQLFunctions.close(selectPrep);
      SQLFunctions.close(connection);
      TestUtils.deleteDatabase(database);
    }

  }
}
