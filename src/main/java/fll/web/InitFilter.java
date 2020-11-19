/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

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

import org.eclipse.jdt.internal.compiler.ast.Annotation.AnnotationTargetAllowed;

import fll.Utilities;
import fll.db.Authentication;
import fll.db.GlobalParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.xml.ChallengeDescription;

/**
 * Initialize web attributes.
 */
@WebFilter("/*")
public class InitFilter implements Filter {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  public void destroy() {
    // nothing
  }

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

      final ServletContext application = session.getServletContext();

      // make sure the authentication is valid first
      checkAuthenticationValid(application, session);

      LOGGER.trace("Loading {} message: {} referer: {} session: {} auth: {}", path,
                   SessionAttributes.getMessage(session), httpRequest.getHeader("referer"), session.getId(),
                   SessionAttributes.getAuthentication(session));

      final boolean needsInit = needsInit(httpRequest.getContextPath(), path);
      LOGGER.debug("needsInit: "
          + needsInit);

      if (needsInit) {
        if (!initialize(httpRequest, httpResponse, session, application)) {
          LOGGER.debug("Returning after initialize did redirect");
          return;
        }
      } else if (null != path
          && path.startsWith(httpRequest.getContextPath()
              + "/setup")) {
        possiblyInstallSetupAuthentication(application, session);
      }

      final boolean permissionDenied = checkSecurity(session, httpRequest, httpResponse);
      if (permissionDenied) {
        LOGGER.debug("User does not have permission for the page, forwarded request");
        return;
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
   * Check if the user can access this page. Note that some pages check
   * authentication themselves. Going forward I suspect that this is the direction
   * that I will go for future code.
   *
   * @return true if the user is being sent to the permission denied page and the
   *         caller should stop processing
   * @throws IOException if there was an error with the forward
   * @throws ServletException if there was an error with the forward
   */
  private boolean checkSecurity(final HttpSession session,
                                final HttpServletRequest request,
                                final HttpServletResponse response)
      throws IOException, ServletException {
    final String path = request.getRequestURI();
    final String contextPath = request.getContextPath();

    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    LOGGER.debug("Checking contextPath: "
        + contextPath
        + " path: "
        + path);
    boolean permissionDenied = false;
    if (null != path
        && (path.startsWith(contextPath
            + "/admin/") //
            || path.startsWith(contextPath
                + "/developer/") //
            || path.startsWith(contextPath
                + "/report/") //
            || path.startsWith(contextPath
                + "/schedule/") //
            || path.startsWith(contextPath
                + "/playoff/InitializeBrackets") //
        )) {
      if (path.startsWith(contextPath
          + "/report/finalist/FinalistTeams")) {
        // allow the list of finalist teams to be public
        permissionDenied = false;
      } else if (path.startsWith(contextPath
          + "/admin/changePassword.jsp")) {
        // handled by the page
        permissionDenied = false;
      } else if (path.startsWith(contextPath
          + "/admin/createUsername.jsp")) {
        // handled by the page
        permissionDenied = false;
      } else if (path.startsWith(contextPath
          + "/admin/CreateUser")) {
        // handled by the page
        permissionDenied = false;
      } else if (path.startsWith(contextPath
          + "/admin/ask-create-admin.jsp")) {
        // handled by the page
        permissionDenied = false;
      } else {
        if (!auth.isAdmin()) {
          permissionDenied = true;
        }
      }
    } else if (null != path
        && path.startsWith(contextPath
            + "/scoreEntry/")) {
      if (!auth.isRef()) {
        permissionDenied = true;
      }
    } else if (null != path
        && path.startsWith(contextPath
            + "/subjective/CheckAuth")) {
      // checking the authentication doesn't require security
      permissionDenied = false;
    } else if (null != path
        && path.startsWith(contextPath
            + "/subjective")) {
      if (!auth.isJudge()) {
        permissionDenied = true;
      }
    } else if (null != path
        && (path.startsWith(contextPath
            + "/playoff/scoregenbrackets.jsp") //
            || path.startsWith(contextPath
                + "/playoff/adminbrackets.jsp")) //
    ) {
      if (!auth.isRef()) {
        permissionDenied = true;
      }
    } else {
      // no authentication needed or handled by the page itself
      permissionDenied = false;
    }

    if (permissionDenied) {
      request.getRequestDispatcher("/permission-denied.jsp").forward(request, response);
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

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // nothing
  }

  private static void possiblyInstallSetupAuthentication(final ServletContext application,
                                                         final HttpSession session) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      // check if the database is initialized
      final boolean dbinitialized = Utilities.testDatabaseInitialized(connection);
      if (!dbinitialized) {
        // setup special authentication for setup
        LOGGER.debug("No database, setting inSetup authentication");
        AuthenticationContext auth = AuthenticationContext.inSetup();
        session.setAttribute(SessionAttributes.AUTHENTICATION, auth);
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * @return true if everything is OK, false if a redirect happened
   */
  private static boolean initialize(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final HttpSession session,
                                    final ServletContext application)
      throws IOException, RuntimeException {
    LOGGER.trace("Top of initialize");

    // make sure hostname updates happen right away
    WebUtils.scheduleHostnameUpdateIfNeeded(application);

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

        // setup special authentication for setup
        AuthenticationContext auth = AuthenticationContext.inSetup();
        session.setAttribute(SessionAttributes.AUTHENTICATION, auth);

        return false;
      }

      if (null == ApplicationAttributes.getAttribute(application, ApplicationAttributes.CHALLENGE_DESCRIPTION,
                                                     ChallengeDescription.class)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Loading challenge descriptor");
        }
        try {
          // load the challenge descriptor
          final ChallengeDescription challengeDescription = GlobalParameters.getChallengeDescription(connection);

          application.setAttribute(ApplicationAttributes.CHALLENGE_DESCRIPTION, challengeDescription);
        } catch (final FLLRuntimeException e) {
          LOGGER.error("Error getting challenge document", e);
          SessionAttributes.appendToMessage(session, "<p class='error'>"
              + e.getMessage()
              + " Please create the database.</p>");

          response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
              + "/setup/index.jsp"));

          // setup special authentication for setup
          AuthenticationContext auth = AuthenticationContext.inSetup();
          session.setAttribute(SessionAttributes.AUTHENTICATION, auth);

          return false;
        }
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

    LOGGER.trace("Bottom of initialize returning true");
    return true;

  }

