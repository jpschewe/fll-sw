/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;
import fll.xml.GenerateDBTest;

import java.io.IOException;
import java.io.InputStream;

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
     * Initialize the database
     * Load teams
     * Enter 3 runs for each
     * --- playoffs need to know which teams match up ---
     * Enter 4th run
     */


    final WebConversation conversation = new WebConversation();
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "setup/");
    WebResponse response = conversation.getResponse(request);
    Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

    // after the first request to the website, we can create a database connection
    final Connection connection = TestUtils.createDBConnection("fll", "fll");
    Assert.assertNotNull("Could not create test database connection", connection);
    
    WebForm[] forms = response.getForms();
    Assert.assertEquals("Recieved incorrect number of forms on setup page", 1, forms.length);
    final WebForm setupForm = forms[0];
    setupForm.setCheckbox("force_rebuild", true); // rebuild the whole database
    final InputStream challengeDocIS = GenerateDBTest.class.getResourceAsStream("data/challenge-test.xml");
    Assert.assertNotNull(challengeDocIS);
    final UploadFileSpec challengeUpload = new UploadFileSpec("challenge-test.xml", challengeDocIS, "text/xml");
    setupForm.setParameter("xmldocument", new UploadFileSpec[] {challengeUpload});
    request = setupForm.getRequest("reinitializeDatabase");
    response = conversation.getResponse(request);
    Assert.assertTrue(response.isHTML());
    Assert.assertNotNull("Error initializing database: " + response.getText(), response.getElementWithID("success"));
    
  }
  
}
