/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;

/**
 * Check if tomcat is running, don't exit until it is.
 * 
 * @version $Revision$
 */
public final class WaitForTomcatStart {

  private static final Logger LOG = Logger.getLogger(WaitForTomcatStart.class);

  private WaitForTomcatStart() {
    // no instances.
  }

  public static void main(final String[] args) {
    waitForTomcatStart();
  }

  /**
   * Keep looping until we get a valid response from tomcat.
   */
  public static void waitForTomcatStart() {
    boolean done = false;
    while(!done) {
      try {
        // need to request a page to get the database server up and running
        final WebConversation conversation = new WebConversation();
        final WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "/setup");
        final WebResponse response = conversation.getResponse(request);
        if(response.isHTML()) {
          done = true;
        }
      } catch(final MalformedURLException mue) {
        throw new RuntimeException("Coding error with url", mue);
      } catch(final IOException e) {
        done = false;
      } catch(final SAXException se) {
        LOG.warn("Error parsing page, assuming tomcat is up", se);
        done = true;
      }
    }
  }

}
