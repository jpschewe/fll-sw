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
import java.io.Reader;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableModel;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.meterware.httpunit.Button;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;
import fll.Utilities;
import fll.db.ImportDB;
import fll.subjective.SubjectiveFrame;
import fll.util.FP;
import fll.util.LogUtils;
import fll.web.developer.QueryHandler;
import fll.web.playoff.PlayoffIndex;
import fll.web.scoreEntry.ScoreEntry;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.Goal;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;

/**
 * Test a full tournament.
 */
public class FullTournamentTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  private WebDriver selenium;

  @Before
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    selenium = IntegrationTestUtils.createWebDriver();
  }

  @After
  public void tearDown() {
    selenium.quit();
  }

  /**
   * Load the data data from CSV files into the specified connection.
   */
  private static void loadTestData(final Connection testDataConn) throws SQLException, IOException {
    final String[] tableNames = { "teamwork", "robustdesign", "research", "programming", "performance", "judges" };
    for (final String table : tableNames) {
      final InputStream typeStream = FullTournamentTest.class.getResourceAsStream("data/"
          + table + ".types");
      Assert.assertNotNull("Missing test data "
          + table + ".types", typeStream);
      final Reader typeReader = new InputStreamReader(typeStream);
      final Map<String, String> columnTypes = ImportDB.loadTypeInfo(typeReader);

      final InputStream tableStream = FullTournamentTest.class.getResourceAsStream("data/"
          + table + ".csv");
      Assert.assertNotNull("Missing test data "
          + table + ".csv", tableStream);
      final Reader tableReader = new InputStreamReader(tableStream);
      Utilities.loadCSVFile(testDataConn, table, columnTypes, tableReader);

    }
  }

  /**
   * Test a full tournament. This tests to make sure everything works normally.
   * 
   * @throws MalformedURLException
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws ParseException
   * @throws SQLException
   * @throws InterruptedException
   * @throws SAXException
   */
  @Test
  public void testFullTournament() throws IOException, ClassNotFoundException, InstantiationException,
      IllegalAccessException, ParseException, SQLException, InterruptedException, SAXException {
    final int numSeedingRounds = 3;

    Connection testDataConn = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      Class.forName("org.hsqldb.jdbcDriver").newInstance();

      testDataConn = DriverManager.getConnection("jdbc:hsqldb:mem:full-tournament-test");
      Assert.assertNotNull("Error connecting to test data database", testDataConn);

      loadTestData(testDataConn);

      final String testTournamentName = "Field";

      // --- initialize database ---
      LOGGER.info("Initializing the database from data/challenge-ft.xml");
      final InputStream challengeDocIS = FullTournamentTest.class.getResourceAsStream("data/challenge-ft.xml");
      IntegrationTestUtils.initializeDatabase(selenium, challengeDocIS);

      LOGGER.info("Loading teams");
      loadTeams();

      LOGGER.info("Setting current tournament");
      IntegrationTestUtils.setTournament(selenium, testTournamentName);

      LOGGER.info("Assigning judges");
      assignJudges(testDataConn, testTournamentName);

      LOGGER.info("Assigning table labels");
      assignTableLabels();

      /*
       * --- Enter 3 runs for each team --- Use data from test data base,
       * converted from Field 2005. Enter 4th run and rest of playoffs.
       */
      prep = testDataConn.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?");
      prep.setString(1, testTournamentName);
      rs = prep.executeQuery();
      Assert.assertTrue("No performance scores in test data", rs.next());
      final int maxRuns = rs.getInt(1);
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      prep = null;

      final Document challengeDocument = ChallengeParser.parse(new InputStreamReader(
                                                                                     FullTournamentTest.class.getResourceAsStream("data/challenge-ft.xml")));
      Assert.assertNotNull(challengeDocument);
      final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());
      final PerformanceScoreCategory performanceElement = description.getPerformance();

      prep = testDataConn.prepareStatement("SELECT TeamNumber FROM Performance WHERE Tournament = ? AND RunNumber = ?");
      boolean initializedPlayoff = false;
      prep.setString(1, testTournamentName);
      final List<String> divisions = getDivisions();
      for (int runNumber = 1; runNumber <= maxRuns; ++runNumber) {

        if (runNumber > numSeedingRounds) {
          if (!initializedPlayoff) {
            // TODO ticket:83 make sure to check the result of checking the
            // seeding rounds

            // initialize the playoff brackets with playoff/index.jsp form
            for (final String division : divisions) {
              LOGGER.info("Initializing playoff brackets for division "
                  + division);
              checkSeedingRoundsForDivision(division);
              IntegrationTestUtils.initializePlayoffsForDivision(selenium, division);
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
          // give the web server a chance to catch up
          Thread.sleep(100);
          verifyPerformanceScore(testDataConn, performanceElement, testTournamentName, runNumber, teamNumber);
        }

        if (runNumber > numSeedingRounds
            && runNumber != maxRuns) {
          for (final String division : divisions) {
            LOGGER.info("Printing scoresheets round: "
                + runNumber + " division '" + division + "'");
            printPlayoffScoresheets(division);
          }
        }

      }

      LOGGER.info("Checking displays");
      checkDisplays();

      LOGGER.info("Checking the subjective scores");
      enterSubjectiveScores(testDataConn, description, testTournamentName);

      LOGGER.info("Computing final scores");
      computeFinalScores();

      LOGGER.info("Checking the reports");
      checkReports();

      LOGGER.info("Checking rank and scores");
      checkRankAndScores(testTournamentName);

    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final ClassNotFoundException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final InstantiationException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IllegalAccessException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final ParseException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final SQLException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final InterruptedException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(testDataConn);
      // Utilities.closeConnection(connection);
    }
  }

  /**
   * Make sure there are no teams with more or less than seeding rounds for the
   * specified division.
   */
  private void checkSeedingRoundsForDivision(final String division) throws IOException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "playoff");
    final Select select = new Select(selenium.findElement(By.id("check-division")));
    select.selectByValue(division);
    selenium.findElement(By.id("check_seeding_rounds")).click();

    Assert.assertFalse("Some teams with more or less than seeding rounds found for division '"
        + division + "'", IntegrationTestUtils.isElementPresent(selenium, By.className("warning")));
  }

  /**
   * Get the divisions in this tournament.
   * 
   * @throws IOException
   */
  private List<String> getDivisions() throws IOException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "playoff");

    final List<String> divisions = new LinkedList<String>();
    final Select select = new Select(selenium.findElement(By.id("initialize-division")));
    for (final WebElement option : select.getOptions()) {
      final String text = option.getText();
      if (!PlayoffIndex.CREATE_NEW_PLAYOFF_DIVISION.equals(text)) {
        divisions.add(text);
      }
    }

    Assert.assertFalse(divisions.isEmpty());
    return divisions;
  }

  private void assignTableLabels() throws IOException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/tables.jsp");
    selenium.findElement(By.name("SideA0")).sendKeys("red");
    selenium.findElement(By.name("SideB0")).sendKeys("blue");
    selenium.findElement(By.id("finished")).click();

    Assert.assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.id("error")));
    Assert.assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));
  }

  private void assignJudge(final String id,
                           final String category,
                           final String station,
                           final int judgeIndex) throws IOException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Assigning judge '"
          + id + "' cat: '" + category + "' station: '" + station + "' index: " + judgeIndex);
    }

    // make sure the row exists
    while (!IntegrationTestUtils.isElementPresent(selenium, By.name("id"
        + String.valueOf(judgeIndex)))) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Adding a row to the judges entry form to get to: "
            + judgeIndex);
        IntegrationTestUtils.storeScreenshot(selenium);
      }
      selenium.findElement(By.id("add_rows")).click();
      try {
        Thread.sleep(2000); // let the javascript do it's work
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    selenium.findElement(By.name("id"
        + judgeIndex)).sendKeys(id);

    final Select categorySelect = new Select(selenium.findElement(By.name("cat"
        + judgeIndex)));
    categorySelect.selectByValue(category);

    final Select stationSelect = new Select(selenium.findElement(By.name("station"
        + judgeIndex)));
    stationSelect.selectByValue(station);

  }

  private void assignJudges(final Connection testDataConn,
                            final String testTournamentName) throws IOException, SQLException {
    ResultSet rs;
    PreparedStatement prep;

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/index.jsp");

    selenium.findElement(By.id("assign_judges")).click();

    // assign judges from database
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Assigning judges");
      IntegrationTestUtils.storeScreenshot(selenium);
    }

    int judgeIndex = 1;
    prep = testDataConn.prepareStatement("SELECT id, category, Division FROM Judges WHERE Tournament = ?");
    prep.setString(1, testTournamentName);
    rs = prep.executeQuery();
    while (rs.next()) {
      final String id = rs.getString(1);
      final String category = rs.getString(2);
      final String division = rs.getString(3);

      if ("All".equals(division)) {

        final Select select = new Select(IntegrationTestUtils.findElement(selenium, By.name("station1"), 5));
        for (final WebElement option : select.getOptions()) {
          final String station = option.getText();
          assignJudge(id, category, station, judgeIndex);
          ++judgeIndex;
        }

      } else {
        assignJudge(id, category, division, judgeIndex);
        ++judgeIndex;
      }
    }
    SQLFunctions.close(rs);
    SQLFunctions.close(prep);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("After assigning judges");
      IntegrationTestUtils.storeScreenshot(selenium);
    }

    // submit those values
    selenium.findElement(By.id("finished")).click();

    Assert.assertFalse("Got error from judges assignment",
                       IntegrationTestUtils.isElementPresent(selenium, By.id("error")));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Verifying judges");
      IntegrationTestUtils.storeScreenshot(selenium);
    }

    // commit judges information
    selenium.findElement(By.id("commit")).click();
    Assert.assertTrue("Error assigning judges", IntegrationTestUtils.isElementPresent(selenium, By.id("success")));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("After committing judges");
      IntegrationTestUtils.storeScreenshot(selenium);
    }

  }

  private void loadTeams() throws IOException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/");

    final InputStream teamsIS = FullTournamentTest.class.getResourceAsStream("data/teams-ft.csv");
    Assert.assertNotNull(teamsIS);
    final File teamsFile = IntegrationTestUtils.storeInputStreamToFile(teamsIS);
    teamsIS.close();
    try {
      selenium.findElement(By.id("teams_file")).sendKeys(teamsFile.getAbsolutePath());

      selenium.findElement(By.id("upload_teams")).click();

      IntegrationTestUtils.assertNoException(selenium);
    } finally {
      if (!teamsFile.delete()) {
        teamsFile.deleteOnExit();
      }
    }

    // skip past the filter page
    selenium.findElement(By.id("next")).click();
    IntegrationTestUtils.assertNoException(selenium);

    // team column selection
    new Select(selenium.findElement(By.name("TeamNumber"))).selectByValue("tea_number");
    new Select(selenium.findElement(By.name("TeamName"))).selectByValue("tea_name");
    new Select(selenium.findElement(By.name("Organization"))).selectByValue("org_name");
    new Select(selenium.findElement(By.name("tournament"))).selectByValue("eve_name");
    new Select(selenium.findElement(By.name("Division"))).selectByValue("div_name");
    selenium.findElement(By.id("next")).click();
    IntegrationTestUtils.assertNoException(selenium);
    Assert.assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));
  }

  private void computeFinalScores() throws IOException {
    // compute final scores
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/SummarizePhase1");

    selenium.findElement(By.id("continue")).click();

    Assert.assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));
  }

  private void checkReports() throws IOException, SAXException {
    // generate reports

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/CategorizedScores");

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/CategoryScoresByJudge");

    // PDF reports need to be done with httpunit
    final WebConversation conversation = WebTestUtils.getConversation();
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "report/finalComputedScores.pdf");
    WebResponse response = WebTestUtils.loadPage(conversation, request);
    Assert.assertEquals("application/pdf", response.getContentType());

    request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "report/CategoryScoresByScoreGroup");
    response = WebTestUtils.loadPage(conversation, request);
    Assert.assertEquals("application/pdf", response.getContentType());

    request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "report/PlayoffReport");
    response = WebTestUtils.loadPage(conversation, request);
    Assert.assertEquals("application/pdf", response.getContentType());

  }

  private void checkRankAndScores(final String testTournamentName) throws IOException, SAXException {
    // check ranking and scores
    final double scoreFP = 1E-1; // just check to one decimal place

    final String sqlTemplate = "SELECT FinalScores.TeamNumber, FinalScores.OverallScore" //
        + " FROM FinalScores, current_tournament_teams, Tournaments" //
        + " WHERE FinalScores.TeamNumber = current_tournament_teams.TeamNumber" //
        + " AND Tournaments.Name = '%s'" + " AND FinalScores.Tournament = Tournaments.tournament_id" //
        + " AND current_tournament_teams.event_division = '%s'" //
        + " ORDER BY FinalScores.OverallScore DESC";

    // division 1
    final int[] division1ExpectedRank = { 2636, 3127, 3439, 4462, 3125, 2116, 2104, 2113 };
    final double[] division1ExpectedScores = { 472.76, 423.58, 411.04, 378.04, 374.86, 346.63, 325.95, 310.61 };
    String division = "DivI/Gr4-6";

    final String div1Query = String.format(sqlTemplate, testTournamentName, division);
    final QueryHandler.ResultData div1Result = WebTestUtils.executeServerQuery(div1Query);

    int rank = 0;
    for (final Map<String, String> row : div1Result.data) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("checkRankAndScores - row: "
            + row);
      }

      final int teamNumber = Integer.parseInt(row.get("teamnumber"));
      Assert.assertEquals("Division I Ranking is incorrect for rank: "
          + rank, division1ExpectedRank[rank], teamNumber);
      final double score = Double.valueOf(row.get("overallscore"));
      Assert.assertEquals("Overall score incorrect for team: "
          + teamNumber, division1ExpectedScores[rank], score, scoreFP);

      ++rank;
    }

    // division 2
    final int[] division2ExpectedRank = { 3208, 3061, 2863, 2110, 3063, 353, 3129, 2043 };
    final double[] division2ExpectedScores = { 546.78, 512.05, 426.02, 410.23, 407.15, 355.42, 350.14, 348.75 };
    division = "DivII/Gr7-9";

    final String div2Query = String.format(sqlTemplate, testTournamentName, division);
    final QueryHandler.ResultData div2Result = WebTestUtils.executeServerQuery(div2Query);

    rank = 0;
    for (final Map<String, String> row : div2Result.data) {
      final int teamNumber = Integer.parseInt(row.get("teamnumber"));
      Assert.assertEquals("Division II Ranking is incorrect for rank: "
          + rank, division2ExpectedRank[rank], teamNumber);
      final double score = Double.valueOf(row.get("overallscore"));
      Assert.assertEquals("Overall score incorrect for team: "
          + teamNumber, division2ExpectedScores[rank], score, scoreFP);

      ++rank;
    }
  }

  /**
   * Visit the printable brackets for the division specified and print the
   * brackets.
   * 
   * @throws IOException
   * @throws MalformedURLException
   * @throws InterruptedException
   * @throws SAXException
   */
  private static void printPlayoffScoresheets(final String division) throws MalformedURLException, IOException,
      InterruptedException, SAXException {
    final WebConversation conversation = WebTestUtils.getConversation();
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "playoff/index.jsp");
    WebResponse response = WebTestUtils.loadPage(conversation, request);
    Assert.assertTrue(response.isHTML());

    // find form named 'printable'
    WebForm form = response.getFormWithName("printable");
    Assert.assertNotNull("printable form not found", form);

    request = form.getRequest();

    // set division
    request.setParameter("division", division);

    // click 'Display Brackets'
    response = WebTestUtils.loadPage(conversation, request);
    Assert.assertTrue(response.isHTML());

    // find form named 'printScoreSheets'
    form = response.getFormWithName("printScoreSheets");
    Assert.assertNotNull("printScoreSheets form not found", form);

    form.setCheckbox("print1", true);

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
   * @throws SAXException
   */
  private void enterSubjectiveScores(final Connection testDataConn,
                                     final ChallengeDescription description,
                                     final String testTournament) throws SQLException, IOException,
      MalformedURLException, ParseException, SAXException {

    final File subjectiveZip = File.createTempFile("fll", ".zip", new File("screenshots"));
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      final WebConversation conversation = WebTestUtils.getConversation();

      // download subjective zip
      WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/subjective-data.fll");
      WebResponse response = WebTestUtils.loadPage(conversation, request);
      final String contentType = response.getContentType();
      if (!"application/zip".equals(contentType)) {
        LOGGER.error("Got non-zip content: "
            + response.getText());
      }
      Assert.assertEquals("application/zip", contentType);
      final InputStream zipStream = response.getInputStream();
      final FileOutputStream outputStream = new FileOutputStream(subjectiveZip);
      final byte[] buffer = new byte[512];
      int bytesRead = 0;
      while (-1 != (bytesRead = zipStream.read(buffer))) {
        outputStream.write(buffer, 0, bytesRead);
      }
      outputStream.close();
      zipStream.close();

      final SubjectiveFrame subjective = new SubjectiveFrame();
      subjective.load(subjectiveZip);

      // insert scores into zip
      for (final ScoreCategory subjectiveElement : description.getSubjectiveCategories()) {
        final String category = subjectiveElement.getName();
        final String title = subjectiveElement.getTitle();

        // find appropriate table model
        final TableModel tableModel = subjective.getTableModelForTitle(title);
        Assert.assertNotNull(tableModel);

        final int teamNumberColumn = findColumnByName(tableModel, "TeamNumber");
        Assert.assertTrue("Can't find TeamNumber column in subjective table model", teamNumberColumn >= 0);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Found team number column at "
              + teamNumberColumn);
        }

        prep = testDataConn.prepareStatement("SELECT * FROM "
            + category + " WHERE Tournament = ?");
        prep.setString(1, testTournament);
        rs = prep.executeQuery();
        while (rs.next()) {
          final int teamNumber = rs.getInt("TeamNumber");

          // find row number in table
          int rowIndex = -1;
          for (int rowIdx = 0; rowIdx < tableModel.getRowCount(); ++rowIdx) {
            final Object teamNumberRaw = tableModel.getValueAt(rowIdx, teamNumberColumn);
            Assert.assertNotNull(teamNumberRaw);
            final int value = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberRaw.toString()).intValue();

            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Checking if "
                  + teamNumber + " equals " + value + " raw: " + teamNumberRaw + "? " + (value == teamNumber)
                  + " rowIdx: " + rowIdx + " numRows: " + tableModel.getRowCount());
            }

            if (value == teamNumber) {
              rowIndex = rowIdx;
              break;
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
            for (final AbstractGoal goalElement : subjectiveElement.getGoals()) {
              if (!goalElement.isComputed()) {
                final String goalName = goalElement.getName();
                final String goalTitle = goalElement.getTitle();

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
   * @throws ParseException if there is an error parsing the default value of
   *           the form element as a number
   * @throws SAXException
   */
  public static void setFormScoreElement(final WebForm form,
                                         final String name,
                                         final int value) throws IOException, ParseException, SAXException {
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
                                     final PerformanceScoreCategory performanceElement,
                                     final String testTournament,
                                     final int runNumber,
                                     final int teamNumber) throws SQLException, IOException, MalformedURLException,
      ParseException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Setting score for "
            + teamNumber + " run: " + runNumber);
      }

      prep = testDataConn.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ? AND TeamNumber = ?");
      prep.setString(1, testTournament);
      prep.setInt(2, runNumber);
      prep.setInt(3, teamNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        // need to get the score entry form
        IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
            + "scoreEntry/select_team.jsp");

        // select this entry
        new Select(selenium.findElement(By.id("select-teamnumber"))).selectByValue(String.valueOf(teamNumber));

        // submit the page
        selenium.findElement(By.id("enter_submit")).click();

        if (rs.getBoolean("NoShow")) {
          selenium.findElement(By.id("no_show")).click();
        } else {
          // walk over challenge descriptor to get all element names and then
          // use the values from rs
          for (final AbstractGoal element : performanceElement.getGoals()) {
            if (!element.isComputed()) {
              final Goal goal = (Goal) element;
              final String name = goal.getName();
              final double min = goal.getMin();
              final double max = goal.getMax();
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Setting form parameter: "
                    + name + " min: " + min + " max: " + max);
              }

              if (goal.isEnumerated()) {
                final String valueStr = rs.getString(name);
                final String radioID = ScoreEntry.getIDForEnumRadio(name, valueStr);
                selenium.findElement(By.id(radioID)).click();
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
                selenium.findElement(By.id(buttonID)).click();
              } else {
                final int initialValue = (int) goal.getInitialValue();
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
                  selenium.findElement(By.id(buttonID)).click();
                }

              }
            } // !computed
          } // foreach goal

          selenium.findElement(By.id("submit")).click();
        }

        final Alert confirmScoreChange = selenium.switchTo().alert();
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Confirmation text: "
              + confirmScoreChange.getText());
        }
        confirmScoreChange.accept();

        Assert.assertFalse("Errors: ", IntegrationTestUtils.isElementPresent(selenium, By.name("error")));
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
   * 
   * @throws InterruptedException
   */
  private void verifyPerformanceScore(final Connection testDataConn,
                                      final PerformanceScoreCategory performanceElement,
                                      final String testTournament,
                                      final int runNumber,
                                      final int teamNumber) throws SQLException, IOException, MalformedURLException,
      ParseException, InterruptedException {
    final String selectTeamPage = TestUtils.URL_ROOT
        + "scoreEntry/select_team.jsp";

    ResultSet rs = null;
    PreparedStatement prep = null;
    try {

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Verify score for "
            + teamNumber + " run: " + runNumber);
      }

      prep = testDataConn.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ? AND TeamNumber = ?");
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
          IntegrationTestUtils.loadPage(selenium, selectTeamPage);

          new Select(selenium.findElement(By.id("select-verify-teamnumber"))).selectByValue(teamNumber
              + "-" + runNumber);

          // submit the page
          selenium.findElement(By.id("verify_submit")).click();

          // walk over challenge descriptor to get all element names and then
          // use the values from rs
          for (final AbstractGoal element : performanceElement.getGoals()) {
            if (!element.isComputed()) {
              final Goal goal = (Goal) element;
              final String name = goal.getName();
              final double min = goal.getMin();
              final double max = goal.getMax();

              if (goal.isEnumerated()) {
                // need check if the right radio button is selected
                final String value = rs.getString(name);

                final String formValue = selenium.findElement(By.name(ScoreEntry.getElementNameForYesNoDisplay(name)))
                                                 .getAttribute("value");
                Assert.assertNotNull("Null value for goal: "
                    + name, formValue);

                Assert.assertEquals("Wrong enum selected for goal: "
                    + name, value.toLowerCase(), formValue.toLowerCase());
              } else if (FP.equals(0, min, ChallengeParser.INITIAL_VALUE_TOLERANCE)
                  && FP.equals(1, max, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
                final String formValue = selenium.findElement(By.name(ScoreEntry.getElementNameForYesNoDisplay(name)))
                                                 .getAttribute("value");
                Assert.assertNotNull("Null value for goal: "
                    + name, formValue);

                // yes/no
                final int value = rs.getInt(name);
                final String expectedValue;
                if (value == 0) {
                  expectedValue = "no";
                } else {
                  expectedValue = "yes";
                }
                Assert.assertEquals("Wrong value for goal: "
                    + name, expectedValue.toLowerCase(), formValue.toLowerCase());
              } else {
                final String formValue = selenium.findElement(By.name(name)).getAttribute("value");
                Assert.assertNotNull("Null value for goal: "
                    + name, formValue);

                final int value = rs.getInt(name);
                final int formValueInt = Integer.parseInt(formValue);
                Assert.assertEquals("Wrong value for goal: "
                    + name, value, formValueInt);
              }
            } // !computed
          } // foreach goal

          // Set the verified field to yes
          selenium.findElement(By.id("Verified_yes")).click();

          // submit score
          selenium.findElement(By.id("submit")).click();
        }

        // confirm selection, not going to bother checking the text
        final Alert confirmScoreChange = selenium.switchTo().alert();
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Confirmation text: "
              + confirmScoreChange.getText());
        }
        confirmScoreChange.accept();

        // give the web server a chance to catch up
        Thread.sleep(1500);

        // check for errors
        // Gives trouble too often
        // Assert.assertEquals(selectTeamPage, selenium.getCurrentUrl());
        Assert.assertTrue("Error submitting form, not on select team page url: "
            + selenium.getCurrentUrl(), selenium.getPageSource().contains("Unverified Runs"));

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
   * 
   * @throws IOException
   */
  private void checkDisplays() throws IOException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "scoreboard/main.jsp");

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "playoff/remoteMain.jsp");

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "welcome.jsp");
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
