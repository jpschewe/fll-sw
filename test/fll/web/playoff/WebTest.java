/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.io.IOException;
import java.net.MalformedURLException;

import junit.framework.Assert;

import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
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
  
  /**
   * Basic load of the pages.
   */
  @Test
  public void testPages()
    throws SAXException, MalformedURLException, IOException {
    final String[] pages = new String[] {
      "index.jsp",
      "check.jsp?division=__all__",
      //"remoteMain.jsp",
    };
    final WebConversation conversation = new WebConversation();
    for(int i=0; i<pages.length; i++) {
      TestUtils.initializeDatabaseFromDump(WebTest.class.getResourceAsStream("/fll/data/test-database.zip"));

      final WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "playoff/" + pages[i]);
      final WebResponse response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
    }
  }
  
}
