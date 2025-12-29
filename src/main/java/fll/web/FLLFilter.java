/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Formatter;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.logging.log4j.CloseableThreadContext;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Utilities;
import fll.Version;
import fll.db.Authentication;
import fll.db.GlobalParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.xml.ChallengeDescription;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Filter for web responses.
 * Initialize web attributes.
 * Ensure that all HTML pages get the same navbar and footer.
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class FLLFilter implements Filter {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  public void doFilter(final ServletRequest request,
                       final ServletResponse response,
                       final FilterChain chain)
      throws IOException, ServletException {
    try (CloseableThreadContext.Instance requestCtx = CloseableThreadContext.push(request.getRequestId())) {

      if (response instanceof HttpServletResponse
          && request instanceof HttpServletRequest) {
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        final HttpServletRequest origHttpRequest = (HttpServletRequest) request;
        final String path = origHttpRequest.getRequestURI();
        final HttpSession session = origHttpRequest.getSession();

        try (CloseableThreadContext.Instance uriContext = CloseableThreadContext.push(path)) {

          LOGGER.trace("Redirect URL is {}", SessionAttributes.getRedirectURL(session));
          LOGGER.trace("forward.request_uri: {}", origHttpRequest.getAttribute("jakarta.servlet.forward.request_uri"));
          LOGGER.trace("forward.context_path: {}",
                       origHttpRequest.getAttribute("jakarta.servlet.forward.context_path"));
          LOGGER.trace("forward.servlet_path: {}",
                       origHttpRequest.getAttribute("jakarta.servlet.forward.servlet_path"));
          LOGGER.trace("forward.path_info: {}", origHttpRequest.getAttribute("jakarta.servlet.forward.path_info"));
          LOGGER.trace("forward.query_string: {}",
                       origHttpRequest.getAttribute("jakarta.servlet.forward.query_string"));

          LOGGER.debug("request content type: {} character encoding: {}", request.getContentType(),
                       request.getCharacterEncoding());

          final HttpServletRequest httpRequest = FormParameterStorage.applyParameters(origHttpRequest, session);

          final ServletContext application = session.getServletContext();

          // make sure the authentication is valid first
          checkAuthenticationValid(application, session);

          LOGGER.trace("Loading {} message: {} referer: {} session: {} auth: {}", path,
                       SessionAttributes.getMessage(session), httpRequest.getHeader("Referer"), session.getId(),
                       SessionAttributes.getAuthentication(session));

          final boolean needsInit = needsInit(httpRequest.getContextPath(), path);
          LOGGER.debug("needsInit: "
              + needsInit);

          if (needsInit) {
            if (!initialize(httpRequest, httpResponse, session, application)) {
              LOGGER.debug("Returning after initialize did redirect");
              return;
            }
          } else if (path.startsWith(httpRequest.getContextPath()
              + "/setup")) {
            possiblyInstallSetupAuthentication(application, session);
          }

          // keep browser from caching any content
          httpResponse.setHeader("Cache-Control", "no-store"); // HTTP 1.1
          httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
          httpResponse.setDateHeader("Expires", 0); // proxy server cache

          if (!noFooter(path)
              || request.isAsyncStarted()) {

            modifyResponseContent(path, httpRequest, httpResponse, chain, session);

          } else {
            LOGGER.debug("No footer filter. async?: {}", request.isAsyncStarted());
            chain.doFilter(request, response);
          }

        } // URI context
      } else {
        LOGGER.debug("Not HttpRequest ({}) and HttpResponse ({})", request.getClass(), response.getClass());
        chain.doFilter(request, response);
      }

    } // request ID context
  }

  private void modifyResponseContent(final String path,
                                     final HttpServletRequest httpRequest,
                                     final HttpServletResponse httpResponse,
                                     final FilterChain chain,
                                     final HttpSession session)
      throws IOException, ServletException {
    final ByteResponseWrapper wrapper = new ByteResponseWrapper(httpResponse);

    LOGGER.debug("Initial response character encoding: {}", httpResponse.getCharacterEncoding());

    chain.doFilter(httpRequest, wrapper);

    final String responseContentType = wrapper.getContentType();
    LOGGER.debug("response content type: {} character encoding: {}", wrapper.getContentType(),
                 wrapper.getCharacterEncoding());

    if (wrapper.isStringUsed()) {
      if (null != responseContentType
          && responseContentType.startsWith("text/html")) {

        final String origStr = wrapper.getString();

        try (PrintWriter writer = httpResponse.getWriter()) {

          final CharArrayWriter caw = new CharArrayWriter();
          final String bodyTag = "<body>";
          final int bodyIndex = origStr.indexOf(bodyTag);
          final int bodyEndIndex = origStr.indexOf("</body>");
          LOGGER.trace("Body index {} body end index {}", bodyIndex, bodyEndIndex);

          if (-1 != bodyIndex
              && -1 != bodyEndIndex) {

            final int endOfBodyTagIndex = bodyIndex
                + bodyTag.length();
            caw.write(origStr.substring(0, endOfBodyTagIndex));

            if (!noNavbar(path)) {
              addNavbar(caw, session, httpRequest);
            } else {
              LOGGER.debug("Skipping navbar");
            }

            caw.write(origStr.substring(endOfBodyTagIndex, bodyEndIndex
                - 1));

            if (path.startsWith(httpRequest.getContextPath()
                + "/public")) {
              addPublicFooter(caw);
            } else {
              addFooter(caw, httpRequest);
            }

            caw.write(origStr.substring(bodyEndIndex, origStr.length()));

            final String modified = caw.toString();
            httpResponse.setContentLength(modified.getBytes(Utilities.DEFAULT_CHARSET).length);
            writer.print(modified);
          } else {
            LOGGER.debug("No navbar/footer");

            writer.print(origStr);
          }

        } // writer allocation
      } else {
        LOGGER.debug("non-html text content type: {}", responseContentType);

        final String origStr = wrapper.getString();
        try (PrintWriter writer = httpResponse.getWriter()) {
          writer.print(origStr);
        }
      }
    } else if (wrapper.isBinaryUsed()) {
      LOGGER.debug("binary content type: {}. Data written directly.", responseContentType);
    } else {
      LOGGER.debug("No output stream used?");
    }
  }

  // CHECKSTYLE:OFF don't want conditional logic simplified
  /**
   * @param url the url to check
   * @return true for all urls that should have no footer
   */
  private static boolean noFooter(final String url) {
    if (url.endsWith("welcome.jsp")) {
      return true;
    } else if (url.indexOf("scoreboard") != -1
        && !url.endsWith("index.jsp")) {
      return true;
    } else if (url.indexOf("playoff/remoteMain.jsp") != -1) {
      return true;
    } else if (url.indexOf("playoff/title.jsp") != -1) {
      return true;
    } else if (url.indexOf("playoff/remoteControlBrackets.jsp") != -1) {
      return true;
    } else if (url.indexOf("playoff/sponsors.jsp") != -1) {
      return true;
    } else if (url.indexOf("report/finalist/FinalistTeams.jsp") != -1) {
      return true;
    } else if (url.indexOf("slideshow.jsp") != -1) {
      return true;
    } else if (url.indexOf("finalist/load.jsp") != -1) {
      return true;
    } else if (url.indexOf("scoreEntry/scoreEntry.jsp") != -1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param path the path to check
   * @return true for all pathsthat should have no footer
   */
  private static boolean noNavbar(final String path) {

    if (path.indexOf("/public") != -1) {
      return true;
    } else if (path.endsWith("welcome.jsp")) {
      return true;
    } else if (path.indexOf("scoreboard") != -1
        && !path.endsWith("index.jsp")) {
      return true;
    } else if (path.indexOf("playoff/remoteMain.jsp") != -1) {
      return true;
    } else if (path.indexOf("playoff/title.jsp") != -1) {
      return true;
    } else if (path.indexOf("playoff/remoteControlBrackets.jsp") != -1) {
      return true;
    } else if (path.indexOf("playoff/sponsors.jsp") != -1) {
      return true;
    } else if (path.indexOf("report/finalist/FinalistTeams.jsp") != -1) {
      return true;
    } else if (path.indexOf("slideshow.jsp") != -1) {
      return true;
    } else if (path.indexOf("login.jsp") != -1) {
      return true;
    } else if (path.indexOf("finalist/load.jsp") != -1) {
      return true;
    } else if (path.indexOf("scoreEntry/scoreEntry.jsp") != -1) {
      return true;
    } else {
      return false;
    }
  }
  // CHECKSTYLE:ON

  /**
   * Writer the footer to the char array writer.
   */
  private static void addFooter(final CharArrayWriter caw,
                                final HttpServletRequest request)
      throws IOException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    final Formatter formatter = new Formatter(caw);
    formatter.format("<hr />%n");
    formatter.format("<table style='border-spacing: 5px'>%n");

    if (auth.getLoggedIn()) {
      formatter.format("  <tr><td>Logged in as %s</td></tr>%n", auth.getUsername());
    } else {
      formatter.format("  <tr><td>Not logged in</td></tr>%n");
    }

    formatter.format("  <tr><td>Software version: %s</td></tr>%n", Version.getVersion());

    formatter.format("</table>%n");
  }

  /**
   * Writer the navbar to the char array writer.
   */
  private static void addNavbar(final CharArrayWriter caw,
                                final HttpSession session,
                                final HttpServletRequest request)
      throws IOException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    final String contextPath = request.getContextPath();
    final Formatter formatter = new Formatter(caw);

    formatter.format("<div class='navbar'>%n");
    formatter.format("  <ul>%n");
    formatter.format("    <li><a href='%s/index.jsp'>Main Index</a></li>%n", contextPath);

    if (auth.isScoringCoordinator()) {
      formatter.format("    <li><a href='%s/scoring-coordinator.jsp'>Scoring Coordinator</a></li>%n", contextPath);
    }

    if (auth.isAdmin()) {
      formatter.format("    <li><a href='%s/admin/index.jsp'>Admin</a></li>%n", contextPath);
    }

    if (auth.isHeadJudge()) {
      formatter.format("    <li><a href='%s/head-judge.jsp'>Head Judge</a></li>%n", contextPath);
    }

    if (auth.isJudge()) {
      formatter.format("    <li><a href='%s/judge-index.jsp'>Judge</a></li>%n", contextPath);
    }

    if (auth.isRef()) {
      formatter.format("    <li><a href='%s/ref-index.jsp'>Ref</a></li>%n", contextPath);
    }

    if (auth.isReportGenerator()) {
      formatter.format("    <li><a href='%s/tournament-reporter.jsp'>Tournament Reporter</a></li>%n", contextPath);
    }

    formatter.format("    <li class='dropdown'>%n");
    formatter.format("      <a href='' class='dropbtn'>Scoreboard</a>%n");
    formatter.format("      <div class='dropdown-content'>%n");
    formatter.format("        <a href='%s/scoreboard/dynamic.jsp?layout=all_teams_auto_scroll'>All Teams, All Performance Runs</a>%n",
                     contextPath);
    formatter.format("        <a href='%s/scoreboard/dynamic.jsp?layout=most_recent'>Most recent performance scores</a>%n",
                     contextPath);
    formatter.format("        <a href='%s/scoreboard/dynamic.jsp?layout=top_scores'>Top scores</a>%n", contextPath);
    formatter.format("      </div>%n");
    formatter.format("    </li>%n");

    if (!auth.getLoggedIn()) {
      formatter.format("    <li><a href='%s/login.jsp'>Login</a></li>%n", contextPath);
    }
    if (auth.getLoggedIn()) {
      formatter.format("    <li><a href='%s/DoLogout'>Logout</a></li>%n", contextPath);
    }

    formatter.format("  </ul>%n");
    formatter.format("</div>%n");
  }

  /**
   * Writer the footer for public pages to the char array writer.
   */
  private static void addPublicFooter(final CharArrayWriter caw) throws IOException {
    final Formatter formatter = new Formatter(caw);
    formatter.format("<hr />%n");
    formatter.format("<table>%n");
    formatter.format("  <tr><td>Software version: %s</td></tr>%n", Version.getVersion());
    formatter.format("</table>%n");
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
    if (path.startsWith(contextPath
        + "/style") //
        || path.startsWith(contextPath
            + "/images") //
        || path.startsWith(contextPath
            + "/"
            + WebUtils.SPONSOR_LOGOS_PATH) //
        || path.startsWith(contextPath
            + "/wiki") //
        || path.endsWith(".jpg") //
        || path.endsWith(".gif") //
        || path.endsWith(".png") //
        || path.endsWith(".html") //
        || path.endsWith(".ico")) {
      return false;
    } else if (path.startsWith(contextPath
        + "/setup")) {
      return false;
    } else if (path.startsWith(contextPath
        + "/robots.txt")) {
      return false;
    }
    return true;
  }

  private static void possiblyInstallSetupAuthentication(final ServletContext application,
                                                         final HttpSession session) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      // check if the database is initialized
      final boolean dbinitialized = Utilities.testDatabaseInitialized(connection);
      if (!dbinitialized) {
        // setup special authentication for setup
        LOGGER.info("No database, setting inSetup authentication");
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

    if (ApplicationAttributes.getNonNullAttribute(application, FLLContextListener.DATABASE_UPGRADE_FAILED,
                                                  Boolean.class)) {
      final String message = "In-place database upgrade failed. See the automatic backups for the previous database to send to the developers. Please create a new database.";
      LOGGER.warn(message);

      SessionAttributes.appendToMessage(session, "<p class='error'>"
          + message
          + "</p>");

      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/setup/index.jsp"));

      // setup special authentication for setup
      LOGGER.info("Setting in-setup authentication after failed database upgrade");
      AuthenticationContext auth = AuthenticationContext.inSetup();
      session.setAttribute(SessionAttributes.AUTHENTICATION, auth);

      // clear the failed state
      application.setAttribute(FLLContextListener.DATABASE_UPGRADE_FAILED, Boolean.FALSE);

      return false;
    }

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
        LOGGER.info("Setting in-setup authentication");
        AuthenticationContext auth = AuthenticationContext.inSetup();
        session.setAttribute(SessionAttributes.AUTHENTICATION, auth);

        return false;
      }

      if (null == ApplicationAttributes.getAttribute(application, ApplicationAttributes.CHALLENGE_DESCRIPTION,
                                                     ChallengeDescription.class)) {
        LOGGER.debug("Loading challenge descriptor from database");
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
          LOGGER.info("Setting in-setup authentication after error");
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
    if (!auth.getLoggedIn()) {
      // not logged in, nothing to check
      return;
    }

    // not null because logged in
    final String username = castNonNull(auth.getUsername());

    final Map<String, LocalDateTime> authLoggedOut = ApplicationAttributes.getAuthLoggedOut(application);

    final LocalDateTime loggedOut = authLoggedOut.get(username);
    if (null != loggedOut
        && loggedOut.isAfter(auth.getCreated())) {
      LOGGER.info("User {} was logged out in another session. Logout time {} is after {}", username, loggedOut,
                  auth.getCreated());
      AuthenticationContext newAuth = AuthenticationContext.notLoggedIn();
      session.setAttribute(SessionAttributes.AUTHENTICATION, newAuth);
    }

    final Map<String, LocalDateTime> authRefresh = ApplicationAttributes.getAuthRefresh(application);
    final LocalDateTime refresh = authRefresh.get(username);
    if (null != refresh
        && refresh.isAfter(auth.getCreated())) {
      LOGGER.info("User {} needs authentication refreshed. Refresh time {} is after {}", username, refresh,
                  auth.getCreated());
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {
        final Set<UserRole> roles = Authentication.getRoles(connection, username);
        final AuthenticationContext newAuth = AuthenticationContext.loggedIn(username, roles);
        session.setAttribute(SessionAttributes.AUTHENTICATION, newAuth);
      } catch (final SQLException e) {
        throw new FLLInternalException("Error refreshing authentication information for "
            + username, e);
      }
    }
  }
}
