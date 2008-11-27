/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.List;

import javax.swing.table.TableModel;

import junit.framework.Assert;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.meterware.httpunit.Button;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HTMLElement;
import com.meterware.httpunit.HTMLElementPredicate;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;

import fll.TestUtils;
import fll.Utilities;
import fll.gui.SubjectiveFrame;
import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

/**
 * Do full tournament tests.
 * 
 * @version $Revision$
 */
public class FullTournamentTest {

  private static final Logger LOG = Logger.getLogger(FullTournamentTest.class);

  /**
   * Test a full tournament as a single thread. This tests to make sure
   * everything works normally.
   */
  @Test
  public void testSerial() throws MalformedURLException, IOException, SAXException, ClassNotFoundException, InstantiationException, IllegalAccessException,
      ParseException, SQLException {
    if (LOG.isInfoEnabled()) {
      LOG.info("Starting serial test");
    }
    doFullTournament(false);
  }

  /**
   * Test a full tournament as multiple threads. This is a more of a stress
   * test.
   */
  @Test
  public void testParallel() throws MalformedURLException, IOException, SAXException, ClassNotFoundException, InstantiationException, IllegalAccessException,
      ParseException, SQLException {
    if (LOG.isInfoEnabled()) {
      LOG.info("Starting parallel test");
    }
    doFullTournament(true);
  }

  /**
   * Run a full tournament.
   * 
   * @param parallel if true run many of the tasks in parallel
   * @throws MalformedURLException
   * @throws IOException
   * @throws SAXException
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws ParseException
   * @throws SQLException
   */
  private void doFullTournament(final boolean parallel) throws MalformedURLException, IOException, SAXException, ClassNotFoundException,
      InstantiationException, IllegalAccessException, ParseException, SQLException {
    final int numSeedingRounds = 3;

    // create connection to database with test data
    Class.forName("org.hsqldb.jdbcDriver").newInstance();
    Connection testDataConn = null;
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      testDataConn = DriverManager.getConnection("jdbc:hsqldb:res:/fll/web/data/flldb-ft");
      final String testTournament = "Field";
      Assert.assertNotNull("Error connecting to test data database", testDataConn);

      stmt = testDataConn.createStatement();

      // --- initialize database ---
      final WebConversation conversation = new WebConversation();
      WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "setup/");
      WebResponse response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

      WebForm form = response.getFormWithID("setup");
      Assert.assertNotNull(form);
      form.setCheckbox("force_rebuild", true); // rebuild the whole database
      final InputStream challengeDocIS = FullTournamentTest.class.getResourceAsStream("data/challenge-ft.xml");
      Assert.assertNotNull(challengeDocIS);
      final UploadFileSpec challengeUpload = new UploadFileSpec("challenge-test.xml", challengeDocIS, "text/xml");
      Assert.assertNotNull(challengeUpload);
      form.setParameter("xmldocument", new UploadFileSpec[] {challengeUpload});
      request = form.getRequest("reinitializeDatabase");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull("Error initializing database: " + response.getText(), response.getElementWithID("success"));
      challengeDocIS.close();

      // --- load teams ---
      // find upload form on admin page
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithID("uploadTeams");

      // set the parameters
      final InputStream teamsIS = FullTournamentTest.class.getResourceAsStream("data/teams-ft.csv");
      Assert.assertNotNull(teamsIS);
      final UploadFileSpec teamsUpload = new UploadFileSpec("teams.csv", teamsIS, "text/plain");
      form.setParameter("teamsFile", new UploadFileSpec[] { teamsUpload });
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      // Assert.assertNotNull("Error uploading data file: " +
      // response.getText(), response.getElementWithID("success"));
      teamsIS.close();

