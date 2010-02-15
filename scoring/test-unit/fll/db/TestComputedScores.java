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

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.TestUtils;
import fll.Utilities;
import fll.util.ScoreUtils;
import fll.web.playoff.DatabaseTeamScore;

/**
 * 
 */
public class TestComputedScores {

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
    final String tournament = "11-21 Plymouth Middle";
    final double expectedTotal = 295;
    
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    Connection connection = null;

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    try {
      final InputStream dumpFileIS = TestComputedScores.class.getResourceAsStream("data/plymouth-2009-11-21.zip");
      Assert.assertNotNull("Cannot find test data", dumpFileIS);
      

      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), database);

      connection = Utilities.createDataSource(database).getConnection();
      
      final Document document = Queries.getChallengeDocument(connection);
      final Element rootElement = document.getDocumentElement();
      final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

      final int tournamentID = Queries.getTournamentID(connection, tournament);
      selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? and TeamNumber = ? AND RunNumber = ?");
      selectPrep.setInt(1, tournamentID);
      selectPrep.setInt(2, teamNumber);
      selectPrep.setInt(3, runNumber);
      rs = selectPrep.executeQuery();
      Assert.assertNotNull(rs);
      Assert.assertTrue(rs.next());
      
      final double computedTotal = ScoreUtils.computeTotalScore(new DatabaseTeamScore(performanceElement, teamNumber, runNumber, rs));
      Assert.assertEquals(expectedTotal, computedTotal, 0D);
      
    } finally {
      if(!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
      TestUtils.deleteDatabase(database);
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(selectPrep);   
      SQLFunctions.closeConnection(connection);
    }

  }
}