  /**
   * Verify that the authentication is valid. Logging out or refreshing as needed.
   * After this method is called the {@code session} will have the current
   * authentication information.
   * 
   * @param application application variable store
   * @param session session variable store
   */
  private static void checkAuthenticationValid(final ServletContext application,
                                               final HttpSession session) {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    final Map<String, LocalDateTime> authLoggedOut = ApplicationAttributes.getAuthLoggedOut(application);

    final LocalDateTime loggedOut = authLoggedOut.get(auth.getUsername());
    if (null != loggedOut
        && loggedOut.isAfter(auth.getCreated())) {
      LOGGER.debug("User {} was logged out in another session. Logout time {} is after {}", auth.getUsername(),
                   loggedOut, auth.getCreated());
      AuthenticationContext newAuth = AuthenticationContext.notLoggedIn();
      session.setAttribute(SessionAttributes.AUTHENTICATION, newAuth);
    }

    final Map<String, LocalDateTime> authRefresh = ApplicationAttributes.getAuthRefresh(application);
    final LocalDateTime refresh = authRefresh.get(auth.getUsername());
    if (null != refresh
        && refresh.isAfter(auth.getCreated())) {
      LOGGER.debug("User {} needs authentication refreshed. Refresh time {} is after {}", auth.getUsername(), refresh,
                   auth.getCreated());
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {
        final Set<UserRole> roles = Authentication.getRoles(connection, auth.getUsername());
        final AuthenticationContext newAuth = AuthenticationContext.loggedIn(auth.getUsername(), roles);
        session.setAttribute(SessionAttributes.AUTHENTICATION, newAuth);
      } catch (final SQLException e) {
        throw new FLLInternalException("Error refreshing authentication information for "
            + auth.getUsername(), e);
      }
    }
  }

}
