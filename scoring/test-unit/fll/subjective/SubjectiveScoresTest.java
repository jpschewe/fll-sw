/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;

import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.admin.DownloadSubjectiveData;
import fll.web.admin.UploadSubjectiveData;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ScoreCategory;
import fll.xml.XMLUtils;

/**
 * Test editing subjective scores.
 */
public class SubjectiveScoresTest {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  @Before
  public void setUp() {
    LogUtils.initializeLogging();

  }

  @After
  public void tearDown() {
  }

  /**
   * Try deleting scores and making sure the file can still be uploaded into a
   * database
   * 
   * @throws SQLException
   * @throws IOException
   * @throws ParseException 
   */
  @Test
  public void testDeleteScores() throws SAXException, SQLException, IOException, ParseException {
    // create database
    final InputStream stream = SubjectiveScoresTest.class.getResourceAsStream("challenge.xml");
    Assert.assertNotNull(stream);
    final Document challengeDocument = ChallengeParser.parse(new InputStreamReader(stream));
    Assert.assertNotNull(challengeDocument);
    
    final ChallengeDescription challenge = new ChallengeDescription(challengeDocument.getDocumentElement());

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    final DataSource datasource = Utilities.createFileDataSource(database);

    final String tournamentName = "test";
    final int teamNumber = 1;
    final String category = "teamwork";
    final String division= "div";
    Connection connection = null;
    PreparedStatement prep = null;
    try {
      connection = datasource.getConnection();

      // setup the database with a team and some judges 
      GenerateDB.generateDB(challengeDocument, connection, true);
      Queries.createTournament(connection, tournamentName, tournamentName);
      Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);
      Queries.addTeam(connection, teamNumber, "team" + teamNumber, "org", division, tournament.getTournamentID());      
      prep = connection.prepareStatement("INSERT INTO Judges (id, category, station, Tournament) VALUES(?, ?, ?, ?)");
      prep.setInt(4, tournament.getTournamentID());
      for (final Element subjectiveElement : new NodelistElementCollectionAdapter(
                                                                                  challengeDocument.getDocumentElement()
                                                                                                    .getElementsByTagName("subjectiveCategory"))) {
        final String categoryName = subjectiveElement.getAttribute("name");
        prep.setString(1, "jon");
        prep.setString(2, categoryName);
        prep.setString(3, division);
        prep.executeUpdate();
      }
      Queries.setCurrentTournament(connection, tournament.getTournamentID());


      // create subjective scores document
      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection);
      final Document scoreDocument = DownloadSubjectiveData.createSubjectiveScoresDocument(challenge,
                                                                                           tournamentTeams.values(),
                                                                                           connection, tournament.getTournamentID());
      StringWriter testWriter = new StringWriter();
      XMLUtils.writeXML(scoreDocument, testWriter, "UTF-8");
      LOGGER.info(testWriter.toString());
      
      ScoreCategory scoreCategory = null;
      for(final ScoreCategory sc : challenge.getSubjectiveCategories()) {
        if(category.equals(sc.getName())) {
          scoreCategory = sc;
        }
      }
      Assert.assertNotNull(scoreCategory);

      
      // create subjective table model for the category we're going to edit
      final SubjectiveTableModel tableModel = new SubjectiveTableModel(scoreDocument, scoreCategory);

      
      // enter scores for a team and category
      final int row = 0;
      for(int goalIdx=0; goalIdx < tableModel.getNumGoals(); ++goalIdx) {
        final int column = goalIdx + SubjectiveTableModel.NUM_COLUMNS_LEFT_OF_SCORES;
        tableModel.setValueAt(5, row, column);
      }

      
      // delete the scores for a team and category
      for(int goalIdx=0; goalIdx < tableModel.getNumGoals(); ++goalIdx) {
        final int column = goalIdx + SubjectiveTableModel.NUM_COLUMNS_LEFT_OF_SCORES;
        tableModel.setValueAt(null, row, column);
      }

            
      // upload the scores
      UploadSubjectiveData.saveSubjectiveData(scoreDocument, tournament.getTournamentID(), challenge, connection);

    } finally {
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);

      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }

      TestUtils.deleteDatabase(database);
    }

  }

}
