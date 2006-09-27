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

import java.sql.Connection;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;

import org.xml.sax.SAXException;

/**
 * Do full tournament tests.
 *
 * @version $Revision$
 */
public class FullTournamentTest extends TestCase {
  
  private static final Logger LOG = Logger.getLogger(FullTournamentTest.class);

  /**
   * Test a full tournament.
   */
  public void test0()
    throws MalformedURLException, IOException, SAXException {
    /*
     * Need to figure out how to access database from outside tomcat
     *   - perhaps use the HSQL server?
     * 
     * Initialize the database
     * Load teams
     * Enter 3 runs for each
     * --- playoffs need to know which teams match up ---
     * Enter 4th run
     */

    // need to request a page to get the database server up and running
    final WebConversation conversation = new WebConversation();
    final WebRequest request = new GetMethodWebRequest(WebTestUtils.URL_ROOT);
    final WebResponse response = conversation.getResponse(request);
    
    final Connection connection = WebTestUtils.createDBConnection("sa", "");
    Assert.assertNotNull("Could not create test database connection", connection);
    
  }
  
}
