/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import fll.TestUtils;
import fll.Utilities;
import fll.web.developer.QueryHandler;

/**
 * Utilities for web tests.
 */
public final class WebTestUtils {

  private WebTestUtils() {
    // no instances
  }

  /**
   * Load a page using HTML unit.
   * 
   * @param conversation the current conversation
   * @param request the request
   * @return the response
   * @throws IOException if there is an error talking to the server
   */
  public static Page loadPage(final WebClient conversation,
                              final com.gargoylesoftware.htmlunit.WebRequest request)
      throws IOException {
    final boolean exceptionOnError = conversation.getOptions().isThrowExceptionOnFailingStatusCode();
    conversation.getOptions().setThrowExceptionOnFailingStatusCode(false);
    try {
      final Page response = conversation.getPage(request);

      // check response code here and fail with useful message
      checkForServerError(response);

      return response;
    } finally {
      // restore value
      conversation.getOptions().setThrowExceptionOnFailingStatusCode(exceptionOnError);
    }
  }

  private static void checkForServerError(final Page page) throws IOException {
    final com.gargoylesoftware.htmlunit.WebResponse response = page.getWebResponse();
    final int code = response.getStatusCode();
    final boolean error;
    if (code >= 400) {
      error = true;
    } else {
      error = false;
    }
    if (error) {
      final String responseMessage = response.getStatusMessage();
      final String text = getPageSource(page);

      final Path screenshots = IntegrationTestUtils.ensureScreenshotDirectoryExists();
      final Path output = Files.createTempFile(screenshots, "server-error", ".html");
      try (Writer writer = Files.newBufferedWriter(output, Utilities.DEFAULT_CHARSET)) {
        writer.write(text);
      }
      fail("Error loading page: "
          + page.getUrl()
          + " code: "
          + code
          + " message: "
          + responseMessage
          + " Contents of error page written to: "
          + output.toAbsolutePath().toString());
    }

  }

  /**
   * Get source of any page type.
   * 
   * @param page the page
   * @return the source as a string
   */
  public static String getPageSource(final Page page) {
    if (page instanceof HtmlPage) {
      return ((HtmlPage) page).asXml();
    } else if (page instanceof SgmlPage) {
      return ((SgmlPage) page).asXml();
    } else if (page instanceof TextPage) {
      return ((TextPage) page).getContent();
    } else if (page instanceof UnexpectedPage) {
      return ((UnexpectedPage) page).getWebResponse().getContentAsString();
    } else {
      throw new RuntimeException("Unexpected page type: "
          + page.getClass());
    }
  }

  /**
   * Get a conversation with the web server that is already logged in.
   * 
   * @return the client to use for loading further pages
   * @throws IOException if there is an error talking to the server
   */
  public static WebClient getConversation() throws IOException {
    final WebClient conversation = new WebClient();

    // always login first
    final Page loginPage = conversation.getPage(TestUtils.URL_ROOT
        + "login.jsp");
    assertTrue(loginPage.isHtmlPage(), "Received non-HTML response from web server");

    final HtmlPage loginHtml = (HtmlPage) loginPage;
    final HtmlForm form = loginHtml.getFormByName("login");
    assertNotNull(form, "Cannot find login form");

    final HtmlTextInput userTextField = form.getInputByName("user");
    userTextField.setValueAttribute(IntegrationTestUtils.TEST_USERNAME);

    final HtmlPasswordInput passTextField = form.getInputByName("pass");
    passTextField.setValueAttribute(IntegrationTestUtils.TEST_PASSWORD);

    final HtmlSubmitInput button = form.getInputByName("submit_login");
    final Page response = button.click();

    final URL responseURL = response.getUrl();
    final String address = responseURL.getPath();
    final boolean correctAddress;
    if (address.contains("login.jsp")) {
      correctAddress = false;
    } else {
      correctAddress = true;
    }
    assertTrue(correctAddress, "Unexpected URL after login: "
        + address);

    return conversation;
  }

  /**
   * Submit a query to developer/QueryHandler, parse the JSON and return it.
   * 
   * @param query the SQL query to execute using the developer tools
   * @return the parsed SQL result
   */
  public static QueryHandler.ResultData executeServerQuery(final String query) throws IOException {
    final WebClient conversation = getConversation();

    final URL url = new URL(TestUtils.URL_ROOT
        + "developer/QueryHandler");
    final WebRequest request = new WebRequest(url);
    request.setRequestParameters(Collections.singletonList(new NameValuePair(QueryHandler.QUERY_PARAMETER, query)));

    final Page response = loadPage(conversation, request);
    final String contentType = response.getWebResponse().getContentType();
    if (!"application/json".equals(contentType)) {
      final Path screenshots = IntegrationTestUtils.ensureScreenshotDirectoryExists();

      final String text = getPageSource(response);
      final Path output = Files.createTempFile(screenshots, "json-error", ".html");
      try (Writer writer = Files.newBufferedWriter(output, Utilities.DEFAULT_CHARSET)) {
        writer.write(text);
      }
      fail("Error JSON from QueryHandler: "
          + response.getUrl()
          + " Contents of error page written to: "
          + output.toAbsolutePath());
    }

    final String responseData = getPageSource(response);

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    final QueryHandler.ResultData result = jsonMapper.readValue(responseData, QueryHandler.ResultData.class);
    assertNull(result.getError(), "SQL Error: "
        + result.getError());

    return result;
  }

}
