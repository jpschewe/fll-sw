/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;

import junit.framework.Assert;

import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;

/**
 * Utilities for web tests.
 */
public final class WebTestUtils {

  private WebTestUtils() {
    // no instances
  }

  /**
   * Load a page and return the response. If there is an HttpException, call
   * {@link Assert#fail(String)} with a reasonable message.
   */
  public static WebResponse loadPage(final WebConversation conversation,
                                     final WebRequest request) throws IOException, SAXException {
    final boolean exceptionOnError = conversation.getExceptionsThrownOnErrorStatus();
    conversation.setExceptionsThrownOnErrorStatus(false);
    try {
      final WebResponse response = conversation.getResponse(request);

      // check response code here and fail with useful message
      checkForServerError(response);

      return response;
    } finally {
      // restore value
      conversation.setExceptionsThrownOnErrorStatus(exceptionOnError);
    }
  }

  private static void checkForServerError(final WebResponse response) throws IOException {
    final int code = response.getResponseCode();
    final boolean error;
    if (response.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
      error = true;
    } else if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
      error = true;
    } else if (response.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
      error = true;
    } else {
      error = false;
    }
    if (error) {
      final String responseMessage = response.getResponseMessage();
      final String text = response.getText();
      final File output = File.createTempFile("server-error", ".html", new File("screenshots"));
      final FileWriter writer = new FileWriter(output);
      writer.write(text);
      writer.close();
      Assert.fail("Error loading page: "
          + response.getURL() + " code: " + code + " message: " + responseMessage
          + " Contents of error page written to: " + output.getAbsolutePath());
    }

  }
  
  /**
   * @return
   * @throws SAXException
   * @throws IOException
   */
  public static WebConversation getConversation() throws IOException, SAXException {
    final WebConversation conversation = new WebConversation();

    // check for login and login if needed
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "setup/existingdb.jsp");
    WebResponse response = conversation.getResponse(request);
    Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

    WebForm form = response.getFormWithName("login");
    if (null != form) {
      request = form.getRequest();
      request.setParameter("j_username", "fll");
      request.setParameter("j_password", "LegoLeague");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
    }

    // setup auth
    // conversation.setAuthentication("FLL", "fll", "LegoLeague");

    return conversation;
  }

}
