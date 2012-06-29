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
import java.net.URL;

import junit.framework.Assert;

import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;
import fll.web.developer.QueryHandler;

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
   * Create a new web conversation that is logged in.
   * 
   * @return a web conversation to use
   * @throws SAXException
   * @throws IOException
   */
  public static WebConversation getConversation() throws IOException, SAXException {
    final WebConversation conversation = new WebConversation();

    // always login first
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "login.jsp");
    WebResponse response = conversation.getResponse(request);
    Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

    WebForm form = response.getFormWithName("login");
    Assert.assertNotNull("Cannot find login form", form);
    request = form.getRequest();
    request.setParameter("user", IntegrationTestUtils.TEST_USERNAME);
    request.setParameter("pass", IntegrationTestUtils.TEST_PASSWORD);
    response = conversation.getResponse(request);
    Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

    final URL responseURL = response.getURL();
    final String address = responseURL.getPath();
    Assert.assertTrue("Unexpected URL after login: " + address, address.endsWith("fll-sw/"));

    return conversation;
  }

  /**
   * Submit a query to developer/QueryHandler, parse the JSON and return it.
   */
  public static QueryHandler.ResultData executeServerQuery(final String query) throws IOException, SAXException {
    final WebConversation conversation = getConversation();
    final WebRequest request = new PostMethodWebRequest(TestUtils.URL_ROOT
        + "developer/QueryHandler");
    request.setParameter(QueryHandler.QUERY_PARAMETER, query);

    final WebResponse response = loadPage(conversation, request);
    final String contentType = response.getContentType();
    if (!"application/json".equals(contentType)) {
      final String text = response.getText();
      final File output = File.createTempFile("json-error", ".html", new File("screenshots"));
      final FileWriter writer = new FileWriter(output);
      writer.write(text);
      writer.close();
      Assert.fail("Error JSON from QueryHandler: "
          + response.getURL() + " Contents of error page written to: " + output.getAbsolutePath());
    }

    final String responseData = response.getText();

    final Gson gson = new Gson();
    QueryHandler.ResultData result = gson.fromJson(responseData, QueryHandler.ResultData.class);
    Assert.assertNull("SQL Error: "
        + result.error, result.error);

    return result;
  }

}
