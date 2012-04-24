/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.thoughtworks.selenium.SeleneseTestBase;
import com.thoughtworks.selenium.Selenium;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.web.IntegrationTestUtils;

/**
 * Test the AJAX Brackets
 */
public class TestAJAXBrackets extends SeleneseTestBase {

  public static String JS_EVAL_TIMEOUT = "10000";

  @Before
  @Override
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    super.setUp(TestUtils.URL_ROOT
        + "setup");
  }

  @Test
  public void testAJAXBracketsInFull() throws IOException, SAXException {
    try {
      // Setup our playoffs
      final InputStream challenge = TestAJAXBrackets.class.getResourceAsStream("data/very-simple.xml");
      IntegrationTestUtils.initializeDatabase(selenium, challenge, true);
      IntegrationTestUtils.setTournament(selenium, GenerateDB.DUMMY_TOURNAMENT_NAME);
      for (int i = 1; i < 6; ++i) {
        IntegrationTestUtils.addTeam(selenium, i, ""
            + i, "htk", "1", GenerateDB.DUMMY_TOURNAMENT_NAME);
      }
      // table labels
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "admin/tables.jsp");
      selenium.type("name=SideA0", "Blue 1");
      selenium.type("name=SideB0", "Table 2");
      selenium.click("id=finished");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      
      // change num seeding rounds
      selenium.select("xpath=//form[@id='changeSeedingRounds']//select[@name='seedingRounds']", "value=0");
      selenium.click("name=changeSeedingRounds");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      // init brackets
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "playoff");
      selenium.select("xpath=//form[@name='initialize']//select[@name='division']", "value=1");
      selenium.click("id=initialize_brackets");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      Assert.assertFalse(selenium.isTextPresent("Exception"));

      // open brackets
      selenium.openWindow(TestUtils.URL_ROOT
          + "playoff/remoteControlBrackets.jsp?scroll=false", "brackets");
      selenium.waitForPopUp("brackets", IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      // open score entry
      selenium.openWindow(TestUtils.URL_ROOT
          + "scoreEntry/select_team.jsp", "scoreentry");
      selenium.waitForPopUp("scoreentry", IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      // give windows a little time to get their bearings
      selenium.runScript("var timerRan = false;setTimeout('timerRan=true;', 5000);");
      selenium.waitForCondition("window.timerRan", JS_EVAL_TIMEOUT);
      // Use HTTPUnit to assign table labels instead of selenium
      WebConversation wc = new WebConversation();
      PostMethodWebRequest tableLabels = new PostMethodWebRequest(TestUtils.URL_ROOT
          + "playoff/ScoresheetServlet");
      tableLabels.setParameter("numMatches", "2");
      tableLabels.setParameter("print1", "1");
      tableLabels.setParameter("teamA1", "1");
      tableLabels.setParameter("teamB1", "2");
      tableLabels.setParameter("round1", "1");
      tableLabels.setParameter("tableA1", "Blue 1");
      tableLabels.setParameter("tableB1", "Table 2");
      wc.getResponse(tableLabels);

      // check for a blue cell
      selenium.selectWindow("brackets"); // TODO: I can't find a way selenium is
                                         // OK with that checks for a present
                                         // CSS
                                         // property, nor a string of JS
                                         // selenium
                                         // is OK with to check for the element.
      // selenium.waitForCondition("window.document.getElementsByClassName('table_assignment')[0].style.backgroundColor=='blue'",
      // JS_EVAL_TIMEOUT); // > 1 element with a style attrib that contains the
      // string 'blue'

      selenium.setSpeed("300"); // I slow down selenium for the AJAX functions
                                // as
                                // while they don't take that long, selenium
                                // spend
                                // a lot less
                                // time between entering data and checking for
                                // it
                                // than it can keep up with.
      // enter unverified score for team 1
      enterScore(selenium, "1", 1);
      selenium.selectWindow("brackets");
      Assert.assertFalse(selenium.getEval("window.document.getElementById('1-1').innerHTML").contains("Score:"));
      // verify
      selenium.selectWindow("scoreentry");
      selenium.select("xpath=//form[@name='verify']//select[@name='TeamNumber']", "value=1-1");
      selenium.click("id=verify_submit");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      selenium.click("id=Verified_yes");
      selenium.click("id=submit");
      selenium.getConfirmation();
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      selenium.selectWindow("brackets");
      selenium.runScript("window.iterate();");

      Assert.assertTrue(selenium.getEval("window.document.getElementById('1-1').innerHTML").contains("Score:"));
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final SAXException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch(final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;      
    }
  }

  private void enterScore(Selenium selenium,
                          String team,
                          int score) {
    selenium.selectWindow("scoreentry");
    selenium.select("xpath=//form[@name='selectTeam']//select[@name='TeamNumber']", "value="
        + team);
    selenium.click("id=enter_submit");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
    for (int i = 0; i < score; i++) {
      selenium.click("id=inc_score_1");
    }
    selenium.click("id=submit");
    selenium.getConfirmation();
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
  }
}
