/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.swing.table.TableModel;

import org.apache.commons.lang3.StringUtils;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.security.NoExitSecurityManagerInstaller;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.diffplug.common.base.Errors;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.opencsv.CSVWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.JudgeInformation;
import fll.TestUtils;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.CategoryColumnMapping;
import fll.db.GlobalParameters;
import fll.db.ImportDB;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.scheduler.TournamentSchedule;
import fll.subjective.SubjectiveFrame;
import fll.web.developer.QueryHandler;
import fll.web.scoreEntry.ScoreEntry;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.Goal;
import fll.xml.PerformanceScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import io.github.artsok.RepeatedIfExceptionsTest;

/**
 * Test a full tournament.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class FullTournamentTest {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static NoExitSecurityManagerInstaller noExitSecurityManagerInstaller;

  @AfterAll
  public static void tearDownOnce() {
    noExitSecurityManagerInstaller.uninstall();
  }

  @BeforeAll
  public static void setUpOnce() {
    FailOnThreadViolationRepaintManager.install();
    noExitSecurityManagerInstaller = NoExitSecurityManagerInstaller.installNoExitSecurityManager(status -> assertEquals(0,
                                                                                                                        status,
                                                                                                                        "Bad exit status"));
  }

  /**
   * Load the test data into the specified database.
   */
  private static void loadTestData(final Connection testDataConn) throws SQLException, IOException {
    try (final InputStream dbResourceStream = FullTournamentTest.class.getResourceAsStream("data/99-final.flldb")) {
      assertNotNull(dbResourceStream, "Missing test data");
      final ZipInputStream zipStream = new ZipInputStream(dbResourceStream);
      final ImportDB.ImportResult result = ImportDB.loadFromDumpIntoNewDB(zipStream, testDataConn);
      TestUtils.deleteImportData(result);
    }
  }

  /**
   * Test a full tournament. This tests to make sure everything works normally.
   *
   * @throws MalformedURLException test error
   * @throws IOException test error
   * @throws ClassNotFoundException test error
   * @throws InstantiationException test error
   * @throws IllegalAccessException test error
   * @throws ParseException test error
   * @throws SQLException test error
   * @throws InterruptedException test error
   * @throws SAXException test error
   */
  @Test
  @RepeatedIfExceptionsTest(repeats = 3)
  public void testFullTournament(final WebDriver selenium) throws IOException, ClassNotFoundException,
      InstantiationException, IllegalAccessException, ParseException, SQLException, InterruptedException, SAXException {
    try {
      Class.forName("org.hsqldb.jdbcDriver").newInstance();

      try (final Connection testDataConn = DriverManager.getConnection("jdbc:hsqldb:mem:full-tournament-test")) {
        assertNotNull(testDataConn, "Error connecting to test data database");

        loadTestData(testDataConn);

        final String testTournamentName = "Field";

        final Path outputDirectory = Files.createDirectories(Paths.get("FullTournamentTestOutputs"));

        if (null != outputDirectory) {
          // make sure the directory exists
          Files.createDirectories(outputDirectory);
        }

        replayTournament(selenium, testDataConn, testTournamentName, outputDirectory);

        LOGGER.info("Computing final scores");
        computeFinalScores(selenium);

        LOGGER.info("Checking the reports");
        checkReports(selenium);

        LOGGER.info("Checking rank and scores");
        checkRankAndScores(testTournamentName);
      } // try Connection

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
    }
  }

  /**
   * Replay a tournament.
   *
   * @param selenium the web driver to use
   * @param testDataConn connection to the source data
   * @param testTournamentName name of the tournament to create
   * @param outputDirectory where to save files, must not be null and must exist
   * @throws IOException
   * @throws SQLException
   * @throws ParseException
   * @throws InterruptedException
   * @throws SAXException
   */
  public void replayTournament(final WebDriver selenium,
                               final Connection testDataConn,
                               final String testTournamentName,
                               final Path outputDirectory)
      throws IOException, SQLException, ParseException, InterruptedException, SAXException {
    final String safeTestTournamentName = sanitizeFilename(testTournamentName);

    final Document challengeDocument = GlobalParameters.getChallengeDocument(testDataConn);
    assertNotNull(challengeDocument);

    assertThat(outputDirectory, notNullValue());
    assertTrue(Files.exists(outputDirectory), "Output directory must exist");

    final Tournament sourceTournament = Tournament.findTournamentByName(testDataConn, testTournamentName);
    assertNotNull(sourceTournament);

    final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(testDataConn,
                                                                          sourceTournament.getTournamentID());

    // --- initialize database ---
    LOGGER.info("Initializing the database");
    IntegrationTestUtils.initializeDatabase(selenium, challengeDocument);

    LOGGER.info("Loading teams");
    loadTeams(selenium, testDataConn, sourceTournament, outputDirectory);

    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "admin/database.flldb"), "application/zip", outputDirectory.resolve(
                                                                              safeTestTournamentName
                                                                                  + "_01-teams-loaded.flldb"));

    LOGGER.info("Setting current tournament");
    IntegrationTestUtils.setTournament(selenium, sourceTournament.getName());

    LOGGER.info("Loading the schedule");
    uploadSchedule(selenium, testDataConn, sourceTournament, outputDirectory);
    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "admin/database.flldb"), "application/zip", outputDirectory.resolve(
                                                                              safeTestTournamentName
                                                                                  + "_02-schedule-loaded.flldb"));

    LOGGER.info("Assigning judges");
    assignJudges(selenium, testDataConn, sourceTournament);

    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "admin/database.flldb"), "application/zip", outputDirectory.resolve(
                                                                              safeTestTournamentName
                                                                                  + "_03-judges-assigned.flldb"));

    LOGGER.info("Assigning table labels");
    assignTableLabels(selenium);

    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "admin/database.flldb"), "application/zip", outputDirectory.resolve(
                                                                              safeTestTournamentName
                                                                                  + "_04-table-labels-assigned.flldb"));

    /*
     * --- Enter 3 runs for each team --- Use data from test data base,
     * converted from Field 2005. Enter 4th run and rest of playoffs.
     */
    final int maxRuns;
    try (
        final PreparedStatement maxRunPrep = testDataConn.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?")) {
      maxRunPrep.setInt(1, sourceTournament.getTournamentID());
      try (final ResultSet maxRunResult = maxRunPrep.executeQuery()) {
        assertTrue(maxRunResult.next(), "No performance scores in test data");
        maxRuns = maxRunResult.getInt(1);
      }
    }

    final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());
    final PerformanceScoreCategory performanceElement = description.getPerformance();

    try (
        final PreparedStatement prep = testDataConn.prepareStatement("SELECT TeamNumber FROM Performance WHERE Tournament = ? AND RunNumber = ?")) {

      boolean initializedPlayoff = false;
      prep.setInt(1, sourceTournament.getTournamentID());

      final Set<String> awardGroups = getAwardGroups(selenium);
      for (int runNumber = 1; runNumber <= maxRuns; ++runNumber) {

        if (runNumber > numSeedingRounds) {
          if (!initializedPlayoff) {
            IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
                + "admin/database.flldb"), "application/zip", outputDirectory.resolve(
                                                                                      safeTestTournamentName
                                                                                          + "_05-seeding-rounds-completed.flldb"));

            checkSeedingRounds(selenium);

            // initialize the playoff brackets with playoff/index.jsp form
            for (final String awardGroup : awardGroups) {
              LOGGER.info("Initializing playoff brackets for division "
                  + awardGroup);
              IntegrationTestUtils.initializePlayoffsForAwardGroup(selenium, awardGroup);
            }
            initializedPlayoff = true;
          }
        }

        prep.setInt(2, runNumber);
        try (final ResultSet rs = prep.executeQuery()) {
          // for each score in a run
          while (rs.next()) {
            final int teamNumber = rs.getInt(1);
            enterPerformanceScore(selenium, testDataConn, performanceElement, sourceTournament, runNumber, teamNumber);

            // give the web server a chance to catch up
            Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

            verifyPerformanceScore(selenium, testDataConn, performanceElement, sourceTournament, runNumber, teamNumber);
          }
        }

        if (runNumber > numSeedingRounds
            && runNumber != maxRuns) {
          for (final String division : awardGroups) {
            printPlayoffScoresheets(division);
          }
        }

      }

      LOGGER.info("Checking displays");
      checkDisplays(selenium);

      LOGGER.info("Checking the subjective scores");
      enterSubjectiveScores(selenium, testDataConn, description, sourceTournament, outputDirectory);

      LOGGER.info("Writing final datbaase");
      IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
          + "admin/database.flldb"), "application/zip", outputDirectory.resolve(
                                                                                safeTestTournamentName
                                                                                    + "_99-final.flldb"));
    }

  }

  /**
   * @param selenium TODO
   * @param testDataConn
   * @param sourceTournament
   * @throws SQLException
   * @throws IOException
   * @throws InterruptedException
   */
  private void uploadSchedule(final WebDriver selenium,
                              final Connection testDataConn,
                              final Tournament sourceTournament,
                              final Path outputDirectory)
      throws SQLException, IOException, InterruptedException {
    if (TournamentSchedule.scheduleExistsInDatabase(testDataConn, sourceTournament.getTournamentID())) {

      final TournamentSchedule schedule = new TournamentSchedule(testDataConn, sourceTournament.getTournamentID());

      final Path outputFile = outputDirectory.resolve(sanitizeFilename(sourceTournament.getName())
          + "_schedule.csv");
      schedule.writeToCSV(outputFile.toFile());

      // upload the saved file
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "admin/index.jsp");
      final WebElement fileInput = selenium.findElement(By.name("scheduleFile"));
      fileInput.sendKeys(outputFile.toAbsolutePath().toString());
      selenium.findElement(By.id("upload-schedule")).click();
      assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.id("error")));

      // accept default schedule constraints
      assertTrue(selenium.getCurrentUrl().contains("scheduleConstraints"));
      selenium.findElement(By.id("submit")).click();
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

      // check that we're on the choose headers page and set the header
      // mappings
      assertTrue(selenium.getCurrentUrl().contains("chooseSubjectiveHeaders"));
      final Collection<CategoryColumnMapping> mappings = CategoryColumnMapping.load(testDataConn,
                                                                                    sourceTournament.getTournamentID());
      for (final CategoryColumnMapping map : mappings) {
        final Select select = new Select(selenium.findElement(By.name(map.getCategoryName()
            + ":header")));
        select.selectByVisibleText(map.getScheduleColumn());
      }
      selenium.findElement(By.id("submit")).click();

      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

      // check that we don't have hard violations and skip past soft
      // violations
      assertThat(selenium.getCurrentUrl(), not(containsString("displayHardViolations")));
      if (selenium.getCurrentUrl().contains("displaySoftViolations")) {
        selenium.findElement(By.id("yes")).click();

        Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);
      }

      // set event divisions
      if (selenium.getCurrentUrl().contains("promptForEventDivision")) {
        selenium.findElement(By.id("yes")).click();

        Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

        // assume the values are fine
        assertThat(selenium.getCurrentUrl(), containsString("displayEventDivisionConfirmation"));
        selenium.findElement(By.id("yes")).click();

        Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);
      }

      // check that it all worked
      assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.id("error")));
      assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));
    }
  }

  /**
   * Make sure there are no teams with more or less than seeding rounds.
   *
   * @param selenium TODO
   * @throws InterruptedException
   */
  private void checkSeedingRounds(final WebDriver selenium) throws IOException, InterruptedException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "playoff");
    selenium.findElement(By.id("check_seeding_rounds")).click();

    assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.className("warning")),
                "Some teams with more or less than seeding rounds found");
  }

  /**
   * Get the award groups in this tournament.
   *
   * @param selenium TODO
   * @throws IOException
   * @throws InterruptedException
   */
  private Set<String> getAwardGroups(final WebDriver selenium) throws IOException, InterruptedException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/index.jsp");

    selenium.findElement(By.id("change-award-groups")).click();

    final Set<String> awardGroups = new HashSet<>();
    final List<WebElement> inputs = selenium.findElements(By.cssSelector("input:checked[type='radio']"));
    for (final WebElement radioButton : inputs) {
      final String awardGroup = radioButton.getAttribute("value");
      awardGroups.add(awardGroup);
    }

    LOGGER.info("Found awardGroups: "
        + awardGroups);

    assertFalse(awardGroups.isEmpty());
    return awardGroups;
  }

  private void assignTableLabels(final WebDriver selenium) throws IOException, InterruptedException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/tables.jsp");

    final WebElement sidea0 = selenium.findElement(By.name("SideA0"));
    final WebElement sideb0 = selenium.findElement(By.name("SideB0"));
    if (StringUtils.isEmpty(sidea0.getAttribute("value"))
        && StringUtils.isEmpty(sideb0.getAttribute("value"))) {
      // Table labels should be assigned by the schedule, but may not be. If
      // they're not assigned, then assign them.
      sidea0.sendKeys("red");
      sideb0.sendKeys("blue");
    }

    selenium.findElement(By.id("finished")).click();

    assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.id("error")));
    assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));
  }

  private void assignJudge(final WebDriver selenium,
                           final String id,
                           final String category,
                           final String station,
                           final int judgeIndex)
      throws IOException, InterruptedException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Assigning judge '"
          + id
          + "' cat: '"
          + category
          + "' station: '"
          + station
          + "' index: "
          + judgeIndex);
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

      // let the javascript do it's work
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);
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

  private void assignJudges(final WebDriver selenium,
                            final Connection testDataConn,
                            final Tournament sourceTournament)
      throws IOException, SQLException, InterruptedException {

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/index.jsp");
    selenium.findElement(By.id("assign_judges")).click();

    final Collection<JudgeInformation> judges = JudgeInformation.getJudges(testDataConn,
                                                                           sourceTournament.getTournamentID());

    int judgeIndex = 1;
    for (final JudgeInformation judge : judges) {

      assignJudge(selenium, judge.getId(), judge.getCategory(), judge.getGroup(), judgeIndex);

      ++judgeIndex;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("After assigning judges");
      IntegrationTestUtils.storeScreenshot(selenium);
    }

    // submit those values
    selenium.findElement(By.id("finished")).click();

    assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.id("error")), "Got error from judges assignment");

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Verifying judges");
      IntegrationTestUtils.storeScreenshot(selenium);
    }

    // commit judges information
    selenium.findElement(By.id("commit")).click();
    assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")), "Error assigning judges");

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("After committing judges");
      IntegrationTestUtils.storeScreenshot(selenium);
    }

  }

  /**
   * Load the teams from testDataConnection.
   *
   * @param selenium TODO
   * @param testDataConnection where to get the teams from
   * @param outputDirectory where to write the teams file, may be null in which
   *          case a temp file will be used
   * @throws IOException
   * @throws SQLException
   * @throws InterruptedException
   */
  private void loadTeams(final WebDriver selenium,
                         final Connection testDataConnection,
                         final Tournament sourceTournament,
                         final Path outputDirectory)
      throws IOException, SQLException, InterruptedException {

    final Path teamsFile = outputDirectory.resolve(sanitizeFilename(sourceTournament.getName())
        + "_teams.csv");
    // write the teams out to a file
    try (Writer writer = Files.newBufferedWriter(teamsFile, Utilities.DEFAULT_CHARSET)) {
      try (CSVWriter csvWriter = new CSVWriter(writer)) {
        csvWriter.writeNext(new String[] { "team_name", "team_number", "affiliation", "award_group", "judging_group",
                                           "tournament" });
        final Map<Integer, TournamentTeam> sourceTeams = Queries.getTournamentTeams(testDataConnection,
                                                                                    sourceTournament.getTournamentID());
        for (final Map.Entry<Integer, TournamentTeam> entry : sourceTeams.entrySet()) {
          final TournamentTeam team = entry.getValue();

          csvWriter.writeNext(new String[] { team.getTeamName(), Integer.toString(team.getTeamNumber()),
                                             team.getOrganization(), team.getAwardGroup(), team.getJudgingGroup(),
                                             sourceTournament.getName() });
        }
      }
    }

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/");

    selenium.findElement(By.id("teams_file")).sendKeys(teamsFile.toAbsolutePath().toString());

    selenium.findElement(By.id("upload_teams")).click();

    IntegrationTestUtils.assertNoException(selenium);

    // skip past the filter page
    selenium.findElement(By.id("next")).click();
    IntegrationTestUtils.assertNoException(selenium);

    // team column selection
    new Select(selenium.findElement(By.name("TeamNumber"))).selectByValue("team_number");
    new Select(selenium.findElement(By.name("TeamName"))).selectByValue("team_name");
    new Select(selenium.findElement(By.name("Organization"))).selectByValue("affiliation");
    new Select(selenium.findElement(By.name("tournament"))).selectByValue("tournament");
    new Select(selenium.findElement(By.name("event_division"))).selectByValue("award_group");
    new Select(selenium.findElement(By.name("judging_station"))).selectByValue("judging_group");
    selenium.findElement(By.id("next")).click();
    IntegrationTestUtils.assertNoException(selenium);
    assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));

  }

  private void computeFinalScores(final WebDriver selenium) throws IOException, InterruptedException {
    // compute final scores
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/summarizePhase1.jsp");

    selenium.findElement(By.id("finish")).click();

    assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));
  }

  private void checkReports(final WebDriver selenium) throws IOException, SAXException, InterruptedException {
    // generate reports

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/CategorizedScores");

    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "report/CategoryScoresByJudge");

    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "report/FinalComputedScores"), "application/pdf", null);

    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "report/CategoryScoresByScoreGroup"), "application/pdf", null);

    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "report/PlayoffReport"), "application/pdf", null);
  }

  private void checkRankAndScores(final String testTournamentName) throws IOException, SAXException {
    // check ranking and scores
    final double scoreFP = 1E-1; // just check to one decimal place

    final String sqlTemplate = "SELECT FinalScores.TeamNumber, FinalScores.OverallScore" //
        + " FROM FinalScores, current_tournament_teams, Tournaments" //
        + " WHERE FinalScores.TeamNumber = current_tournament_teams.TeamNumber" //
        + " AND Tournaments.Name = '%s'"
        + " AND FinalScores.Tournament = Tournaments.tournament_id" //
        + " AND current_tournament_teams.event_division = '%s'" //
        + " ORDER BY FinalScores.OverallScore DESC";

    // division 1
    final int[] division1ExpectedRank = { 2636, 3127, 3439, 4462, 3125, 2116, 2104, 2113 };
    final double[] division1ExpectedScores = { 472.76, 423.58, 411.04, 378.04, 374.86, 346.63, 325.95, 310.61 };
    String division = "DivI/Gr4-6";

    final String div1Query = String.format(sqlTemplate, testTournamentName, division);
    final QueryHandler.ResultData div1Result = WebTestUtils.executeServerQuery(div1Query);

    int rank = 0;
    for (final Map<String, String> row : div1Result.getData()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("checkRankAndScores - row: "
            + row);
      }

      final int teamNumber = Integer.parseInt(row.get("teamnumber"));
      assertEquals(division1ExpectedRank[rank], teamNumber, "Division I Ranking is incorrect for rank: "
          + rank);
      final double score = Double.valueOf(row.get("overallscore"));
      assertEquals(division1ExpectedScores[rank], score, scoreFP, "Overall score incorrect for team: "
          + teamNumber);

      ++rank;
    }

    // division 2
    final int[] division2ExpectedRank = { 3208, 3061, 2863, 2110, 3063, 353, 3129, 2043 };
    final double[] division2ExpectedScores = { 546.78, 512.05, 426.02, 410.23, 407.15, 355.42, 350.14, 348.75 };
    division = "DivII/Gr7-9";

    final String div2Query = String.format(sqlTemplate, testTournamentName, division);
    final QueryHandler.ResultData div2Result = WebTestUtils.executeServerQuery(div2Query);

    rank = 0;
    for (final Map<String, String> row : div2Result.getData()) {
      final int teamNumber = Integer.parseInt(row.get("teamnumber"));
      assertEquals(division2ExpectedRank[rank], teamNumber, "Division II Ranking is incorrect for rank: "
          + rank);
      final double score = Double.valueOf(row.get("overallscore"));
      assertEquals(division2ExpectedScores[rank], score, scoreFP, "Overall score incorrect for team: "
          + teamNumber);

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
  private static void printPlayoffScoresheets(final String division)
      throws MalformedURLException, IOException, InterruptedException, SAXException {
    final WebClient conversation = WebTestUtils.getConversation();

    final Page indexResponse = WebTestUtils.loadPage(conversation, new WebRequest(new URL(TestUtils.URL_ROOT
        + "playoff/index.jsp")));
    assertTrue(indexResponse.isHtmlPage());
    final HtmlPage indexHtml = (HtmlPage) indexResponse;

    // find form named 'printable'
    HtmlForm form = indexHtml.getFormByName("printable");
    assertNotNull(form, "printable form not found");

    final String formSource = WebTestUtils.getPageSource(form.getPage());
    LOGGER.debug("Form source: "
        + formSource);

    // set division
    final HtmlSelect divisionSelect = indexHtml.getHtmlElementById("printable.division");
    final HtmlOption divisionOption = divisionSelect.getOptionByValue(division);
    divisionSelect.setSelectedAttribute(divisionOption, true);

    // click 'Display Brackets'
    final HtmlSubmitInput displayBrackets = form.getInputByValue("Display Brackets");
    final com.gargoylesoftware.htmlunit.WebRequest displayBracketsRequest = form.getWebRequest(displayBrackets);
    final Page displayResponse = WebTestUtils.loadPage(conversation, displayBracketsRequest);

    assertTrue(displayResponse.isHtmlPage());
    final HtmlPage displayHtml = (HtmlPage) displayResponse;

    // find form named 'printScoreSheets'
    form = displayHtml.getFormByName("printScoreSheets");
    assertNotNull(form, "printScoreSheets form not found");

    final HtmlCheckBoxInput printCheck = form.getInputByName("print1");
    printCheck.setChecked(true);

    // click 'Print scoresheets'
    final HtmlSubmitInput print = form.getInputByValue("Print scoresheets");
    final com.gargoylesoftware.htmlunit.WebRequest printRequest = form.getWebRequest(print);
    final Page printResponse = WebTestUtils.loadPage(conversation, printRequest);

    // check that result is PDF
    assertEquals("application/pdf", printResponse.getWebResponse().getContentType());

  }

  /**
   * Simulate entering subjective scores by pulling them out of testDataConn.
   *
   * @param testDataConn Where to get the test data from
   * @param challengeDocument the challenge descriptor
   * @throws SQLException
   * @throws SAXException
   * @throws InterruptedException
   */
  private void enterSubjectiveScores(final WebDriver selenium,
                                     final Connection testDataConn,
                                     final ChallengeDescription description,
                                     final Tournament sourceTournament,
                                     final Path outputDirectory)
      throws SQLException, IOException, MalformedURLException, ParseException, SAXException, InterruptedException {

    final Path subjectiveZip = outputDirectory.resolve(sanitizeFilename(sourceTournament.getName())
        + "_subjective-data.fll");

    IntegrationTestUtils.downloadFile(new URL(TestUtils.URL_ROOT
        + "admin/subjective-data.fll"), "application/zip", subjectiveZip);

    try {
      EventQueue.invokeAndWait(Errors.rethrow().wrap(() -> {
        enterSubjectiveScores(testDataConn, description, sourceTournament, subjectiveZip);

      }));
    } catch (final InvocationTargetException e) {
      LOGGER.error("Error entering subjective scores", e);
      fail("Got error entering subjective scores");
    }

    // upload scores
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/index.jsp");
    final WebElement fileInput = selenium.findElement(By.name("subjectiveFile"));
    fileInput.sendKeys(subjectiveZip.toAbsolutePath().toString());

    selenium.findElement(By.id("uploadSubjectiveFile")).click();

    assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.id("error")));
    assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("success")));

  }

  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", justification = "Need to specify category for table name")
  public void enterSubjectiveScores(final Connection testDataConn,
                                    final ChallengeDescription description,
                                    final Tournament sourceTournament,
                                    final Path subjectiveZip)
      throws IOException, SQLException, ParseException {
    final SubjectiveFrame subjective = new SubjectiveFrame();
    subjective.load(subjectiveZip.toFile());

    // insert scores into zip
    for (final SubjectiveScoreCategory subjectiveElement : description.getSubjectiveCategories()) {
      final String category = subjectiveElement.getName();
      final String title = subjectiveElement.getTitle();

      // find appropriate table model
      final TableModel tableModel = subjective.getTableModelForTitle(title);
      assertNotNull(tableModel);

      final int teamNumberColumn = findColumnByName(tableModel, "TeamNumber");
      assertTrue(teamNumberColumn >= 0, "Can't find TeamNumber column in subjective table model");
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Found team number column at "
            + teamNumberColumn);
      }

      try (final PreparedStatement prep = testDataConn.prepareStatement("SELECT * FROM "
          + category
          + " WHERE Tournament = ?")) {
        prep.setInt(1, sourceTournament.getTournamentID());

        try (final ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt("TeamNumber");

            // find row number in table
            int rowIndex = -1;
            for (int rowIdx = 0; rowIdx < tableModel.getRowCount(); ++rowIdx) {
              final Object teamNumberRaw = tableModel.getValueAt(rowIdx, teamNumberColumn);
              assertNotNull(teamNumberRaw);
              final int value = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(teamNumberRaw.toString()).intValue();

              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Checking if "
                    + teamNumber
                    + " equals "
                    + value
                    + " raw: "
                    + teamNumberRaw
                    + "? "
                    + (value == teamNumber)
                    + " rowIdx: "
                    + rowIdx
                    + " numRows: "
                    + tableModel.getRowCount());
              }

              if (value == teamNumber) {
                rowIndex = rowIdx;
                break;
              }
            }
            assertTrue(rowIndex >= 0, "Can't find team "
                + teamNumber
                + " in subjective table model");

            if (rs.getBoolean("NoShow")) {
              // find column for no show
              final int columnIndex = findColumnByName(tableModel, "No Show");
              assertTrue(columnIndex >= 0, "Can't find No Show column in subjective table model");
              tableModel.setValueAt(Boolean.TRUE, rowIndex, columnIndex);
            } else {
              for (final AbstractGoal goalElement : subjectiveElement.getGoals()) {
                if (!goalElement.isComputed()) {
                  final String goalName = goalElement.getName();
                  final String goalTitle = goalElement.getTitle();

                  // find column index for goal and call set
                  final int columnIndex = findColumnByName(tableModel, goalTitle);
                  assertTrue(columnIndex >= 0, "Can't find "
                      + goalTitle
                      + " column in subjective table model");
                  final int value = rs.getInt(goalName);
                  tableModel.setValueAt(Integer.valueOf(value), rowIndex, columnIndex);
                }
              }
            } // not NoShow
          } // foreach score
        } // try ResultSet
      } // try PreparedStatement
    } // foreach category
    subjective.save();
  }

  /**
   * Enter a teams performance score. Data is pulled from testDataConn and
   * pushed to the website.
   *
   * @param selenium TODO
   * @throws InterruptedException
   */
  private void enterPerformanceScore(final WebDriver selenium,
                                     final Connection testDataConn,
                                     final PerformanceScoreCategory performanceElement,
                                     final Tournament sourceTournament,
                                     final int runNumber,
                                     final int teamNumber)
      throws SQLException, IOException, MalformedURLException, ParseException, InterruptedException {

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Setting score for "
          + teamNumber
          + " run: "
          + runNumber);
    }

    try (
        final PreparedStatement prep = testDataConn.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ? AND TeamNumber = ?")) {
      prep.setInt(1, sourceTournament.getTournamentID());
      prep.setInt(2, runNumber);
      prep.setInt(3, teamNumber);

      try (final ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          if (rs.getBoolean("BYE")) {
            LOGGER.info("Run is a bye, not entering a score");
            return;
          }

          // need to get the score entry form
          IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
              + "scoreEntry/select_team.jsp");

          // select this entry
          new Select(selenium.findElement(By.id("select-teamnumber"))).selectByValue(String.valueOf(teamNumber));

          // submit the page
          selenium.findElement(By.id("enter_submit")).click();

          assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.name("error")), "Errors: ");

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
                      + name
                      + " min: "
                      + min
                      + " max: "
                      + max);
                }

                if (goal.isEnumerated()) {
                  final String valueStr = rs.getString(name);
                  final String radioID = ScoreEntry.getIDForEnumRadio(name, valueStr);
                  selenium.findElement(By.id(radioID)).click();
                } else if (goal.isYesNo()) {
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

            // check that the submit button is active
            assertTrue(selenium.findElement(By.id("submit")).isEnabled(),
                       "Submit button is not enabled, invalid score entered");

            selenium.findElement(By.id("submit")).click();
          } // not NoShow

          Thread.sleep(50);

          final Alert confirmScoreChange = selenium.switchTo().alert();
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Confirmation text: "
                + confirmScoreChange.getText());
          }
          confirmScoreChange.accept();

          assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.name("error")), "Errors: ");
        } else {
          fail("Cannot find scores for "
              + teamNumber
              + " run "
              + runNumber);
        }
      } // try ResultSet
    } // try PreparedStatement

  }

  /**
   * Enter a teams performance score. Data is pulled from testDataConn and
   * pushed to the website.
   *
   * @param selenium TODO
   * @throws InterruptedException
   */
  private void verifyPerformanceScore(final WebDriver selenium,
                                      final Connection testDataConn,
                                      final PerformanceScoreCategory performanceElement,
                                      final Tournament sourceTournament,
                                      final int runNumber,
                                      final int teamNumber)
      throws SQLException, IOException, MalformedURLException, ParseException, InterruptedException {
    final String selectTeamPage = TestUtils.URL_ROOT
        + "scoreEntry/select_team.jsp";

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Verify score for "
          + teamNumber
          + " run: "
          + runNumber);
    }

    try (
        final PreparedStatement prep = testDataConn.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ? AND TeamNumber = ?")) {
      prep.setInt(1, sourceTournament.getTournamentID());
      prep.setInt(2, runNumber);
      prep.setInt(3, teamNumber);

      try (final ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          if (rs.getBoolean("NoShow")) {
            // no shows don't need verifying
            return;
          } else {
            // need to get the score entry form
            IntegrationTestUtils.loadPage(selenium, selectTeamPage);

            new Select(selenium.findElement(By.id("select-verify-teamnumber"))).selectByValue(teamNumber
                + "-"
                + runNumber);

            // submit the page
            selenium.findElement(By.id("verify_submit")).click();
            Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

            // walk over challenge descriptor to get all element names and then
            // use the values from rs
            for (final AbstractGoal element : performanceElement.getGoals()) {
              if (!element.isComputed()) {
                final Goal goal = (Goal) element;
                final String name = goal.getName();

                if (goal.isEnumerated()) {
                  // need check if the right radio button is selected
                  final String value = rs.getString(name);

                  final String formValue = selenium.findElement(By.name(ScoreEntry.getElementNameForYesNoDisplay(name)))
                                                   .getAttribute("value");
                  assertNotNull(formValue, "Null value for goal: "
                      + name);

                  assertEquals(value.toLowerCase(), formValue.toLowerCase(), "Wrong enum selected for goal: "
                      + name);
                } else if (goal.isYesNo()) {
                  final String formValue = selenium.findElement(By.name(ScoreEntry.getElementNameForYesNoDisplay(name)))
                                                   .getAttribute("value");
                  assertNotNull(formValue, "Null value for goal: "
                      + name);

                  // yes/no
                  final int value = rs.getInt(name);
                  final String expectedValue;
                  if (value == 0) {
                    expectedValue = "no";
                  } else {
                    expectedValue = "yes";
                  }
                  assertEquals(expectedValue.toLowerCase(), formValue.toLowerCase(), "Wrong value for goal: "
                      + name);
                } else {
                  final String formValue = selenium.findElement(By.name(name)).getAttribute("value");
                  assertNotNull(formValue, "Null value for goal: "
                      + name);

                  final int value = rs.getInt(name);
                  final int formValueInt = Integer.parseInt(formValue);
                  assertEquals(value, formValueInt, "Wrong value for goal: "
                      + name);
                }
              } // !computed
            } // foreach goal

            // Set the verified field to yes
            selenium.findElement(By.id("Verified_yes")).click();

            // submit score
            selenium.findElement(By.id("submit")).click();
          } // not NoShow

          Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

          LOGGER.debug("Checking for an alert");

          // confirm selection, not going to bother checking the text
          final Alert confirmScoreChange = selenium.switchTo().alert();
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Confirmation text: "
                + confirmScoreChange.getText());
          }
          confirmScoreChange.accept();

          // give the web server a chance to catch up
          Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

          // check for errors
          // Gives trouble too often
          // Assert.assertEquals(selectTeamPage, selenium.getCurrentUrl());
          assertTrue(selenium.getPageSource().contains("Unverified Runs"),
                     "Error submitting form, not on select team page url: "
                         + selenium.getCurrentUrl());

        } else {
          fail("Cannot find scores for "
              + teamNumber
              + " run "
              + runNumber);
        }
      } // try ResultSet
    } // try PreparedStatement

  }

  /**
   * Check display pages that aren't shown otherwise.
   *
   * @param selenium TODO
   * @throws IOException
   * @throws InterruptedException
   */
  private void checkDisplays(final WebDriver selenium) throws IOException, InterruptedException {
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

  /**
   * Create a string that's a valid file name.
   */
  private static String sanitizeFilename(final String str) {
    if (null == str
        || "".equals(str)) {
      return "NULL";
    } else {
      String ret = str;
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");

      return ret;
    }
  }

  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9_]");

}
