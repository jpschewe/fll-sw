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

import javax.swing.table.TableModel;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.meterware.httpunit.Button;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.thoughtworks.selenium.SeleneseTestCase;

import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.subjective.SubjectiveFrame;
import fll.util.FP;
import fll.util.LogUtils;
import fll.web.scoreEntry.ScoreEntry;
import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

/**
 * Test a full tournament.
 */
public class FullTournamentTest extends SeleneseTestCase {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    super.setUp("http://localhost:9080/setup");
  }

  /**
   * Test a full tournament. This tests to make sure everything works normally.
   * 
   * @throws MalformedURLException
   * @throws IOException
   * @throws SAXException
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws ParseException
   * @throws SQLException
   * @throws InterruptedException
   */
  @Test
  public void testFullTournament() throws MalformedURLException, IOException, SAXException, ClassNotFoundException,
      InstantiationException, IllegalAccessException, ParseException, SQLException, InterruptedException {
    final int numSeedingRounds = 3;

    // create connection to database with test data
    Class.forName("org.hsqldb.jdbcDriver").newInstance();
    Connection testDataConn = null;
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      testDataConn = DriverManager.getConnection("jdbc:hsqldb:res:/fll/web/data/flldb-ft");
      final String testTournamentName = "Field";
      Assert.assertNotNull("Error connecting to test data database", testDataConn);

      stmt = testDataConn.createStatement();

      final WebConversation conversation = new WebConversation();

      // --- initialize database ---
      final InputStream challengeDocIS = FullTournamentTest.class.getResourceAsStream("data/challenge-ft.xml");
      IntegrationTestUtils.initializeDatabase(selenium, challengeDocIS, true);

      // --- load teams ---
      // find upload form on admin page
      WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/");
      WebResponse response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      WebForm form = response.getFormWithID("uploadTeams");

      // set the parameters
      final InputStream teamsIS = FullTournamentTest.class.getResourceAsStream("data/teams-ft.csv");
      Assert.assertNotNull(teamsIS);
      final UploadFileSpec teamsUpload = new UploadFileSpec("teams.csv", teamsIS, "text/plain");
      form.setParameter("file", new UploadFileSpec[] { teamsUpload });
      request = form.getRequest();
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      // Assert.assertNotNull("Error uploading data file: " +
      // response.getText(), response.getElementWithID("success"));
      teamsIS.close();

      // skip past the filter page
      form = response.getFormWithName("filterTeams");
      request = form.getRequest("next");
      response = WebTestUtils.loadPage(conversation, request);
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
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull("Error loading teams: "
          + response.getText(), response.getElementWithID("success"));

      // create tournaments for all regions
      request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/AddTournamentsForRegions");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      Assert.assertNotNull(response.getElementWithID("success"));

      // initialize tournaments by region
      request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/tournamentInitialization.jsp");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("form");
      Assert.assertNotNull(form);
      request = form.getRequest();
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      form = response.getFormWithName("verify");
      Assert.assertNotNull(form);
      request = form.getRequest();
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getElementWithID("success"));

      final Connection serverConnection = TestUtils.createTestDBConnection();
      Assert.assertNotNull("Could not create test database connection", serverConnection);

      // set the tournament
      IntegrationTestUtils.setTournament(selenium, testTournamentName);

      // assign judges
      request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/judges.jsp");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("judges");
      Assert.assertNotNull(form);

      // need to add rows to form if test database has more judges than
      // categories
      prep = testDataConn.prepareStatement("SELECT COUNT(id) FROM Judges WHERE Tournament = ?");
      prep.setString(1, testTournamentName);
      rs = prep.executeQuery();
      Assert.assertTrue("Could not find judges information in test data", rs.next());
      final int numJudges = rs.getInt(1);
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      while (!form.hasParameterNamed("id"
          + String.valueOf(numJudges - 1))) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Adding a row to the judges entry form");
        }
        request = form.getRequest("submit", "Add Row");
        response = WebTestUtils.loadPage(conversation, request);
        Assert.assertTrue(response.isHTML());
        form = response.getFormWithName("judges");
        Assert.assertNotNull(form);
      }

      // assign judges from database
      int judgeIndex = 0;
      prep = testDataConn.prepareStatement("SELECT id, category, Division FROM Judges WHERE Tournament = ?");
      prep.setString(1, testTournamentName);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String id = rs.getString(1);
        final String category = rs.getString(2);
        final String division = rs.getString(3);
        form.setParameter("id"
            + judgeIndex, id);
        form.setParameter("cat"
            + judgeIndex, category);
        form.setParameter("div"
            + judgeIndex, division);
        ++judgeIndex;
      }
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);

      // submit those values
      request = form.getRequest("finished", "Finished");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNull("Got error from judges assignment", response.getElementWithID("error"));

      // commit judges information
      form = response.getFormWithName("judges");
      Assert.assertNotNull(form);
      request = form.getRequest("commit", "Commit");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull("Error assigning judges", response.getElementWithID("success"));

      // assign table labels
      request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/tables.jsp");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("tables");
      Assert.assertNotNull(form);
      form.setParameter("SideA0", "red");
      form.setParameter("SideB0", "blue");
      request = form.getRequest("submit", "Finished");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNull("Got error from judges assignment", response.getElementWithID("error"));
      Assert.assertNotNull(response.getElementWithID("success"));

      /*
       * --- Enter 3 runs for each team --- Use data from test data base,
       * converted from Field 2005. Enter 4th run and rest of playoffs
       */
      prep = testDataConn.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?");
      prep.setString(1, testTournamentName);
      rs = prep.executeQuery();
      Assert.assertTrue("No performance scores in test data", rs.next());
      final int maxRuns = rs.getInt(1);
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);

      final Document challengeDocument = ChallengeParser
                                                        .parse(new InputStreamReader(
                                                                                     FullTournamentTest.class
                                                                                                             .getResourceAsStream("data/challenge-ft.xml")));
      Assert.assertNotNull(challengeDocument);
      final Element rootElement = challengeDocument.getDocumentElement();
      final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

      prep = testDataConn.prepareStatement("SELECT TeamNumber FROM Performance WHERE Tournament = ? AND RunNumber = ?");
      boolean initializedPlayoff = false;
      prep.setString(1, testTournamentName);
      for (int runNumber = 1; runNumber <= maxRuns; ++runNumber) {
        request = new GetMethodWebRequest(TestUtils.URL_ROOT
            + "playoff");
        response = WebTestUtils.loadPage(conversation, request);
        Assert.assertTrue(response.isHTML());
        form = response.getFormWithName("initialize");
        Assert.assertNotNull(form);
        final String[] divisions = form.getOptionValues("division");

        if (runNumber > numSeedingRounds) {
          if (!initializedPlayoff) {
            // TODO make sure to check the result of checking the seeding rounds

            // initialize the playoff brackets with playoff/index.jsp form
            for (int divIdx = 0; divIdx < divisions.length; ++divIdx) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Initializing playoff brackets for division "
                    + divisions[divIdx]);
              }
              form.setParameter("division", divisions[divIdx]);
              request = form.getRequest();
              response = WebTestUtils.loadPage(conversation, request);
              Assert.assertTrue(response.isHTML());
            }
            initializedPlayoff = true;
          }
        }

        prep.setInt(2, runNumber);
        rs = prep.executeQuery();

        // for each score in a run
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          enterPerformanceScore(testDataConn, performanceElement, testTournamentName, runNumber, teamNumber);
          verifyPerformanceScore(testDataConn, performanceElement, testTournamentName, runNumber, teamNumber);
        }

        if (runNumber > numSeedingRounds
            && runNumber != maxRuns) {
          for (int divIdx = 0; divIdx < divisions.length; ++divIdx) {
            printPlayoffScoresheets(divisions[divIdx]);
            LOGGER.info("Succssfully printed scoresheets round: "
                + runNumber + "division: " + divisions[divIdx]);
          }
        }

      }

      checkDisplays();

      enterSubjectiveScores(testDataConn, challengeDocument, testTournamentName);

      computeFinalScores();

      checkReports();

      checkRankAndScores(serverConnection, testTournamentName);
      
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
      SQLFunctions.close(testDataConn);
      // Utilities.closeConnection(connection);
    }
  }
  
  private void computeFinalScores() throws IOException {
    // compute final scores
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/summarizePhase1.jsp");
    
    selenium.click("id=continue");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
    
    Assert.assertTrue(selenium.isElementPresent("id=success"));    
  }
  
  private void checkReports() throws IOException {
    // generate reports
    
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/CategorizedScores");
    
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/CategoryScoresByJudge");
    
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/CategoryScoresByScoreGroup");

    // TODO PDF reports
//    request = new GetMethodWebRequest(TestUtils.URL_ROOT
//        + "report/finalComputedScores.pdf");
//    response = WebTestUtils.loadPage(conversation, request);
//    Assert.assertEquals("application/pdf", response.getContentType());
  }

  private void checkRankAndScores(final Connection serverConnection,
                                  final String testTournamentName) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      // check ranking and scores
      final double scoreFP = 1E-1; // just check to one decimal place

      final Tournament testTournament = Tournament.findTournamentByName(serverConnection, testTournamentName);
      final int testTournamentID = testTournament.getTournamentID();
      prep = serverConnection.prepareStatement("SELECT FinalScores.TeamNumber, FinalScores.OverallScore FROM"
          + " FinalScores, current_tournament_teams WHERE FinalScores.TeamNumber = "
          + " current_tournament_teams.TeamNumber AND FinalScores.Tournament = ? AND"
          + " current_tournament_teams.event_division = ? ORDER BY" + " FinalScores.OverallScore DESC");
      prep.setInt(1, testTournamentID);

      // division 1
      final int[] division1ExpectedRank = { 2636, 3127, 3439, 4462, 3125, 2116, 2104, 2113 };
      final double[] division1ExpectedScores = { 472.76, 423.58, 411.04, 378.04, 374.86, 346.63, 325.95, 310.61 };
      String division = "DivI/Gr4-6";
      prep.setString(2, division);
      rs = prep.executeQuery();
      int rank = 0;
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        Assert.assertEquals("Division I Ranking is incorrect for rank: "
            + rank, division1ExpectedRank[rank], teamNumber);
        final double score = rs.getDouble(2);
        Assert.assertEquals("Overall score incorrect for team: "
            + teamNumber, division1ExpectedScores[rank], score, scoreFP);

        ++rank;
      }
      SQLFunctions.close(rs);

      // division 2
      final int[] division2ExpectedRank = { 3208, 3061, 2863, 2110, 3063, 353, 3129, 2043 };
      final double[] division2ExpectedScores = { 546.78, 512.05, 426.02, 410.23, 407.15, 355.42, 350.14, 348.75 };
      division = "DivII/Gr7-9";
      prep.setString(2, division);
      rs = prep.executeQuery();
      rank = 0;
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        Assert.assertEquals("Division II Ranking is incorrect for rank: "
            + rank, division2ExpectedRank[rank], teamNumber);
        final double score = rs.getDouble(2);
        Assert.assertEquals("Overall score incorrect for team: "
            + teamNumber, division2ExpectedScores[rank], score, scoreFP);

        ++rank;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    // TODO check scores?
  }

  /**
   * Visit the printable brackets for the division specified and print the
   * brackets.
   * 
   * @throws SAXException
   * @throws IOException
   * @throws MalformedURLException
   * @throws InterruptedException
   */
  private static void printPlayoffScoresheets(final String division) throws MalformedURLException, IOException,
      SAXException, InterruptedException {
    final WebConversation conversation = new WebConversation();
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "playoff/index.jsp");
    WebResponse response = WebTestUtils.loadPage(conversation, request);
    Assert.assertTrue(response.isHTML());

    // find form named 'printable'
    WebForm form = response.getFormWithName("printable");

    request = form.getRequest();

    // set division
    request.setParameter("division", division);

    // click 'Display Brackets'
    response = WebTestUtils.loadPage(conversation, request);
    Assert.assertTrue(response.isHTML());

    // find form named 'printScoreSheets'
    form = response.getFormWithName("printScoreSheets");

    // click 'Print scoresheets'
    request = form.getRequest();
    response = WebTestUtils.loadPage(conversation, request);

    // check that result is PDF
    Assert.assertEquals("application/pdf", response.getContentType());

  }

  /**
   * Simulate entering subjective scores by pulling them out of testDataConn.
   * 
   * @param testDataConn Where to get the test data from
   * @param challengeDocument the challenge descriptor
   * @param testTournament the name of the tournament to enter scores for
   * @throws SQLException
   */
  private void enterSubjectiveScores(final Connection testDataConn,
                                     final Document challengeDocument,
                                     final String testTournament) throws SQLException, IOException,
      MalformedURLException, SAXException, ParseException {

    final File subjectiveZip = File.createTempFile("fll", "zip");
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      final WebConversation conversation = new WebConversation();

      // download subjective zip
      WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/subjective-data.fll");
      WebResponse response = WebTestUtils.loadPage(conversation, request);
      Assert.assertEquals("application/zip", response.getContentType());
      final InputStream zipStream = response.getInputStream();
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
      for (final Element subjectiveElement : new NodelistElementCollectionAdapter(
                                                                                  challengeDocument
                                                                                                   .getDocumentElement()
                                                                                                   .getElementsByTagName(
                                                                                                                         "subjectiveCategory"))) {
        final String category = subjectiveElement.getAttribute("name");
        final String title = subjectiveElement.getAttribute("title");
        // find appropriate table model
        final TableModel tableModel = subjective.getTableModelForTitle(title);

        prep = testDataConn.prepareStatement("SELECT * FROM "
            + category + " WHERE Tournament = ?");
        prep.setString(1, testTournament);
        rs = prep.executeQuery();
        while (rs.next()) {
          final int teamNumber = rs.getInt("TeamNumber");
          // find row number in table
          final int teamNumberColumn = findColumnByName(tableModel, "TeamNumber");
          Assert.assertTrue("Can't find TeamNumber column in subjective table model", teamNumberColumn >= 0);
          int rowIndex = -1;
          for (int rowIdx = 0; rowIdx < tableModel.getRowCount()
              && rowIndex == -1; ++rowIdx) {
            final Object teamNumberRaw = tableModel.getValueAt(rowIdx, teamNumberColumn);
            Assert.assertNotNull(teamNumberRaw);
            final int value = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberRaw.toString()).intValue();
            if (value == teamNumber) {
              rowIndex = rowIdx;
            }
          }
          Assert.assertTrue("Can't find team "
              + teamNumber + " in subjective table model", rowIndex >= 0);

          if (rs.getBoolean("NoShow")) {
            // find column for no show
            final int columnIndex = findColumnByName(tableModel, "No Show");
            Assert.assertTrue("Can't find No Show column in subjective table model", columnIndex >= 0);
            tableModel.setValueAt(Boolean.TRUE, rowIndex, columnIndex);
          } else {
            for (final Element goalElement : new NodelistElementCollectionAdapter(
                                                                                  subjectiveElement
                                                                                                   .getElementsByTagName("goal"))) {
              final String goalName = goalElement.getAttribute("name");
              final String goalTitle = goalElement.getAttribute("title");

              // find column index for goal and call set
              final int columnIndex = findColumnByName(tableModel, goalTitle);
              Assert.assertTrue("Can't find "
                  + goalTitle + " column in subjective table model", columnIndex >= 0);
              final int value = rs.getInt(goalName);
              tableModel.setValueAt(Integer.valueOf(value), rowIndex, columnIndex);
            }
          }
        }
      }
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      subjective.save();

      // upload scores
      request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/index.jsp");
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      final WebForm form = response.getFormWithName("uploadSubjective");
      request = form.getRequest();
      final UploadFileSpec subjectiveUpload = new UploadFileSpec(subjectiveZip);
      form.setParameter("subjectiveFile", new UploadFileSpec[] { subjectiveUpload });
      response = WebTestUtils.loadPage(conversation, request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getElementWithID("success"));
    } finally {
      if (!subjectiveZip.delete()) {
        subjectiveZip.deleteOnExit();
      }
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Set a score element that may need button presses to be
   * incremented/decremented.
   * 
   * @param form the form
   * @param name the name of the element
   * @param value the value to set the score element to
   * @throws IOException if there is an error writing to the form
   * @throws SAXException if there is an error clicking the buttons
   * @throws ParseException if there is an error parsing the default value of
   *           the form element as a number
   */
  public static void setFormScoreElement(final WebForm form,
                                         final String name,
                                         final int value) throws IOException, SAXException, ParseException {
    // must be a number
    final double defaultValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(form.getParameterValue(name)).doubleValue();
    final double difference = value
        - defaultValue;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Need to increment/decrement "
          + name + " " + difference);
    }
    final Button button;
    if (difference > 0) {
      button = form.getButtonWithID("inc_"
          + name + "_1");
    } else {
      button = form.getButtonWithID("dec_"
          + name + "_-1");
    }
    if (null == button) {
      throw new RuntimeException("Cannot find button for increment/decrement of "
          + name + "button: " + button);
    }
    for (int val = 0; val < Math.abs(difference); ++val) {
      button.click();
    }
  }

  /**
   * Enter a teams performance score. Data is pulled from testDataConn and
   * pushed to the website.
   */
  private void enterPerformanceScore(final Connection testDataConn,
                                     final Element performanceElement,
                                     final String testTournament,
                                     final int runNumber,
                                     final int teamNumber) throws SQLException, IOException, SAXException,
      MalformedURLException, ParseException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Setting score for "
            + teamNumber + " run: " + runNumber);
      }

      prep = testDataConn
                         .prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ? AND TeamNumber = ?");
      prep.setString(1, testTournament);
      prep.setInt(2, runNumber);
      prep.setInt(3, teamNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        // need to get the score entry form
        IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
            + "scoreEntry/select_team.jsp");

        // select this entry
        selenium.select("xpath=//form[@name='selectTeam']//select[@name='TeamNumber']", "value="
            + teamNumber);

        // submit the page
        selenium.click("id=enter_submit");
        selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

        if (rs.getBoolean("NoShow")) {
          selenium.click("id=no_show");
        } else {
          // walk over challenge descriptor to get all element names and then
          // use the values from rs
          for (final Element element : new NodelistElementCollectionAdapter(
                                                                            performanceElement
                                                                                              .getElementsByTagName("goal"))) {
            final String name = element.getAttribute("name");
            final double min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).doubleValue();
            final double max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).doubleValue();
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Setting form parameter: "
                  + name + " min: " + min + " max: " + max);
            }

            if (XMLUtils.isEnumeratedGoal(element)) {
              final String valueStr = rs.getString(name);
              final String radioID = ScoreEntry.getIDForEnumRadio(name, valueStr);
              selenium.click("id="
                  + radioID);
            } else if (FP.equals(0, min, ChallengeParser.INITIAL_VALUE_TOLERANCE)
                && FP.equals(1, max, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
              final int value = rs.getInt(name);
              final String buttonID;
              if (0 == value) {
                buttonID = name
                    + "_no";
              } else {
                buttonID = name
                    + "_yes";
              }
              selenium.click("id="
                  + buttonID);
            } else {
              final int initialValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("initialValue"))
                                                                       .intValue();
              final int value = rs.getInt(name);
              final String buttonID;
              final int difference;
              if (initialValue < value) {
                // increment
                difference = value
                    - initialValue;
                buttonID = ScoreEntry.getIncDecButtonID(name, 1);
              } else if (value < initialValue) {
                // decrement
                difference = initialValue
                    - value;
                buttonID = ScoreEntry.getIncDecButtonID(name, -1);
              } else {
                // no change
                difference = 0;
                buttonID = null;
              }
              for (int i = 0; i < difference; ++i) {
                selenium.click("id="
                    + buttonID);
              }

            }
          }

          selenium.click("id=submit");
        }

        selenium.getConfirmation();

        selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

        Assert.assertFalse("Errors: ", selenium.isElementPresent("name=error"));
      } else {
        Assert.fail("Cannot find scores for "
            + teamNumber + " run " + runNumber);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

  }

  /**
   * Enter a teams performance score. Data is pulled from testDataConn and
   * pushed to the website.
   */
  private void verifyPerformanceScore(final Connection testDataConn,
                                      final Element performanceElement,
                                      final String testTournament,
                                      final int runNumber,
                                      final int teamNumber) throws SQLException, IOException, SAXException,
      MalformedURLException, ParseException {
    final String selectTeamPage = TestUtils.URL_ROOT
        + "scoreEntry/select_team.jsp";

    ResultSet rs = null;
    PreparedStatement prep = null;
    try {

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Verify score for "
            + teamNumber + " run: " + runNumber);
      }

      prep = testDataConn
                         .prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ? AND TeamNumber = ?");
      prep.setString(1, testTournament);
      prep.setInt(2, runNumber);
      prep.setInt(3, teamNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        if (rs.getBoolean("NoShow")) {
          // no shows don't need verifying
          return;
        } else {
          // need to get the score entry form
          selenium.open(selectTeamPage);
          selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

          // verify that teamNumber is in the list
          Assert.assertTrue("Can't find team number: "
              + teamNumber + " run number: " + runNumber + " in verify list", selenium.isTextPresent("Run "
              + runNumber + " - " + teamNumber));

          // select this entry
          selenium.select("xpath=//form[@name='verify']//select[@name='TeamNumber']", "label=regexp:Run "
              + runNumber + "\\s-\\s" + teamNumber);

          // submit the page
          selenium.click("id=verify_submit");
          selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

          // walk over challenge descriptor to get all element names and then
          // use the values from rs
          for (final Element element : new NodelistElementCollectionAdapter(
                                                                            performanceElement
                                                                                              .getElementsByTagName("goal"))) {
            final String name = element.getAttribute("name");
            final double min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).doubleValue();
            final double max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).doubleValue();

            if (XMLUtils.isEnumeratedGoal(element)) {
              // need check if the right radio button is selected
              final String value = rs.getString(name);
              final String id = ScoreEntry.getIDForEnumRadio(name, value);

              final String formValue = selenium.getValue("id="
                  + id);
              Assert.assertNotNull("Null value for goal: "
                  + name, formValue);

              Assert.assertEquals("Wrong enum selected for goal: "
                  + name, "on", formValue);
            } else if (FP.equals(0, min, ChallengeParser.INITIAL_VALUE_TOLERANCE)
                && FP.equals(1, max, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
              final String formValue = selenium.getValue("name="
                  + name);
              Assert.assertNotNull("Null value for goal: "
                  + name, formValue);

              // yes/no
              final int value = rs.getInt(name);
              final String expectedValue;
              if (value == 0) {
                expectedValue = "off";
              } else {
                expectedValue = "on";
              }
              Assert.assertEquals("Wrong value for goal: "
                  + name, expectedValue, formValue);
            } else {
              final String formValue = selenium.getValue("name="
                  + name);
              Assert.assertNotNull("Null value for goal: "
                  + name, formValue);

              final int value = rs.getInt(name);
              final int formValueInt = Integer.valueOf(formValue);
              Assert.assertEquals("Wrong value for goal: "
                  + name, value, formValueInt);
            }
          }

          // Set the verified field to yes
          selenium.click("id=Verified_yes");

          // submit score
          selenium.click("id=submit");
        }

        // confirm selection, not going to bother checking the text
        selenium.getConfirmation();
        selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

        // check for errors
        Assert.assertEquals(selectTeamPage, selenium.getLocation());
        Assert.assertTrue("Error submitting form, not on select team page", selenium.isTextPresent("Unverified Runs"));

      } else {
        Assert.fail("Cannot find scores for "
            + teamNumber + " run " + runNumber);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

  }

  /**
   * Check display pages that aren't shown otherwise.
   */
  private void checkDisplays() {
    selenium.open(TestUtils.URL_ROOT
        + "scoreboard/main.jsp");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
    Assert.assertFalse("Error loading scoreboard", selenium.isTextPresent("An error has occurred"));

    selenium.open(TestUtils.URL_ROOT
        + "playoff/remoteMain.jsp");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
    Assert.assertFalse("Error loading playoffs", selenium.isTextPresent("An error has occurred"));

    selenium.open(TestUtils.URL_ROOT
        + "welcome.jsp");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
    Assert.assertFalse("Error loading welcome", selenium.isTextPresent("An error has occurred"));
  }

  /**
   * Find a column index in a table model by name.
   * 
   * @return -1 if not found
   */
  private static int findColumnByName(final TableModel tableModel,
                                      final String name) {
    for (int i = 0; i < tableModel.getColumnCount(); ++i) {
      if (name.equals(tableModel.getColumnName(i))) {
        return i;
      }
    }
    return -1;
  }

}