      // skip past the filter page
      form = response.getFormWithName("filterTeams");
      request = form.getRequest("next");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());

      // team column selection
      form = response.getFormWithName("verifyTeams");
      Assert.assertNotNull(form);
      form.setParameter("TeamNumber", "tea_number");
      form.setParameter("TeamName", "tea_name");
      form.setParameter("Organization", "org_name");
      form.setParameter("Region", "eve_name");
      form.setParameter("Division", "div_name");
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull("Error loading teams: " + response.getText(), response.getElementWithID("success"));

      // create tournaments for all regions
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/index.jsp");
      request.setParameter("addTournamentsForRegions", "1");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully added tournaments for regions"));

      // initialize tournaments by region
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/tournamentInitialization.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("form");
      Assert.assertNotNull(form);
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      form = response.getFormWithName("verify");
      Assert.assertNotNull(form);
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully initialized tournament for teams based on region."));

      // set the tournamet
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/index.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithID("currentTournament");
      Assert.assertNotNull(form);
      form.setParameter("currentTournament", testTournament);
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      Assert.assertNotNull("Error loading teams: " + response.getText(), response.getElementWithID("success"));
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Tournament changed to " + testTournament));

      // assign judges
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/judges.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("judges");
      Assert.assertNotNull(form);

      // need to add rows to form if test database has more judges than
      // categories
      prep = testDataConn.prepareStatement("SELECT COUNT(id) FROM Judges WHERE Tournament = ?");
      prep.setString(1, testTournament);
      rs = prep.executeQuery();
      Assert.assertTrue("Could not find judges information in test data", rs.next());
      final int numJudges = rs.getInt(1);
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
      while (!form.hasParameterNamed("id" + String.valueOf(numJudges - 1))) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Adding a row to the judges entry form");
        }
        request = form.getRequest("submit", "Add Row");
        response = conversation.getResponse(request);
        Assert.assertTrue(response.isHTML());
        form = response.getFormWithName("judges");
        Assert.assertNotNull(form);
      }

      // assign judges from database
      int judgeIndex = 0;
      prep = testDataConn.prepareStatement("SELECT id, category, Division FROM Judges WHERE Tournament = ?");
      prep.setString(1, testTournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String id = rs.getString(1);
        final String category = rs.getString(2);
        final String division = rs.getString(3);
        form.setParameter("id" + judgeIndex, id);
        form.setParameter("cat" + judgeIndex, category);
        form.setParameter("div" + judgeIndex, division);
        ++judgeIndex;
      }
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);

      // submit those values
      request = form.getRequest("finished", "Finished");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNull("Got error from judges assignment", response.getElementWithID("error"));

      // commit judges information
      form = response.getFormWithName("judges");
      Assert.assertNotNull(form);
      request = form.getRequest("commit", "Commit");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully assigned judges."));

      // assign table labels
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/tables.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("tables");
      Assert.assertNotNull(form);
      form.setParameter("SideA0", "red");
      form.setParameter("SideB0", "blue");
      request = form.getRequest("submit", "Finished");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNull("Got error from judges assignment", response.getElementWithID("error"));
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully assigned tables."));

      /*
       * --- Enter 3 runs for each team --- Use data from test data base,
       * converted from Field 2005. Enter 4th run and rest of playoffs
       */
      prep = testDataConn.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?");
      prep.setString(1, testTournament);
      rs = prep.executeQuery();
      Assert.assertTrue("No performance scores in test data", rs.next());
      final int maxRuns = rs.getInt(1);
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);

      final Document challengeDocument = ChallengeParser.parse(new InputStreamReader(FullTournamentTest.class.getResourceAsStream("data/challenge-ft.xml")));
      Assert.assertNotNull(challengeDocument);
      final Element rootElement = challengeDocument.getDocumentElement();
      final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

      prep = testDataConn.prepareStatement("SELECT TeamNumber FROM Performance WHERE Tournament = ? AND RunNumber = ?");
      boolean initializedPlayoff = false;
      prep.setString(1, testTournament);
      final ScoreEntryQueue scoreEntryQueue = new ScoreEntryQueue(parallel ? 4 : 1, testDataConn, performanceElement, testTournament);
      for (int runNumber = 1; runNumber <= maxRuns; ++runNumber) {
        if (!initializedPlayoff && runNumber > numSeedingRounds) {
          // TODO make sure to check the result of checking the seeding rounds

          // initialize the playoff brackets with playoff/index.jsp form
          request = new GetMethodWebRequest(TestUtils.URL_ROOT + "playoff");
          response = conversation.getResponse(request);
          Assert.assertTrue(response.isHTML());
          form = response.getFormWithName("initialize");
          Assert.assertNotNull(form);
          final String[] divisions = form.getOptionValues("division");
          for (int divIdx = 0; divIdx < divisions.length; ++divIdx) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Initializing playoff brackets for division " + divisions[divIdx]);
            }
            form.setParameter("division", divisions[divIdx]);
            request = form.getRequest();
            response = conversation.getResponse(request);
            Assert.assertTrue(response.isHTML());
          }
          initializedPlayoff = true;

        }

        prep.setInt(2, runNumber);
        rs = prep.executeQuery();

        // for each score in a run
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          enterPerformanceScore(testDataConn, performanceElement, testTournament, runNumber, teamNumber);
        }
        LOG.info("Waiting for queue to finish");
        scoreEntryQueue.waitForQueueToFinish();
      }
      scoreEntryQueue.shutdown();

      enterSubjectiveScores(testDataConn, challengeDocument, testTournament);

      // compute final scores
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "report/summarizePhase1.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "report/summarizePhase2.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully summarized scores"));

      // generate reports
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "report/categorizedScores.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "report/categoryScoresByJudge.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "report/CategoryScoresByScoreGroup");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());

      // PDF reports
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "GetFile");
      request.setParameter("filename", "finalComputedScores.pdf");
      response = conversation.getResponse(request);
      Assert.assertEquals("application/pdf", response.getContentType());

      // check ranking
      // final Connection connection = TestUtils.createDBConnection();
      // Assert.assertNotNull("Could not create test database connection",
      // connection);

      // division 1
      final int[] division1ExpectedRank = { 2636, 3127, 3439, 4462, 3125, 2116, 2104, 2113 };
      String division = "DivI/Gr4-6";
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "developer/query.jsp");
      request.setParameter("query", "SELECT FinalScores.TeamNumber" + " FROM FinalScores, current_tournament_teams"
          + " WHERE FinalScores.TeamNumber = current_tournament_teams.TeamNumber" + " AND FinalScores.Tournament = '" + testTournament + "'"
          + " AND current_tournament_teams.event_division = '" + division + "'" + " ORDER BY FinalScores.OverallScore DESC");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      WebTable table = response.getTableWithID("queryResult");
      for (int rank = 0; rank < division1ExpectedRank.length; ++rank) {
        final String expectedTeamNumberStr = String.valueOf(division1ExpectedRank[rank]);
        Assert.assertEquals("Ranking is incorrect", expectedTeamNumberStr, table.getCellAsText(rank + 1, 0));
      }

      // division 2
      final int[] division2ExpectedRank = { 3208, 3061, 2863, 2110, 3063, 353, 2043, 3129 };
      division = "DivII/Gr7-9";
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "developer/query.jsp");
      request.setParameter("query", "SELECT FinalScores.TeamNumber" + " FROM FinalScores, current_tournament_teams"
          + " WHERE FinalScores.TeamNumber = current_tournament_teams.TeamNumber" + " AND FinalScores.Tournament = '" + testTournament + "'"
          + " AND current_tournament_teams.event_division = '" + division + "'" + " ORDER BY FinalScores.OverallScore DESC");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      table = response.getTableWithID("queryResult");
      for (int rank = 0; rank < division2ExpectedRank.length; ++rank) {
        final String expectedTeamNumberStr = String.valueOf(division2ExpectedRank[rank]);
        Assert.assertEquals("Ranking is incorrect", expectedTeamNumberStr, table.getCellAsText(rank + 1, 0));
      }

      /*
       * prep = connection.prepareStatement("SELECT FinalScores.TeamNumber FROM
       * FinalScores, current_tournament_teams WHERE FinalScores.TeamNumber =
       * current_tournament_teams.TeamNumber AND FinalScores.Tournament = ? AND
       * current_tournament_teams.event_division = ? ORDER BY FinalScores.OverallScore
       * DESC"); prep.setString(1, testTournament); // division 1
       * prep.setString(2, "DivI/Gr4-6"); rs = prep.executeQuery(); int rank =
       * 0; while(rs.next()) { final int teamNumber = rs.getInt(1);
       * Assert.assertEquals("Ranking is incorrect",
       * division1ExpectedRank[rank], teamNumber); }
       * SQLFunctions.closeResultSet(rs); // division2 prep.setString(2,
       * "DivII/Gr7-9"); rs = prep.executeQuery(); rank = 0; while(rs.next()) {
       * final int teamNumber = rs.getInt(1); Assert.assertEquals("Ranking is
       * incorrect", division2ExpectedRank[rank], teamNumber); }
       * SQLFunctions.closeResultSet(rs); SQLFunctions.closePreparedStatement(prep);
       */

      // TODO check scores?
      
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closePreparedStatement(prep);
      SQLFunctions.closeConnection(testDataConn);
      // Utilities.closeConnection(connection);
    }
  }

  /**
   * Simulate entering subjective scores by pulling them out of testDataConn.
   * 
   * @param testDataConn Where to get the test data from
   * @param challengeDocument the challenge descriptor
   * @param testTournament the name of the tournament to enter scores for
   * @throws SQLException
   */
  private void enterSubjectiveScores(final Connection testDataConn, final Document challengeDocument, final String testTournament) throws SQLException,
      IOException, MalformedURLException, SAXException, ParseException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      final WebConversation conversation = new WebConversation();

      // download subjective zip
      WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "GetFile");
      request.setParameter("filename", "subjective-data.zip");
      WebResponse response = conversation.getResponse(request);
      Assert.assertEquals("application/zip", response.getContentType());
      final InputStream zipStream = response.getInputStream();
      final File subjectiveZip = File.createTempFile("fll", "zip");
      subjectiveZip.deleteOnExit();
      final FileOutputStream outputStream = new FileOutputStream(subjectiveZip);
      final byte[] buffer = new byte[512];
      int bytesRead = 0;
      while (-1 != (bytesRead = zipStream.read(buffer))) {
        outputStream.write(buffer, 0, bytesRead);
      }
      outputStream.close();
      zipStream.close();
      final SubjectiveFrame subjective = new SubjectiveFrame(subjectiveZip);

      // insert scores into zip
      for(final Element subjectiveElement : XMLUtils.filterToElements(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
        final String category = subjectiveElement.getAttribute("name");
        final String title = subjectiveElement.getAttribute("title");
        // find appropriate table model
        final TableModel tableModel = subjective.getTableModelForTitle(title);

        final List<Element> goals = XMLUtils.filterToElements(subjectiveElement.getElementsByTagName("goal"));
        prep = testDataConn.prepareStatement("SELECT * FROM " + category + " WHERE Tournament = ?");
        prep.setString(1, testTournament);
        rs = prep.executeQuery();
        while (rs.next()) {
          final int teamNumber = rs.getInt("TeamNumber");
          // find row number in table
          final int teamNumberColumn = findColumnByName(tableModel, "TeamNumber");
          Assert.assertTrue("Can't find TeamNumber column in subjective table model", teamNumberColumn >= 0);
          int rowIndex = -1;
          for (int rowIdx = 0; rowIdx < tableModel.getRowCount() && rowIndex == -1; ++rowIdx) {
            final Object teamNumberRaw= tableModel.getValueAt(rowIdx, teamNumberColumn);
            Assert.assertNotNull(teamNumberRaw);
            final int value = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberRaw.toString()).intValue();
            if (value == teamNumber) {
              rowIndex = rowIdx;
            }
          }
          Assert.assertTrue("Can't find team " + teamNumber + " in subjective table model", rowIndex >= 0);

          if (rs.getBoolean("NoShow")) {
            // find column for no show
            final int columnIndex = findColumnByName(tableModel, "No Show");
            Assert.assertTrue("Can't find No Show column in subjective table model", columnIndex >= 0);
            tableModel.setValueAt(Boolean.TRUE, rowIndex, columnIndex);
          } else {
            for(final Element goalElement : goals) {
              final String goalName = goalElement.getAttribute("name");
              final String goalTitle = goalElement.getAttribute("title");

              // find column index for goal and call set
              final int columnIndex = findColumnByName(tableModel, goalTitle);
              Assert.assertTrue("Can't find " + goalTitle + " column in subjective table model", columnIndex >= 0);
              final int value = rs.getInt(goalName);
              tableModel.setValueAt(Integer.valueOf(value), rowIndex, columnIndex);
            }
          }
        }
      }
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
      subjective.save();

      // upload scores
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/index.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      WebForm form = response.getFormWithName("uploadSubjective");
      request = form.getRequest();
      final UploadFileSpec subjectiveUpload = new UploadFileSpec(subjectiveZip);
      form.setParameter("subjectiveFile", new UploadFileSpec[] { subjectiveUpload });
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Subjective data uploaded successfully"));
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
    }
  }

  /**
   * Enter a teams performance score. Data is pulled from testDataConn and
   * pushed to the website.
   * 
   * @param testDataConn where to get the test data from
   * @param performanceElement the challenge description
   * @param testTournament the name of the tournament to enter data for
   * @param runNumber the run to enter data for
   * @param teamNumber the team to enter data for
   * @throws SQLException
   * @throws IOException
   * @throws SAXException
   * @throws MalformedURLException
   * @throws ParseException
   */
  /* package */static void enterPerformanceScore(final Connection testDataConn, final Element performanceElement, final String testTournament,
      final int runNumber, final int teamNumber) throws SQLException, IOException, SAXException, MalformedURLException, ParseException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {

      if (LOG.isInfoEnabled()) {
        LOG.info("Setting score for " + teamNumber + " run: " + runNumber);
      }
      final WebConversation conversation = new WebConversation();

      prep = testDataConn.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ? AND TeamNumber = ?");
      prep.setString(1, testTournament);
      prep.setInt(2, runNumber);
      prep.setInt(3, teamNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        // need to get the score entry form
        WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "scoreEntry/select_team.jsp");
        WebResponse response = conversation.getResponse(request);
        Assert.assertTrue(response.isHTML());
        WebForm form = response.getFormWithName("selectTeam");
        Assert.assertNotNull(form);
        form.setParameter("TeamNumber", String.valueOf(teamNumber));
        request = form.getRequest();
        response = conversation.getResponse(request);
        Assert.assertTrue(response.isHTML());

        form = response.getFormWithName("scoreEntry");

        if (rs.getBoolean("NoShow")) {
          form.setParameter("NoShow", "1");
        } else {
          // walk over challenge descriptor to get all element names and then
          // use the values from rs
          for(final Element element : XMLUtils.filterToElements(performanceElement.getElementsByTagName("goal"))) {
            final String name = element.getAttribute("name");
            final int min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).intValue();
            final int max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).intValue();
            if (LOG.isDebugEnabled()) {
              LOG.debug("Setting form parameter: " + name + " min: " + min + " max: " + max + " readonly: " + form.isReadOnlyParameter(name));
            }
            if (form.isReadOnlyParameter(name)) {
              final int value = rs.getInt(name);
              final int defaultValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(form.getParameterValue(name)).intValue();
              final int difference = value - defaultValue;
              if (LOG.isDebugEnabled()) {
                LOG.debug("Need to increment/decrement " + name + " " + difference);
              }
              final Button button;
              if (difference < 0) {
                button = form.getButtonWithID("inc_" + name + "_1");
              } else {
                button = form.getButtonWithID("dec_" + name + "_-1");
              }
              Assert.assertNotNull("Cannot find button for increment/decrement of " + name, button);
              for (int val = 0; val < Math.abs(difference); ++val) {
                button.click();
              }
            } else {
              final String value = rs.getString(name);
              form.setParameter(name, value);
            }
          }
        }

        // submit score
        request = form.getRequest("submit");
        response = conversation.getResponse(request);
        Assert.assertTrue(response.isHTML());
        Assert.assertEquals("Errors: " + response.getText(), 0, response.getElementsWithName("error").length);
      } else {
        Assert.fail("Cannot find scores for " + teamNumber + " run " + runNumber);
      }
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
    }

  }

  /**
   * Find a column index in a table model by name.
   * 
   * @return -1 if not found
   */
  private static int findColumnByName(final TableModel tableModel, final String name) {
    for (int i = 0; i < tableModel.getColumnCount(); ++i) {
      if (name.equals(tableModel.getColumnName(i))) {
        return i;
      }
    }
    return -1;
  }

  private static final HTMLElementPredicate MATCH_TEXT = new HTMLElementPredicate() {
    public boolean matchesCriteria(final Object htmlElement, final Object criteria) {
      return ((HTMLElement) htmlElement).getText().equals(criteria);
    }
  };
}
