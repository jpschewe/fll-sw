/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;

import java.io.IOException;

import java.net.MalformedURLException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;

import org.xml.sax.SAXException;

/**
 * Basic tests.
 *
 * @version $Revision$
 */
public class WebTest extends TestCase {
  
  private static final Logger LOG = Logger.getLogger(WebTest.class);

  /**
   * Basic load of the pages.
   */
  public void testPages()
    throws SAXException, MalformedURLException, IOException {
    final String[] pages = new String[] {
      "",
      "index.jsp",
      "tournaments.jsp",
      "judges.jsp",
      "tables.jsp",
      "select_team.jsp",
      //"remoteControl.jsp",
      "advanceTeams.jsp",
    };
    final WebConversation conversation = new WebConversation();
    for(int i=0; i<pages.length; i++) {
      final WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/" + pages[i]);
      final WebResponse response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
    }
  }

  /**
   * Test changing tournaments to DUMMY and then back to State.
   */
  public void testChangeTournament()
    throws MalformedURLException, IOException, SAXException {
    final WebConversation conversation = new WebConversation();
    final WebRequest request = new PostMethodWebRequest(TestUtils.URL_ROOT + "admin/index.jsp");
    request.setParameter("currentTournament", "DUMMY");
    conversation.getResponse(request);

    request.setParameter("currentTournament", "State");
    conversation.getResponse(request);
    
  }
  
}
