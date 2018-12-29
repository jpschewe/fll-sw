/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.web.IntegrationTestUtils;
import fll.xml.BracketSortType;

/**
 * Test the AJAX Brackets
 */
public class TestAJAXBrackets {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static String JS_EVAL_TIMEOUT = "10000";

  private WebDriver selenium;

  private WebDriver bracketsWindow;

  private WebDriver scoreEntryWindow;

  private WebDriver scoresheetWindow;

  @Before
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    selenium = IntegrationTestUtils.createWebDriver();

    bracketsWindow = IntegrationTestUtils.createWebDriver();

    scoreEntryWindow = IntegrationTestUtils.createWebDriver();

    scoresheetWindow = IntegrationTestUtils.createWebDriver();
  }

  @After
  public void tearDown() {
    selenium.quit();
    bracketsWindow.quit();
    scoreEntryWindow.quit();
    scoresheetWindow.quit();
  }

  @Test
  public void testAJAXBracketsInFull() throws IOException, SAXException, InterruptedException {
    try {
      // Setup our playoffs
      final InputStream challenge = TestAJAXBrackets.class.getResourceAsStream("data/very-simple.xml");
      IntegrationTestUtils.initializeDatabase(selenium, challenge);
      IntegrationTestUtils.setTournament(selenium, GenerateDB.DUMMY_TOURNAMENT_NAME);
      for (int i = 1; i < 6; ++i) {
        IntegrationTestUtils.addTeam(selenium, i, String.valueOf(i), "htk", "1", GenerateDB.DUMMY_TOURNAMENT_NAME);
      }
      // table labels
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "admin/tables.jsp");

      selenium.findElement(By.name("SideA0")).sendKeys("Blue 1");
      selenium.findElement(By.name("SideB0")).sendKeys("Table 2");
      selenium.findElement(By.id("finished")).click();

      final int tournamentId = IntegrationTestUtils.getCurrentTournamentId(selenium);
      IntegrationTestUtils.changeNumSeedingRounds(selenium, tournamentId, 0);

      // init brackets
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "playoff");

      final String division = "1";

      IntegrationTestUtils.initializePlayoffsForAwardGroup(selenium, division, BracketSortType.ALPHA_TEAM);

      // set display to show the head to head brackets
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT + "admin/remoteControl.jsp");
      selenium.findElement(By.cssSelector("[type='radio'][name='remotePage'][value='playoffs']")).click();
      selenium.findElement(By.name("submit")).click();
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

      // open brackets
      IntegrationTestUtils.loadPage(bracketsWindow, TestUtils.URL_ROOT
          + "playoff/remoteControlBrackets.jsp?scroll=false");

      // open score entry
      IntegrationTestUtils.loadPage(scoreEntryWindow, TestUtils.URL_ROOT
          + "scoreEntry/select_team.jsp");

      // give windows a little time to get their bearings
      // selenium.runScript("var timerRan = false;setTimeout('timerRan=true;',
      // 5000);");
      // selenium.waitForCondition("window.timerRan", JS_EVAL_TIMEOUT);

      // assign tables for the scoresheets
      IntegrationTestUtils.loadPage(scoresheetWindow, TestUtils.URL_ROOT
          + "playoff/scoregenbrackets.jsp?division=" + division + "&firstRound=1&lastRound=7");

      scoresheetWindow.findElement(By.name("print1")).click();
      scoresheetWindow.findElement(By.name("tableA1")).sendKeys("Blue 1");
      scoresheetWindow.findElement(By.name("tableB1")).sendKeys("Table 2");
      scoresheetWindow.findElement(By.id("print_scoresheets")).click();

      // check for a blue cell
      // TODO: I can't find a way selenium is
      // OK with that checks for a present CSS property, nor a string of JS
      // selenium is OK with to check for the element.
      // bracketsWindow.waitForCondition("window.document.getElementsByClassName('table_assignment')[0].style.backgroundColor=='blue'",
      // JS_EVAL_TIMEOUT); // > 1 element with a style attrib that contains the
      // string 'blue'

      // I slow down selenium for the AJAX functions
      // as they don't take that long, selenium
      // spends a lot less
      // time between entering data and checking for
      // it than it can keep up with.
      // JPS may need to add some wait calls to replace this
      // bracketsWindow.setSpeed("300");

      // enter unverified score for team 4
      enterScore(scoreEntryWindow, "4", 1);

      final String scoreTextBefore = bracketsWindow.findElement(By.id("0-3-1")).getText();
      // final String scoreTextBefore =
      // String.valueOf(bracketsWindowJS.executeScript("window.document.getElementById('0-3-1').innerHTML"));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Score text before: "
            + scoreTextBefore);
      }
      Assert.assertFalse("Should not find score yet '"
          + scoreTextBefore + "'", scoreTextBefore.contains("Score:"));

      // verify
      final Select verifySelect = new Select(scoreEntryWindow.findElement(By.id("select-verify-teamnumber")));
      verifySelect.selectByValue("4-1");
      scoreEntryWindow.findElement(By.id("verify_submit")).click();

      scoreEntryWindow.findElement(By.id("Verified_yes")).click();
      scoreEntryWindow.findElement(By.id("submit")).click();

      final Alert confirmVerifyChange = scoreEntryWindow.switchTo().alert();
      LOGGER.info("Confirmation text: "
          + confirmVerifyChange.getText());
      confirmVerifyChange.accept();

      // give the web server a chance to catch up
      Thread.sleep(30000);

      final String scoreTextAfter = bracketsWindow.findElement(By.id("0-3-1")).getText();
      // final String scoreTextAfter =
      // String.valueOf(seleniumJS.executeScript("window.document.getElementById('0-3-1').innerHTML"));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Score text after: "
            + scoreTextAfter);
      }
      Assert.assertTrue("Should find score in '"
          + scoreTextAfter + "'", scoreTextAfter.contains("Score:"));

    } catch (final IOException | RuntimeException | AssertionError e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot("main", selenium);
      IntegrationTestUtils.storeScreenshot("brackets", selenium);
      IntegrationTestUtils.storeScreenshot("scoreEntry", selenium);
      IntegrationTestUtils.storeScreenshot("scoreSheet", selenium);
      throw e;
    }
  }

  private void enterScore(final WebDriver webDriver,
                          final String team,
                          final int score) {
    final Select teamSelect = new Select(webDriver.findElement(By.id("select-teamnumber")));
    teamSelect.selectByValue(team);
    webDriver.findElement(By.id("enter_submit")).click();

    for (int i = 0; i < score; i++) {
      webDriver.findElement(By.id("inc_score_1")).click();
    }
    webDriver.findElement(By.id("submit")).click();

    Alert confirmScoreChange = null;
    final int maxAttempts = 5;
    int attempt = 0;
    while (null == confirmScoreChange
        && attempt <= maxAttempts) {
      try {
        confirmScoreChange = webDriver.switchTo().alert();
        LOGGER.info("Confirmation text: "
            + confirmScoreChange.getText());
        confirmScoreChange.accept();
      } catch (final NoAlertPresentException ex) {
        ++attempt;
        confirmScoreChange = null;

        if (attempt >= maxAttempts) {
          throw ex;
        } else {
          LOGGER.warn("Trouble finding alert, trying again", ex);
        }
      }
    }
  }
}
