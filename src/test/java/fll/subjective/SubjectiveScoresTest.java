/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;

import fll.web.admin.DownloadSubjectiveData;
import fll.web.admin.UploadSubjectiveData;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.SubjectiveScoreCategory;
import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Test editing subjective scores.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class SubjectiveScoresTest {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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
    assertNotNull(stream);
    final Document challengeDocument = ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    assertNotNull(challengeDocument);

    final ChallengeDescription challenge = new ChallengeDescription(challengeDocument.getDocumentElement());

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    final DataSource datasource = Utilities.createFileDataSource(database);

    final String tournamentName = "test";
    final int teamNumber = 1;
    final String category = "teamwork";
    final String division = "div";
    Connection connection = null;
    PreparedStatement prep = null;
    try {
      connection = datasource.getConnection();

      // setup the database with a team and some judges
      GenerateDB.generateDB(challengeDocument, connection);
      Tournament.createTournament(connection, tournamentName, tournamentName, null);
      Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);
      assertNull(Queries.addTeam(connection, teamNumber, "team"
          + teamNumber, "org"));
      Queries.addTeamToTournament(connection, teamNumber, tournament.getTournamentID(), division, division);

      prep = connection.prepareStatement("INSERT INTO Judges (id, category, station, Tournament) VALUES(?, ?, ?, ?)");
      prep.setInt(4, tournament.getTournamentID());
      for (final Element subjectiveElement : new NodelistElementCollectionAdapter(challengeDocument.getDocumentElement()
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
                                                                                           connection,
                                                                                           tournament.getTournamentID());
      StringWriter testWriter = new StringWriter();
      XMLUtils.writeXML(scoreDocument, testWriter, Utilities.DEFAULT_CHARSET.name());
      LOGGER.info(testWriter.toString());

      SubjectiveScoreCategory scoreCategory = null;
      for (final SubjectiveScoreCategory sc : challenge.getSubjectiveCategories()) {
        if (category.equals(sc.getName())) {
          scoreCategory = sc;
        }
      }
      assertNotNull(scoreCategory);

      // create subjective table model for the category we're going to edit
      final SubjectiveTableModel tableModel = new SubjectiveTableModel(scoreDocument, scoreCategory, null, null);

      // enter scores for a team and category
      final int row = 0;
      for (int goalIdx = 0; goalIdx < tableModel.getNumGoals(); ++goalIdx) {
        final int column = goalIdx
            + tableModel.getNumColumnsLeftOfScores();
        tableModel.setValueAt(5, row, column);
      }

      // delete the scores for a team and category
      for (int goalIdx = 0; goalIdx < tableModel.getNumGoals(); ++goalIdx) {
        final int column = goalIdx
            + tableModel.getNumColumnsLeftOfScores();
        tableModel.setValueAt(null, row, column);
      }

      // upload the scores
      UploadSubjectiveData.saveSubjectiveData(scoreDocument, tournament.getTournamentID(), challenge, connection,
                                              false);

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
