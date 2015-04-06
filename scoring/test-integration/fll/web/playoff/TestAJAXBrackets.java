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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.web.IntegrationTestUtils;
import fll.web.WebWindow;
import fll.xml.BracketSortType;

/**
 * Test the AJAX Brackets
 */
public class TestAJAXBrackets {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static String JS_EVAL_TIMEOUT = "10000";

  private WebDriver selenium;

  private JavascriptExecutor seleniumJS;

  @Before
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    selenium = IntegrationTestUtils.createWebDriver();

    if (selenium instanceof JavascriptExecutor) {
      seleniumJS = (JavascriptExecutor) selenium;
    } else {
      throw new RuntimeException("WebDriver is not capable of Javascript execution");
    }

  }

  @After
  public void tearDown() {
    selenium.quit();
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

      // change num seeding rounds
      final Select seedingRoundsSelection = new Select(selenium.findElement(By.name("seedingRounds")));
      seedingRoundsSelection.selectByValue("0");
      selenium.findElement(By.name("changeSeedingRounds")).click();

      // init brackets
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "playoff");

      final String division = "1";

      IntegrationTestUtils.initializePlayoffsForDivision(selenium, division, BracketSortType.ALPHA_TEAM);

      // open brackets
      final WebWindow bracketsWindow = new WebWindow(selenium, TestUtils.URL_ROOT
          + "playoff/remoteControlBrackets.jsp?scroll=false");

      // open score entry
      final WebWindow scoreEntryWindow = new WebWindow(selenium, TestUtils.URL_ROOT
          + "scoreEntry/select_team.jsp");

      // give windows a little time to get their bearings
      // selenium.runScript("var timerRan = false;setTimeout('timerRan=true;', 5000);");
      // selenium.waitForCondition("window.timerRan", JS_EVAL_TIMEOUT);

      // assign tables for the scoresheets
      final WebWindow scoresheetWindow = new WebWindow(selenium, TestUtils.URL_ROOT
          + "playoff/scoregenbrackets.jsp?division=" + division + "&firstRound=1&lastRound=7");
      selenium.switchTo().window(scoresheetWindow.getWindowHandle());
      selenium.findElement(By.name("print1")).click();
      selenium.findElement(By.name("tableA1")).sendKeys("Blue 1");
      selenium.findElement(By.name("tableB1")).sendKeys("Table 2");
      selenium.findElement(By.id("print_scoresheets")).click();
      
      // check for a blue cell
      selenium.switchTo().window(bracketsWindow.getWindowHandle());
      // TODO: I can't find a way selenium is
      // OK with that checks for a present CSS property, nor a string of JS
      // selenium is OK with to check for the element.
      // selenium.waitForCondition("window.document.getElementsByClassName('table_assignment')[0].style.backgroundColor=='blue'",
      // JS_EVAL_TIMEOUT); // > 1 element with a style attrib that contains the
      // string 'blue'

      // I slow down selenium for the AJAX functions
      // as they don't take that long, selenium
      // spends a lot less
      // time between entering data and checking for
      // it than it can keep up with.
      // JPS may need to add some wait calls to replace this
      // selenium.setSpeed("300");

      // enter unverified score for team 1
      selenium.switchTo().window(scoreEntryWindow.getWindowHandle());
      enterScore("4", 1);

      selenium.switchTo().window(bracketsWindow.getWindowHandle());
      final String scoreTextBefore = selenium.findElement(By.id("9-1")).getText();
      // final String scoreTextBefore =
      // String.valueOf(seleniumJS.executeScript("window.document.getElementById('9-1').innerHTML"));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Score text before: "
            + scoreTextBefore);
      }
      Assert.assertFalse("Should not find score yet '"
          + scoreTextBefore + "'", scoreTextBefore.contains("Score:"));

      // verify
      selenium.switchTo().window(scoreEntryWindow.getWindowHandle());

      final Select verifySelect = new Select(selenium.findElement(By.id("select-verify-teamnumber")));
      verifySelect.selectByValue("4-1");
      selenium.findElement(By.id("verify_submit")).click();

      selenium.findElement(By.id("Verified_yes")).click();
      selenium.findElement(By.id("submit")).click();

      final Alert confirmVerifyChange = selenium.switchTo().alert();
      LOGGER.info("Confirmation text: "
          + confirmVerifyChange.getText());
      confirmVerifyChange.accept();

      // give the web server a chance to catch up
      Thread.sleep(1000);

      selenium.switchTo().window(bracketsWindow.getWindowHandle());
      // run the javascript to refresh everything
      seleniumJS.executeScript("window.iterate();");

      // give the web server a chance to catch up
      Thread.sleep(30000);

      final String scoreTextAfter = selenium.findElement(By.id("9-1")).getText();
      // final String scoreTextAfter =
      // String.valueOf(seleniumJS.executeScript("window.document.getElementById('1-2').innerHTML"));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Score text after: "
            + scoreTextAfter);
      }
      Assert.assertTrue("Should find score in '"
          + scoreTextAfter + "'", scoreTextAfter.contains("Score:"));

    } catch (final IOException e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final AssertionError e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  private void enterScore(final String team,
                          final int score) {
    final Select teamSelect = new Select(selenium.findElement(By.id("select-teamnumber")));
    teamSelect.selectByValue(team);
    selenium.findElement(By.id("enter_submit")).click();

    for (int i = 0; i < score; i++) {
      selenium.findElement(By.id("inc_score_1")).click();
    }
    selenium.findElement(By.id("submit")).click();

    Alert confirmScoreChange = null;
    final int maxAttempts = 5;
    int attempt = 0;
    while (null == confirmScoreChange
        && attempt <= maxAttempts) {
      try {
        confirmScoreChange = selenium.switchTo().alert();
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
