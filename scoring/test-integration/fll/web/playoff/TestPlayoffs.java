/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.thoughtworks.selenium.SeleneseTestCase;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.web.IntegrationTestUtils;

/**
 * Test things about the playoffs.
 */
public class TestPlayoffs extends SeleneseTestCase {

  @Override
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    super.setUp("http://localhost:9080/setup");
  }

  /**
   * Test that when trying to enter a score for a team that hasn't advanced to a
   * particular playoff round results in an error message and the user being
   * sent back to the select team page.
   * 
   * @throws IOException
   */
  @Test
  public void testNotAdvanced() throws IOException {
    // initialize database using simple challenge descriptor that just has 1
    // goal from 1 - 100
    final InputStream challengeStream = TestPlayoffs.class.getResourceAsStream("data/simple.xml");
    IntegrationTestUtils.initializeDatabase(selenium, challengeStream, true);

    IntegrationTestUtils.setTournament(selenium, GenerateDB.DUMMY_TOURNAMENT_NAME);

    // add 4 teams to dummy tournament
    for (int teamNumber = 0; teamNumber < 4; ++teamNumber) {
      IntegrationTestUtils.addTeam(selenium, teamNumber, "team "
          + teamNumber, "org", "dummy", "1", GenerateDB.DUMMY_TOURNAMENT_NAME);
    }

    // set seeding rounds to 1
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "admin/index.jsp");
    selenium.select("id=changeSeedingRounds", "1");
    selenium.click("changeSeedingRounds");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

    // enter 1 score for all teams equal to their team number
    for (int teamNumber = 0; teamNumber < 4; ++teamNumber) {
      enterTeamScore(teamNumber);

      Assert.assertFalse("Errors: ", selenium.isElementPresent("name=error"));
    }

    // initialize playoffs
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "playoff");
    selenium.select("xpath=//form[@name='initialize']//select[@name='division']", "value=1");
    selenium.click("id=initialize_brackets");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
    Assert.assertFalse(selenium.isTextPresent("Exception"));

    // enter score for teams 3 and 0 with 3 winning
    enterTeamScore(4);
    Assert.assertFalse("Errors: ", selenium.isElementPresent("name=error"));
    enterTeamScore(1);
    Assert.assertFalse("Errors: ", selenium.isElementPresent("name=error"));


    // enter score for teams 2 and 1 with 1 winning
    enterTeamScore(3);
    Assert.assertFalse("Errors: ", selenium.isElementPresent("name=error"));
    enterTeamScore(2);
    Assert.assertFalse("Errors: ", selenium.isElementPresent("name=error"));


    // attempt to enter score for 1
    enterTeamScore(1);
    
    // check for error message
    Assert.assertTrue("Should have errors", selenium.isElementPresent("name=error"));
  }

  private void enterTeamScore(final int teamNumber) throws IOException {
    IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
        + "scoreEntry/select_team.jsp");
    selenium.select("xpath=//form[@name='selectTeam']//select[@name='TeamNumber']", "value="
        + teamNumber);
    selenium.click("id=enter_submit");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

    selenium.type("g1", String.valueOf(teamNumber));
    selenium.click("id=Verified_yes");

    selenium.click("id=submit");

    selenium.getConfirmation();

    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
  }
}
