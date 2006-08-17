/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.io.IOException;

import java.net.MalformedURLException;

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
      "display.jsp",
      "instructions.jsp",
      "welcome.jsp",

    };
    final WebConversation conversation = new WebConversation();
    for(int i=0; i<pages.length; i++) {
      final WebRequest request = new GetMethodWebRequest(WebTestUtils.URL_ROOT + pages[i]);
      final WebResponse response = conversation.getResponse(request);
    }
  }
  
}
