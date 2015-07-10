/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.ajax;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.DisplayNames;
import fll.web.SessionAttributes;

/**
 * Send big screen display data via JavaScript, to avoid network-timeout induced
 * freezes
 */
@WebServlet("/ajax/DisplayQuery")
public class DisplayQueryServlet extends BaseFLLServlet {
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final String localDisplayPage;
    final String localDisplayURL;
    final String displayName = SessionAttributes.getAttribute(session, "displayName", String.class);
    if (displayName != null) {
      // update last seen time
      DisplayNames.appendDisplayName(application, session, displayName);

      final String myDisplayPage = ApplicationAttributes.getAttribute(application, displayName
          + "_displayPage", String.class);
      final String myDisplayURL = ApplicationAttributes.getAttribute(application, displayName
          + "_displayURL", String.class);

      localDisplayPage = myDisplayPage != null ? myDisplayPage
          : ApplicationAttributes.getAttribute(application, ApplicationAttributes.DISPLAY_PAGE, String.class);

      localDisplayURL = myDisplayURL != null ? myDisplayURL : ApplicationAttributes.getAttribute(application,
                                                                                                 "displayURL",
                                                                                                 String.class);
    } else {
      localDisplayPage = ApplicationAttributes.getAttribute(application, ApplicationAttributes.DISPLAY_PAGE,
                                                            String.class);
      localDisplayURL = ApplicationAttributes.getAttribute(application, "displayURL", String.class);
    }

    final String url = pickURL(request, localDisplayPage, localDisplayURL, application, session);
    final DisplayResponse displayResponse = new DisplayResponse(url);

    final ObjectMapper jsonMapper = new ObjectMapper();

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    jsonMapper.writeValue(writer, displayResponse);
  }

  /**
   * Convert displayPage variable into URL. The names here need to match the
   * values
   * of the "remotePage" radio buttons in remoteControl.jsp.
   */
  private String pickURL(final HttpServletRequest request,
                         final String displayPage,
                         final String displayURL,
                         final ServletContext application,
                         final HttpSession session) {
    final String contextPath = request.getContextPath();

    if (null == displayPage) {
      return contextPath
          + "/welcome.jsp";
    } else if ("scoreboard".equals(displayPage)) {
      return contextPath
          + "/scoreboard/main.jsp";
    } else if ("slideshow".equals(displayPage)) {
      return contextPath
          + "/slideshow/index.jsp";
    } else if ("playoffs".equals(displayPage)) {
      return contextPath
          + "/playoff/remoteMain.jsp";
    } else if ("finalistSchedule".equals(displayPage)) {
      try {
        String finalistScheduleDivision = null;

        final String displayName = SessionAttributes.getAttribute(session, "displayName", String.class);
        if (null != displayName) {
          finalistScheduleDivision = ApplicationAttributes.getAttribute(application, displayName
              + "_finalistScheduleDivision", String.class);
        }

        if (null == finalistScheduleDivision) {
          finalistScheduleDivision = ApplicationAttributes.getAttribute(application, "finalistDivision", String.class);
        }

        return String.format("%s/report/finalist/PublicFinalistDisplaySchedule.jsp?finalistScheduleScroll=true&division=%s",
                             contextPath, URLEncoder.encode(finalistScheduleDivision, Utilities.DEFAULT_CHARSET.name()));
      } catch (final UnsupportedEncodingException e) {
        throw new FLLInternalException("Cannot encode using default charset?", e);
      }
    } else if ("special".equals(displayPage)) {
      return contextPath
          + "/" + displayURL;
    } else {
      return contextPath
          + "/welcome.jsp";
    }
  }

  /**
   * Response to the query servlet.
   */
  public static class DisplayResponse {
    @SuppressFBWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public final String displayURL;

    public String getDisplayURL() {
      return this.displayURL;
    }

    public DisplayResponse(@JsonProperty("displayURL") final String displayURL) {
      this.displayURL = displayURL;
    }
  }
}
