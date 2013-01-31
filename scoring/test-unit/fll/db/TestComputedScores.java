/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.util.LogUtils;
import fll.web.playoff.DatabaseTeamScore;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;

/**
 * 
 */
public class TestComputedScores {

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Check the score computation that was a problem in 2009 where 0 != 0.
   * 
   * @param args
   * @throws SQLException
   * @throws ParseException
   * @throws IOException
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
      Assert.assertNotNull("Cannot find test data", dumpFileIS);

      connection = Utilities.createFileDataSource(database).getConnection();

      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), connection);

      final Document document = GlobalParameters.getChallengeDocument(connection);
      final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());
      final PerformanceScoreCategory performanceElement = description.getPerformance();

      final Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);
      final int tournamentID = tournament.getTournamentID();
      selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? and TeamNumber = ? AND RunNumber = ?");
      selectPrep.setInt(1, tournamentID);
      selectPrep.setInt(2, teamNumber);
      selectPrep.setInt(3, runNumber);
      rs = selectPrep.executeQuery();
      Assert.assertNotNull("Error getting performance scores", rs);
      Assert.assertTrue("No scores found", rs.next());

      final double computedTotal = performanceElement.evaluate(new DatabaseTeamScore(teamNumber, runNumber, rs));
      Assert.assertEquals(expectedTotal, computedTotal, 0D);

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
