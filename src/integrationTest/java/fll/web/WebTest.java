/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.WebDriver;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;

/**
 * Basic tests of loading pages.
 */
public class WebTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Requirements for running tests.
   */
  @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by the JUnit framework")
  @Rule
  public RuleChain chain = RuleChain.outerRule(new IntegrationTestUtils.TomcatRequired());

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
   * Basic load of the pages.
   * 
   * @throws InterruptedException
   */
  @Test
  public void testPages() throws SAXException, MalformedURLException, IOException, InterruptedException {
    try {
      final String[] pages = new String[] { //
                                            "", //
                                            "display.jsp", //
                                            "index.jsp", //
                                            "welcome.jsp", "admin", "admin/index.jsp", "admin/edit_event_division.jsp",
                                            "admin/tournaments.jsp", "admin/judges.jsp", "admin/tables.jsp",
                                            "admin/select_team.jsp", "admin/remoteControl.jsp", //
                                            "credits/credits.jsp", //
                                            "developer/index.jsp", "developer/query.jsp", //
                                            "playoff/index.jsp", "playoff/check.jsp?division=__all__",
                                            // "playoff/remoteMain.jsp",
                                            "report/index.jsp",
                                            // "report/CategorizedScores",
                                            // "scoreboard/index.jsp",
                                            // "scoreboard/main.jsp",
                                            // "scoreboard_800/main.jsp",
                                            "scoreEntry/select_team.jsp", "setup/index.jsp",
                                            "troubleshooting/index.jsp", };
      for (final String page : pages) {
        LOGGER.info("Testing page #"
            + page
            + "#");
        IntegrationTestUtils.initializeDatabaseFromDump(selenium,
                                                        TestUtils.class.getResourceAsStream("/fll/data/testdb.flldb"));

        final String url = TestUtils.URL_ROOT
            + page;
        IntegrationTestUtils.loadPage(selenium, url);
      }
    } catch (final IOException | RuntimeException | AssertionError e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot(selenium);
    }
  }

  /**
   * Test changing tournaments to DUMMY and then back to State.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  @Test
  public void testChangeTournament() throws IOException, InterruptedException {
    try {
      IntegrationTestUtils.initializeDatabaseFromDump(selenium,
                                                      TestUtils.class.getResourceAsStream("/fll/data/testdb.flldb"));

      IntegrationTestUtils.setTournament(selenium, GenerateDB.DUMMY_TOURNAMENT_NAME);

      IntegrationTestUtils.setTournament(selenium, GenerateDB.DROP_TOURNAMENT_NAME);
    } catch (final IOException | RuntimeException | AssertionError e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot(selenium);
    }
  }

}
