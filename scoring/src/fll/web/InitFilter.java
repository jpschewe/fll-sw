/*
 * Copyright (c) 2008
 *      Jon Schewe.  All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * I'd appreciate comments/suggestions on the code jpschewe@mtu.net
 */
package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLRuntimeException;

/**
 * Initialize web attributes.
 * 
 * @web.filter name="Init Filter"
 * @web.filter-mapping url-pattern="/*"
 */
public class InitFilter implements Filter {

  private static final Logger LOGGER = Logger.getLogger(InitFilter.class);

  /**
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {
    // nothing
  }

  /**
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
    if (response instanceof HttpServletResponse
        && request instanceof HttpServletRequest) {
      final HttpServletResponse httpResponse = (HttpServletResponse) response;
      final HttpServletRequest httpRequest = (HttpServletRequest) request;

      final String path = httpRequest.getRequestURI();
      if (null != path
          && (path.startsWith(httpRequest.getContextPath()
              + "/setup")
              || path.startsWith(httpRequest.getContextPath()
                  + "/style") || path.startsWith(httpRequest.getContextPath()
                  + "/images") || path.startsWith(httpRequest.getContextPath()
                  + "/sponsor_logos") || path.startsWith(httpRequest.getContextPath()
                  + "/wiki") || path.endsWith(".jpg") || path.endsWith(".gif") || path.endsWith(".png") || path.endsWith(".pdf") || path.endsWith(".html"))) {
        // don't do init on the setup pages
        chain.doFilter(request, response);
      } else {
        try {
          final String redirect = initialize(httpRequest, httpResponse);
          if (null != redirect) {
            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(redirect));
          } else {
            chain.doFilter(request, response);
          }
        } catch (final SQLException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  /**
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(final FilterConfig filterConfig) throws ServletException {
    // nothing
  }

  /**
   * @param request
   * @param response
   * @return the URL to redirect to if there was trouble. This should be passed
   *         to encodeRedirectURL. If this is null, then everything initialized
   *         OK
   * @throws IOException
   * @throws RuntimeException
   * @throws SQLException
   */
  private String initialize(final HttpServletRequest request, final HttpServletResponse response) throws IOException, SQLException, RuntimeException {
    final HttpSession session = request.getSession();
    final ServletContext application = session.getServletContext();

    application.setAttribute(ApplicationAttributes.DATABASE, application.getRealPath("/WEB-INF/flldb"));

    // set some default text
    application.setAttribute(ApplicationAttributes.SCORE_PAGE_TEXT, "FLL");

    final String database = ApplicationAttributes.getDatabase(application);

    final boolean dbok = Utilities.testHSQLDB(database);
    if (!dbok) {
      LOGGER.warn("Database files not ok, redirecting to setup");
      session
             .setAttribute(SessionAttributes.MESSAGE,
                           "<p class='error'>The database does not exist yet or there is a problem with the database files. Please create the database.<br/></p>");
      return request.getContextPath()
          + "/setup";
    }

    // initialize the datasource
    final DataSource datasource;
    if (null == SessionAttributes.getDataSource(session)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Datasource not available, creating");
      }
      datasource = Utilities.createDataSource(database);
      session.setAttribute(SessionAttributes.DATASOURCE, datasource);
    } else {
      datasource = SessionAttributes.getDataSource(session);
    }

    // Initialize the connection
    final Connection connection = datasource.getConnection();

    // check if the database is initialized
    final boolean dbinitialized = Utilities.testDatabaseInitialized(connection);
    if (!dbinitialized) {
      LOGGER.warn("Database not initialized, redirecting to setup");
      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>The database is not yet initialized. Please create the database.</p>");
      return request.getContextPath()
          + "/setup";
    }

    // load the challenge descriptor
    if (null == ApplicationAttributes.getChallengeDocument(application)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loading challenge descriptor");
      }
      try {
        final Document document = Queries.getChallengeDocument(connection);
        if (null == document) {
          LOGGER.warn("Could not find challenge descriptor");
          session.setAttribute(SessionAttributes.MESSAGE,
                               "<p class='error'>Could not find xml challenge description in the database! Please create the database.</p>");
          return request.getContextPath()
              + "/setup";
        }
        application.setAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT, document);
      } catch (final FLLRuntimeException e) {
        LOGGER.error("Error getting challenge document", e);
        session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>"
            + e.getMessage() + " Please create the database.</p>");
        return request.getContextPath()
            + "/setup";
      }
    }

    // TODO put this in a separate filter to turn off caching

    // keep browser from caching any content
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", 0); // prevents caching at the proxy
    // server

    return null;
  }
}
