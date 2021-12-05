/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Formatter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import fll.Utilities;
import fll.Version;

/**
 * Ensure that all HTML pages get the same navbar and footer.
 */
@WebFilter("/*")
public class FooterFilter implements Filter {

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
      final ByteResponseWrapper wrapper = new ByteResponseWrapper(httpResponse);
      chain.doFilter(request, wrapper);

      final String path = httpRequest.getRequestURI();

      final String contentType = wrapper.getContentType();
      LOGGER.debug("Page: {} content type: {}", httpRequest.getRequestURL(), contentType);

      if (wrapper.isStringUsed()) {
        if (null != contentType
            && contentType.startsWith("text/html")) {
          final String url = httpRequest.getRequestURI();

          final String origStr = wrapper.getString();

          final PrintWriter writer = response.getWriter();

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

            if (!noNavbar(url)) {
              addNavbar(caw, httpRequest);
            } else {
              LOGGER.debug("Skipping navbar");
            }

            caw.write(origStr.substring(endOfBodyTagIndex, bodyEndIndex
                - 1));

            if (!noFooter(url)) {
              if (path.startsWith(httpRequest.getContextPath()
                  + "/public")) {
                addPublicFooter(caw);
              } else {
                addFooter(caw, httpRequest);
              }
            }

            caw.write(origStr.substring(bodyEndIndex, origStr.length()));

            final String modified = caw.toString();
            response.setContentLength(modified.getBytes(Utilities.DEFAULT_CHARSET).length);
            writer.print(modified);
          } else {
            LOGGER.debug("No navbar/footer");

            writer.print(origStr);
          }
          writer.close();
        } else {
          final String origStr = wrapper.getString();
          LOGGER.debug("non-html text page: {} content type: {}", httpRequest.getRequestURL(), contentType);

          final PrintWriter writer = response.getWriter();
          writer.print(origStr);
          writer.close();
        }
      } else if (wrapper.isBinaryUsed()) {
        LOGGER.debug("binary page: {} content type: {}", httpRequest.getRequestURL(), contentType);

        final byte[] origData = wrapper.getBinary();
        final ServletOutputStream out = response.getOutputStream();
        out.write(origData);
        out.close();
      } else {
        LOGGER.debug("No output stream used, just returning page: {}", httpRequest.getRequestURL());
      }
    } else {
      LOGGER.debug("Not HttpRequest ({}) and HttpRespones ({})", request.getClass(), response.getClass());
      chain.doFilter(request, response);
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
    } else {
      return false;
    }
  }

  /**
   * @param url the url to check
   * @return true for all urls that should have no footer
   */
  private static boolean noNavbar(final String url) {

    if (url.indexOf("/public") != -1) {
      return true;
    } else if (url.endsWith("welcome.jsp")) {
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
    } else if (url.indexOf("login.jsp") != -1) {
      return true;
    } else if (url.indexOf("finalist/load.jsp") != -1) {
      return true;
    } else if (url.indexOf("scoreEntry/scoreEntry.jsp") != -1) {
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
                                final HttpServletRequest request)
      throws IOException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    final String contextPath = request.getContextPath();
    final Formatter formatter = new Formatter(caw);

    formatter.format("<div class='navbar'>%n");
    formatter.format("  <ul>%n");
    formatter.format("    <li><a href='%s/index.jsp'>Main Index</a></li>%n", contextPath);

    if (auth.isAdmin()) {
      formatter.format("    <li><a href='%s/admin/performance-area.jsp'>Scoring Coordinator</a></li>%n", contextPath);
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

    formatter.format("    <li class='dropdown'>%n");
    formatter.format("      <a href='' class='dropbtn'>Scoreboard</a>%n");
    formatter.format("      <div class='dropdown-content'>%n");
    formatter.format("        <a href='%s/scoreboard/allteams.jsp'>All Teams, All Performance Runs</a>%n", contextPath);
    formatter.format("        <a href='%s/scoreboard/Last8'>Most recent performance scores</a>%n", contextPath);
    formatter.format("        <a href='%s/scoreboard/Top10'>Top scores</a>%n", contextPath);
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

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // nothing
  }

}
