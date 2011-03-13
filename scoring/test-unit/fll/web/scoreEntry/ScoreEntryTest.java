/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeParserTest;

/**
 * @author jpschewe
 * @version $Revision$
 */
public class ScoreEntryTest {

  /**
   * Just returns the document when asked.
   */
  private static class TestServletContext implements ServletContext {

    public Object getAttribute(final String attr) {
      if(ApplicationAttributes.CHALLENGE_DOCUMENT.equals(attr)) {
        final InputStream stream = ChallengeParserTest.class.getResourceAsStream("data/all-elements.xml");
        Assert.assertNotNull(stream);
        final Document document = ChallengeParser.parse(new InputStreamReader(stream));
        Assert.assertNotNull(document);
        return document;
      } else {
      return null;
      }
    }

    public Enumeration<?> getAttributeNames() {
      return null;
    }

    public ServletContext getContext(final String arg0) {
      return null;
    }

    public String getContextPath() {
      return null;
    }

    public String getInitParameter(final String arg0) {
      return null;
    }

    public Enumeration<?> getInitParameterNames() {
      return null;
    }

    public int getMajorVersion() {
      return 0;
    }

    public String getMimeType(final String arg0) {
      return null;
    }

    public int getMinorVersion() {
      return 0;
    }

    public RequestDispatcher getNamedDispatcher(final String arg0) {
      return null;
    }

    public String getRealPath(final String arg0) {
      return null;
    }

    public RequestDispatcher getRequestDispatcher(final String arg0) {
      return null;
    }

    public URL getResource(final String arg0) throws MalformedURLException {
      return null;
    }

    public InputStream getResourceAsStream(final String arg0) {
      return null;
    }

    public Set<?> getResourcePaths(final String arg0) {
      return null;
    }

    public String getServerInfo() {
      return null;
    }

    public Servlet getServlet(final String arg0) throws ServletException {
      return null;
    }

    public String getServletContextName() {
      return null;
    }

    public Enumeration<?> getServletNames() {
      return null;
    }

    public Enumeration<?> getServlets() {
      return null;
    }

    public void log(String arg0) {
    }

    public void log(final Exception arg0,
                    final String arg1) {
    }

    public void log(final String arg0,
                    final Throwable arg1) {
    }

    public void removeAttribute(final String arg0) {
    }

    public void setAttribute(final String arg0,
                             final Object arg1) {
    }

  }

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Test method for
   * {@link fll.web.scoreEntry.ScoreEntry#generateCheckRestrictionsBody(java.io.Writer, org.w3c.dom.Document)}
   * .
   * <p>
   * Load all-elements.xml (from {@link ChallengeParserTest}) and make sure
   * there are no errors.
   * </p>
   */
  @Test
  public void testGenerateCheckRestrictionsBody() throws IOException, ParseException {
    final StringWriter writer = new StringWriter();
    
    ScoreEntry.generateCheckRestrictionsBody(writer, new TestServletContext());
    Assert.assertTrue(writer.toString().length() > 0);
  }
  
  
}
