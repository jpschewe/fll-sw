/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.db.GenerateDB;


/**
 * Basic tests of loading pages.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class WebTest {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Basic load of the pages.
   *
   * @throws InterruptedException
   */
  @Test
  public void testPages(final WebDriver selenium)
      throws SAXException, MalformedURLException, IOException, InterruptedException {
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
                                            // "scoreboard/index.jsp",
                                            // "scoreboard/main.jsp",
                                            // "scoreboard/main_small.jsp",
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
  public void testChangeTournament(final WebDriver selenium) throws IOException, InterruptedException {
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
