/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Initialize web attributes.
 */
@WebFilter("/*")
public class InitFilter implements Filter {

  private static final Logger LOGGER = LogUtils.getLogger();

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
  public void doFilter(final ServletRequest request,
                       final ServletResponse response,
                       final FilterChain chain) throws IOException, ServletException {
    if (response instanceof HttpServletResponse
        && request instanceof HttpServletRequest) {
      final HttpServletResponse httpResponse = (HttpServletResponse) response;
      final HttpServletRequest httpRequest = (HttpServletRequest) request;
      final String path = httpRequest.getRequestURI();
      final HttpSession session = httpRequest.getSession();

      final ServletContext application = session.getServletContext();

      // call init before check security, if a page doesn't need init,
      // then it doesn't require security either
      final boolean needsInit = needsInit(httpRequest.getContextPath(), path);
      final boolean needsSecurity = needsSecurity(httpRequest.getContextPath(), path);
      LOGGER.debug("needsInit: "
          + needsInit + " needsSecurity: " + needsSecurity);

      if (needsInit) {
        if (!initialize(httpRequest, httpResponse, session, application)) {
          LOGGER.debug("Returning after initialize did redirect");
          return;
        }
      }

      if (needsSecurity) {
        if (!checkSecurity(httpRequest, httpResponse, application, session)) {
          LOGGER.debug("Returning after checkSecurity did redirect");
          return;
        }
      }
    }

    chain.doFilter(request, response);
  }

  /**
   * Check if the path needs security.
   * 
   * @param contextPath the contet of the web app
   * @param path the path to the requested resource
   * @return true if a valid login is required for this resource
   */
  private boolean needsSecurity(final String contextPath,
                                final String path) {
    LOGGER.debug("Checking contextPath: "
        + contextPath + " path: " + path);
    if (null != path
        && (path.startsWith(contextPath
            + "/admin/") //
            || path.startsWith(contextPath
                + "/developer/") //
            || path.startsWith(contextPath
                + "/scoreEntry/") //
            || path.startsWith(contextPath
                + "/report/") //
            || path.startsWith(contextPath
                + "/schedule/") //
            || path.startsWith(contextPath
                + "/playoff/InitializeBrackets") //
            || path.startsWith(contextPath
                + "/playoff/scoregenbrackets.jsp") //
            || path.startsWith(contextPath
                + "/playoff/adminbrackets.jsp") //
        || path.startsWith(contextPath
            + "/setup") //
        )) {
      LOGGER.debug("Returning true from needsSecurity");
      return true;
    } else {
      return false;
    }
  }

  /**
   * Check if the path needs init to be called.
   * 
   * @param contextPath the contet of the web app
   * @param path the path to the requested resource
   * @return true if initalize needs to be called
   */
  private boolean needsInit(final String contextPath,
                            final String path) {
    if (null != path) {
      if (path.startsWith(contextPath
          + "/style") //
          || path.startsWith(contextPath
              + "/images") //
          || path.startsWith(contextPath
              + "/sponsor_logos") //
          || path.startsWith(contextPath
              + "/wiki") //
          || path.endsWith(".jpg") //
          || path.endsWith(".gif") //
          || path.endsWith(".png") //
          || path.endsWith(".html")) {
        return false;
      } else if (path.equals(contextPath
          + "/setup/index.jsp")) {
        return false;
      } else if (path.equals(contextPath
          + "/setup/CreateDB")) {
        // FIXME need to know the difference between creating new database and
        // importing into an existing one
        return false;
      }
    }
    return true;
  }

  /**
   * Check if the current connection is authenticated or the page doesn't
   * require authentication.
   * 
   * @param request request
   * @param response response
   * @param session session for the request
   * @return true if everything is OK, false if a redirect to the login page was
   *         issued
   */
  private boolean checkSecurity(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      if (Queries.isAuthenticationEmpty(connection)) {
        LOGGER.debug("Returning true from checkSecurity for empty auth");
        return true;
      }

      final Collection<String> loginKeys = CookieUtils.findLoginKey(request);
      if (Queries.checkValidLogin(connection, loginKeys)) {
        LOGGER.debug("Returning true from checkSecurity for valid login: "
            + loginKeys);
        return true;
      } else {
        session.setAttribute(SessionAttributes.REDIRECT_URL, request.getRequestURI());
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
            + "/login.jsp"));
        LOGGER.debug("Returning false from checkSecurity");
        return false;
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(final FilterConfig filterConfig) throws ServletException {
    // nothing
  }

  public static void initDataSource(final ServletContext application) {
    final String database = application.getRealPath("/WEB-INF/flldb");
    
    // initialize the datasource
    if (null == ApplicationAttributes.getDataSource(application)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Datasource not available, creating");
      }
      final DataSource datasource = Utilities.createFileDataSource(database);
      application.setAttribute(ApplicationAttributes.DATASOURCE, datasource);
    } 
  }
  
  /**
   * @param request
   * @param response
   * @return true if everything is OK, false if a redirect happened
   */
  private static boolean initialize(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final HttpSession session,
                                    final ServletContext application) throws IOException, RuntimeException {
    try {
      // set some default text
      if (null == application.getAttribute(ApplicationAttributes.SCORE_PAGE_TEXT)) {
        application.setAttribute(ApplicationAttributes.SCORE_PAGE_TEXT, "FLL");
      }

      initDataSource(application);
      final DataSource datasource = ApplicationAttributes.getDataSource(application);

      // Initialize the connection
      Connection connection = null;
      try {
        connection = datasource.getConnection();

        // check if the database is initialized
        final boolean dbinitialized = Utilities.testDatabaseInitialized(connection);
        if (!dbinitialized) {
          LOGGER.warn("Database not initialized, redirecting to setup");
          session.setAttribute(SessionAttributes.MESSAGE,
                               "<p class='error'>The database is not yet initialized. Please create the database.</p>");
          response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
              + "/setup/index.jsp"));
          return false;
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
              response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
                  + "/setup/index.jsp"));
              return false;
            }
            application.setAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT, document);
          } catch (final FLLRuntimeException e) {
            LOGGER.error("Error getting challenge document", e);
            session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>"
                + e.getMessage() + " Please create the database.</p>");
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
                + "/setup/index.jsp"));
            return false;
          }
        }
      } finally {
        SQLFunctions.close(connection);
      }

      // TODO ticket:87 allow static data to be cached

      // keep browser from caching any content
      response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
      response.setHeader("Pragma", "no-cache"); // HTTP 1.0
      response.setDateHeader("Expires", 0); // prevents caching at the proxy
      // server

      return true;
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
