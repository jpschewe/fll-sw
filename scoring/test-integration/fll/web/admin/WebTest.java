/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.net.MalformedURLException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;

/**
 * Basic tests.
 * 
 * @version $Revision$
 */
public class WebTest {

  private static final Logger LOGGER = Logger.getLogger(WebTest.class);

  /**
   * Basic load of the pages.
   */
  @Test
  public void testPages() throws SAXException, MalformedURLException, IOException {
    final String[] pages = new String[] { "", "index.jsp", "edit_event_division.jsp", "tournaments.jsp", "judges.jsp", "tables.jsp", "select_team.jsp",
                                         "remoteControl.jsp", "advanceTeams.jsp", };
    final WebConversation conversation = new WebConversation();
    for (final String page : pages) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Testing page "
            + page);
      }
      TestUtils.initializeDatabaseFromDump(WebTest.class.getResourceAsStream("/fll/data/test-database.zip"));
      final WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
          + "admin/" + page);
      final WebResponse response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
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
        + "admin/index.jsp");
    request.setParameter("currentTournament", "DUMMY");
    conversation.getResponse(request);

    request.setParameter("currentTournament", "State");
    conversation.getResponse(request);

  }

}
