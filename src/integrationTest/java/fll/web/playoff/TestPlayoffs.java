/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.web.IntegrationTestUtils;

/**
 * Test things about the playoffs.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class TestPlayoffs {

  /**
   * Test that when trying to enter a score for a team that hasn't advanced to a
   * particular playoff round results in an error message and the user being
   * sent back to the select team page.
   *
   * @param selenium the browser driver
   * @param seleniumWait used to wait for elements
   * @throws IOException if there is an error interacting with the browser
   * @throws InterruptedException if there is an error waiting for an element
   */
  @Test
  public void testNotAdvanced(final WebDriver selenium,
                              final WebDriverWait seleniumWait)
      throws IOException, InterruptedException {
    try {
      // initialize database using simple challenge descriptor that just has 1
      // goal from 1 - 100
      final InputStream challengeStream = TestPlayoffs.class.getResourceAsStream("data/simple.xml");
      IntegrationTestUtils.initializeDatabase(selenium, seleniumWait, challengeStream);

      IntegrationTestUtils.setTournament(selenium, seleniumWait, GenerateDB.DUMMY_TOURNAMENT_NAME);

      IntegrationTestUtils.setRunningHeadToHead(selenium, seleniumWait, true);

      // add 4 teams to dummy tournament
      for (int teamNumber = 0; teamNumber < 4; ++teamNumber) {
        IntegrationTestUtils.addTeam(selenium, seleniumWait, teamNumber, "team "
            + teamNumber, "org", "1", GenerateDB.DUMMY_TOURNAMENT_NAME);
      }

      // set seeding rounds to 1
      IntegrationTestUtils.changeNumSeedingRounds(selenium, seleniumWait, 1);

      // enter 1 score for all teams equal to their team number
      for (int teamNumber = 0; teamNumber < 4; ++teamNumber) {
        enterTeamScore(selenium, seleniumWait, teamNumber);

        assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.name("error")), "Errors: ");
      }

      // initialize playoffs
      IntegrationTestUtils.initializePlayoffsForAwardGroup(selenium, seleniumWait, "1");

      IntegrationTestUtils.assertNoException(selenium);

      // enter score for teams 3 and 0 with 3 winning
      enterTeamScore(selenium, seleniumWait, 3);
      assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.name("error")), "Errors: ");
      enterTeamScore(selenium, seleniumWait, 0);
      assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.name("error")), "Errors: ");

      // enter score for teams 2 and 1 with 1 winning
      enterTeamScore(selenium, seleniumWait, 2);
      assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.name("error")), "Errors: ");
      enterTeamScore(selenium, seleniumWait, 1);
      assertFalse(IntegrationTestUtils.isElementPresent(selenium, By.name("error")), "Errors: ");

      // attempt to enter score for 0
      IntegrationTestUtils.loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
          + "scoreEntry/select_team.jsp");
      final Select teamSelect = new Select(selenium.findElement(By.id("select-teamnumber")));
      teamSelect.selectByValue("0");
      selenium.findElement(By.id("enter_submit")).click();

      // check for error message
      assertTrue(IntegrationTestUtils.isElementPresent(selenium, By.id("error-not-advanced")), "Should have errors");

      // final String text = selenium.getPageSource();
      // Assert.assertTrue("Should have errors",
      // text.contains("Selected team has not advanced to the next head to head
      // round."));
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  private void enterTeamScore(final WebDriver selenium,
                              final WebDriverWait seleniumWait,
                              final int teamNumber)
      throws IOException, InterruptedException {
    IntegrationTestUtils.loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
        + "scoreEntry/select_team.jsp");

    final Select teamSelect = new Select(selenium.findElement(By.id("select-teamnumber")));
    teamSelect.selectByValue(String.valueOf(teamNumber));

    selenium.findElement(By.id("enter_submit")).click();

    selenium.findElement(By.name("g1")).sendKeys(String.valueOf(teamNumber));
    selenium.findElement(By.id("Verified_yes_span")).click();

    IntegrationTestUtils.submitPerformanceScore(selenium, seleniumWait);
  }
}
