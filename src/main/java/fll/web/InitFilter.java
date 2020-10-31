/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
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
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Utilities;
import fll.db.GlobalParameters;
import fll.util.FLLRuntimeException;
import fll.xml.ChallengeDescription;

/**
 * Initialize web attributes.
 */
@WebFilter("/*")
public class InitFilter implements Filter {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
    // nothing
  }

  /**
   * @param request the HTTP request
   * @param response the response that is filtered
   * @param chain the chain of filters
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(final ServletRequest request,
                       final ServletResponse response,
                       final FilterChain chain)
      throws IOException, ServletException {
    if (response instanceof HttpServletResponse
        && request instanceof HttpServletRequest) {
      final HttpServletResponse httpResponse = (HttpServletResponse) response;
      final HttpServletRequest httpRequest = (HttpServletRequest) request;
      final String path = httpRequest.getRequestURI();
      final HttpSession session = httpRequest.getSession();

      LOGGER.trace("Loading {} message: {} referer: {} session: {}", path, SessionAttributes.getMessage(session),
                   httpRequest.getHeader("referer"), session.getId());

      final ServletContext application = session.getServletContext();

      // call init before check security, if a page doesn't need init,
      // then it doesn't require security either
      final boolean needsInit = needsInit(httpRequest.getContextPath(), path);
      final boolean needsSecurity = needsSecurity(httpRequest.getContextPath(), path);
      LOGGER.debug("needsInit: "
          + needsInit
          + " needsSecurity: "
          + needsSecurity);

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

      // keep browser from caching any content
      httpResponse.setHeader("Cache-Control", "no-store"); // HTTP 1.1
      httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
      httpResponse.setDateHeader("Expires", 0); // proxy server cache

    } else {
      LOGGER.trace("Non-servlet request: {}", request);
    }

    LOGGER.trace("Bottom of doFilter");
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
        + contextPath
        + " path: "
        + path);
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
                + "/api") //
            || path.startsWith(contextPath
                + "/subjective") //
            || path.startsWith(contextPath
                + "/setup") //
        )) {
      if (path.startsWith(contextPath
          + "/report/finalist/FinalistTeams")) {
        // allow the list of finalist teams to be public
        return false;
      } else if (path.startsWith(contextPath
          + "/api/CheckAuth")) {
        // checking the authentication doesn't require security
        return false;
      } else if (path.startsWith("/robots.txt")) {
        return false;
      } else {
        LOGGER.debug("Returning true from needsSecurity");
        return true;
      }
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
      } else if (path.startsWith(contextPath
          + "/setup")) {
        return false;
      } else if (path.startsWith(contextPath
          + "/robots.txt")) {
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
                                final HttpSession session)
      throws IOException {
    LOGGER.trace("Top of checkSecurity");

    if (WebUtils.checkAuthenticated(request, application)) {
      LOGGER.trace("Returning true from checkSecurity");

      return true;
    } else {
      session.setAttribute(SessionAttributes.REDIRECT_URL, WebUtils.getFullURL(request));
      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/login.jsp"));
      LOGGER.debug("Returning false from checkSecurity");
      return false;
    }
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // nothing
  }

  private static final Object INIT_LOCK = new Object();

  /**
   * @param request
   * @param response
   * @return true if everything is OK, false if a redirect happened
   */
  private static boolean initialize(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final HttpSession session,
                                    final ServletContext application)
      throws IOException, RuntimeException {
    LOGGER.trace("Top of initialize");

    synchronized (INIT_LOCK) {

      // make sure that we compute the host names as soon as possible
      WebUtils.updateHostNamesInBackground(application);

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {

        // check if the database is initialized
        final boolean dbinitialized = Utilities.testDatabaseInitialized(connection);
        if (!dbinitialized) {
          LOGGER.warn("Database not initialized, redirecting to setup");
          SessionAttributes.appendToMessage(session,
                                            "<p class='error'>The database is not yet initialized. Please create the database.</p>");
          response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
              + "/setup/index.jsp"));
          return false;
        }

        // load the challenge descriptor
        if (null == ApplicationAttributes.getAttribute(application, ApplicationAttributes.CHALLENGE_DESCRIPTION,
                                                       ChallengeDescription.class)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Loading challenge descriptor");
          }
          try {
            final ChallengeDescription challengeDescription = GlobalParameters.getChallengeDescription(connection);

            application.setAttribute(ApplicationAttributes.CHALLENGE_DESCRIPTION, challengeDescription);
          } catch (final FLLRuntimeException e) {
            LOGGER.error("Error getting challenge document", e);
            SessionAttributes.appendToMessage(session, "<p class='error'>"
                + e.getMessage()
                + " Please create the database.</p>");
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
                + "/setup/index.jsp"));
            return false;
          }
        }

      } catch (final SQLException e) {
        throw new RuntimeException(e);
      }

      LOGGER.trace("Bottom of initialize returning true");
      return true;

    } // lock
  }

}
