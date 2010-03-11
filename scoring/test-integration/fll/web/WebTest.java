/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.net.MalformedURLException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;

/**
 * Basic tests of loading pages.
 */
public class WebTest /* extends SeleneseTestCase */{

  private static final Logger LOG = Logger.getLogger(WebTest.class);

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
    for (final String page : pages) {
      LOG.info("Testing page #" + page + "#");
      TestUtils.initializeDatabaseFromDump(WebTest.class.getResourceAsStream("/fll/data/test-database.zip"));

      final String url = TestUtils.URL_ROOT
          + page;
      loadPage(url);
    }
  }

  private void loadPage(final String url) throws IOException, SAXException {
    final WebConversation conversation = new WebConversation();
    // conversation.setExceptionsThrownOnErrorStatus(false);
    final WebRequest request = new GetMethodWebRequest(url);
    try {
      final WebResponse response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
    } catch (final HttpException e) {
      final String responseMessage = e.getResponseMessage();
      final int code = e.getResponseCode();
      Assert.fail("Error loading page: "
          + url + " code: " + code + " message: " + responseMessage);
    }
  }

  /**
   * Test changing tournaments to DUMMY and then back to State.
   */
  @Test
  public void testChangeTournament() throws MalformedURLException, IOException, SAXException {
    TestUtils.initializeDatabaseFromDump(WebTest.class.getResourceAsStream("/fll/data/test-database.zip"));

    final WebConversation conversation = new WebConversation();
    final WebRequest request = new PostMethodWebRequest(TestUtils.URL_ROOT
        + "admin/SetCurrentTournament");
    request.setParameter("currentTournament", "DUMMY");
    conversation.getResponse(request);

    request.setParameter("currentTournament", "State");
    conversation.getResponse(request);
  }

}
