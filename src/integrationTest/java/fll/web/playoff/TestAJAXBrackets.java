/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.web.IntegrationTestUtils;
import fll.xml.BracketSortType;

/**
 * Test the AJAX Brackets
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class TestAJAXBrackets {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  public static final String JS_EVAL_TIMEOUT = "10000";

  private WebDriver bracketsWindow;

  private WebDriver scoreEntryWindow;

  private WebDriverWait scoreEntryWait;

  private WebDriver scoresheetWindow;

  @BeforeEach
  public void setUp() throws Exception {
    bracketsWindow = IntegrationTestUtils.createWebDriver();

    scoreEntryWindow = IntegrationTestUtils.createWebDriver();
    scoreEntryWait = new WebDriverWait(scoreEntryWindow, IntegrationTestUtils.WAIT_FOR_ELEMENT.getSeconds());

    scoresheetWindow = IntegrationTestUtils.createWebDriver();
  }

  @AfterEach
  public void tearDown() {
    if (null != bracketsWindow) {
      bracketsWindow.quit();
    }
    if (null != scoreEntryWindow) {
      scoreEntryWindow.quit();
    }
    if (null != scoresheetWindow) {
      scoresheetWindow.quit();
    }
  }

  @Test
  public void testAJAXBracketsInFull(final WebDriver selenium,
                                     final WebDriverWait seleniumWait)
      throws IOException, SAXException, InterruptedException {
    try {
      // Setup our playoffs
      final InputStream challenge = TestAJAXBrackets.class.getResourceAsStream("data/very-simple.xml");
      IntegrationTestUtils.initializeDatabase(selenium, seleniumWait, challenge);
      IntegrationTestUtils.setTournament(selenium, GenerateDB.DUMMY_TOURNAMENT_NAME);
      for (int i = 1; i < 6; ++i) {
        IntegrationTestUtils.addTeam(selenium, seleniumWait, i, String.valueOf(i), "htk", "1",
                                     GenerateDB.DUMMY_TOURNAMENT_NAME);
      }
      // table labels
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "admin/tables.jsp");

      selenium.findElement(By.name("SideA0")).sendKeys("Blue 1");
      selenium.findElement(By.name("SideB0")).sendKeys("Table 2");
      selenium.findElement(By.id("finished")).click();

      IntegrationTestUtils.changeNumSeedingRounds(selenium, 0);

      IntegrationTestUtils.setRunningHeadToHead(selenium, seleniumWait, true);

      // init brackets
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "playoff");

      final String division = "1";

      IntegrationTestUtils.initializePlayoffsForAwardGroup(selenium, seleniumWait, division,
                                                           BracketSortType.ALPHA_TEAM);

      // set display to show the head to head brackets
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "admin/remoteControl.jsp");
      selenium.findElement(By.cssSelector("[type='radio'][name='remotePage'][value='playoffs']")).click();
      selenium.findElement(By.name("submit_data")).click();
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
          + "playoff/scoregenbrackets.jsp?division="
          + division
          + "&firstRound=1&lastRound=7");

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
      enterScore(scoreEntryWindow, scoreEntryWait, "4", 1);

      final String scoreTextBefore = bracketsWindow.findElement(By.id("0-3-1")).getText();
      // final String scoreTextBefore =
      // String.valueOf(bracketsWindowJS.executeScript("window.document.getElementById('0-3-1').innerHTML"));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Score text before: "
            + scoreTextBefore);
      }
      assertFalse(scoreTextBefore.contains("Score:"), "Should not find score yet '"
          + scoreTextBefore
          + "'");

      // verify
      final Select verifySelect = new Select(scoreEntryWindow.findElement(By.id("select-verify-teamnumber")));
      boolean found = false;
      for (final WebElement option : verifySelect.getOptions()) {
        final String value = option.getAttribute("value");
        if (value.startsWith("4-")) {
          verifySelect.selectByValue(value);
          found = true;
          break;
        }
      }
      if (!found) {
        fail("Unable to find verification for team 4");
      }
      scoreEntryWindow.findElement(By.id("verify_submit")).click();

      scoreEntryWindow.findElement(By.id("Verified_yes")).click();

      IntegrationTestUtils.submitPerformanceScore(scoreEntryWindow, scoreEntryWait);

      // give the web server a chance to catch up
      Thread.sleep(30000);

      final String scoreTextAfter = bracketsWindow.findElement(By.id("0-3-1")).getText();
      // final String scoreTextAfter =
      // String.valueOf(seleniumJS.executeScript("window.document.getElementById('0-3-1').innerHTML"));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Score text after: "
            + scoreTextAfter);
      }
      assertTrue(scoreTextAfter.contains("Score:"), "Should find score in '"
          + scoreTextAfter
          + "'");

    } catch (final IOException | RuntimeException | AssertionError e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot("main", selenium);
      IntegrationTestUtils.storeScreenshot("brackets", bracketsWindow);
      IntegrationTestUtils.storeScreenshot("scoreEntry", scoreEntryWindow);
      IntegrationTestUtils.storeScreenshot("scoreSheet", scoresheetWindow);
      throw e;
    }
  }

  private void enterScore(final WebDriver webDriver,
                          final WebDriverWait webDriverWait,
                          final String team,
                          final int score) {
    final Select teamSelect = new Select(webDriver.findElement(By.id("select-teamnumber")));
    teamSelect.selectByValue(team);
    webDriver.findElement(By.id("enter_submit")).click();

    for (int i = 0; i < score; i++) {
      webDriver.findElement(By.id("inc_score_1")).click();
    }

    IntegrationTestUtils.submitPerformanceScore(webDriver, webDriverWait);
  }
}
