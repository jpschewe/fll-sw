/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.ajax;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.DisplayInfo;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Send big screen display data via JavaScript, to avoid network-timeout induced
 * freezes.
 */
@WebServlet("/ajax/DisplayQuery")
public class DisplayQueryServlet extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);

    final String url = pickURL(displayInfo, request);
    final DisplayResponse displayResponse = new DisplayResponse(url);

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

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
  private String pickURL(final DisplayInfo displayInfo,
                         final HttpServletRequest request) {
    final String contextPath = request.getContextPath();

    if (displayInfo.isWelcome()) {
      return contextPath
          + "/welcome.jsp";
    } else if (displayInfo.isScoreboard()) {
      return contextPath
          + "/scoreboard/main.jsp";
    } else if (displayInfo.isSlideshow()) {
      return contextPath
          + "/slideshow.jsp";
    } else if (displayInfo.isHeadToHead()) {
      return contextPath
          + "/playoff/remoteMain.jsp";
    } else if (displayInfo.isFinalistTeams()) {
      return contextPath
          + "/report/finalist/FinalistTeams.jsp?finalistTeamsScroll=true";
    } else if (displayInfo.isSpecial()) {
      return contextPath
          + "/custom/"
          + displayInfo.getSpecialUrl();
    } else {
      return contextPath
          + "/welcome.jsp";
    }
  }

  /**
   * Response to the query servlet.
   */
  public static class DisplayResponse {
    private final String displayURL;

    /**
     * @return {@link #displayURL}
     */
    public String getDisplayURL() {
      return this.displayURL;
    }

    /**
     * @param displayURL {@link #displayURL}
     */
    public DisplayResponse(@JsonProperty("displayURL") final String displayURL) {
      this.displayURL = displayURL;
    }
  }
}
