/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;

/**
 * Basic tests of loading pages.
 */
public class WebTest /* extends SeleneseTestCase */{

  private static final Logger LOG = LogUtils.getLogger();

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  // @Override
  // public void setUp() throws Exception {
  // super.setUp("http://localhost:9080/setup");
  // }

  /**
   * Basic load of the pages.
   */
  @Test
  public void testPages() throws SAXException, MalformedURLException, IOException {
    final String[] pages = new String[] { // 
        "", //
        "display.jsp", //
        "index.jsp", //
        "welcome.jsp", "admin", "admin/index.jsp", "admin/edit_event_division.jsp", "admin/tournaments.jsp", "admin/judges.jsp", "admin/tables.jsp",
        "admin/select_team.jsp", "admin/remoteControl.jsp", //
        "credits/credits.jsp", //
        "developer/index.jsp", "developer/query.jsp", //
        "playoff/index.jsp", "playoff/check.jsp?division=__all__",
        // "playoff/remoteMain.jsp",
        "report/index.jsp",
        // "report/CategorizedScores",
        // "report/ScoreGroupScores",
        // "scoreboard/index.jsp",
        // "scoreboard/main.jsp",
        // "scoreboard_800/main.jsp",
        "scoreEntry/select_team.jsp", "setup/index.jsp", "style/style.jsp", "troubleshooting/index.jsp", };
    final WebConversation conversation = new WebConversation();
    for (final String page : pages) {
      LOG.info("Testing page #"
          + page + "#");
      WebTestUtils.initializeDatabaseFromDump(WebTest.class.getResourceAsStream("/fll/data/test-database.zip"));

      final String url = TestUtils.URL_ROOT
          + page;
      WebTestUtils.loadPage(conversation, url);
    }
  }

  /**
   * Test changing tournaments to DUMMY and then back to State.
   */
  @Test
  public void testChangeTournament() throws MalformedURLException, IOException, SAXException {
    WebTestUtils.initializeDatabaseFromDump(WebTest.class.getResourceAsStream("/fll/data/test-database.zip"));

    final WebConversation conversation = new WebConversation();
    WebTestUtils.setTournament(conversation, GenerateDB.DUMMY_TOURNAMENT_NAME);

    WebTestUtils.setTournament(conversation, "STATE");
  }

}
