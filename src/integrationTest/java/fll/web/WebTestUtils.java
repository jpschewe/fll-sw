/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Assert;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.JavaScriptPage;
import com.gargoylesoftware.htmlunit.Page;
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

  public static Page loadPage(final WebClient conversation,
                              final com.gargoylesoftware.htmlunit.WebRequest request)
      throws IOException, SAXException {
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
      final Path output = Files.createTempFile(Paths.get("screenshots"), "server-error", ".html");
      try (Writer writer = Files.newBufferedWriter(output, Utilities.DEFAULT_CHARSET)) {
        writer.write(text);
      }
      Assert.fail("Error loading page: "
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
   */
  public static String getPageSource(final Page page) {
    if (page instanceof HtmlPage) {
      return ((HtmlPage) page).asXml();
    } else if (page instanceof JavaScriptPage) {
      return ((JavaScriptPage) page).getContent();
    } else if (page instanceof TextPage) {
      return ((TextPage) page).getContent();
    } else if (page instanceof UnexpectedPage) {
      // page instanceof UnexpectedPage
      return ((UnexpectedPage) page).getWebResponse().getContentAsString();
    } else {
      throw new RuntimeException("Unexpected page type: "
          + page.getClass());
    }
  }

  public static WebClient getConversation() throws IOException, SAXException {
    final WebClient conversation = new WebClient();

    // always login first
    final Page loginPage = conversation.getPage(TestUtils.URL_ROOT
        + "login.jsp");
    Assert.assertTrue("Received non-HTML response from web server", loginPage.isHtmlPage());

    final HtmlPage loginHtml = (HtmlPage) loginPage;
    HtmlForm form = loginHtml.getFormByName("login");
    Assert.assertNotNull("Cannot find login form", form);

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
    Assert.assertTrue("Unexpected URL after login: "
        + address, correctAddress);

    return conversation;
  }

  /**
   * Submit a query to developer/QueryHandler, parse the JSON and return it.
   */
  public static QueryHandler.ResultData executeServerQuery(final String query) throws IOException, SAXException {
    final WebClient conversation = getConversation();

    final URL url = new URL(TestUtils.URL_ROOT
        + "developer/QueryHandler");
    final WebRequest request = new WebRequest(url);
    request.setRequestParameters(Collections.singletonList(new NameValuePair(QueryHandler.QUERY_PARAMETER, query)));

    final Page response = loadPage(conversation, request);
    final String contentType = response.getWebResponse().getContentType();
    if (!"application/json".equals(contentType)) {
      final String text = getPageSource(response);
      final Path output = Files.createTempFile(Paths.get("screenshots"), "json-error", ".html");
      try (Writer writer = Files.newBufferedWriter(output, Utilities.DEFAULT_CHARSET)) {
        writer.write(text);
      }
      Assert.fail("Error JSON from QueryHandler: "
          + response.getUrl()
          + " Contents of error page written to: "
          + output.toAbsolutePath());
    }

    final String responseData = getPageSource(response);

    final ObjectMapper jsonMapper = new ObjectMapper();
    QueryHandler.ResultData result = jsonMapper.readValue(responseData, QueryHandler.ResultData.class);
    Assert.assertNull("SQL Error: "
        + result.getError(), result.getError());

    return result;
  }

}
