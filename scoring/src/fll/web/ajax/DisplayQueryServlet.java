/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.ajax;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.web.BaseFLLServlet;
import fll.web.DisplayInfo;

/**
 * Send big screen display data via JavaScript, to avoid network-timeout induced
 * freezes
 */
@WebServlet("/ajax/DisplayQuery")
public class DisplayQueryServlet extends BaseFLLServlet {
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);

    final String url = pickURL(displayInfo, request);
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
          + "/slideshow/index.jsp";
    } else if (displayInfo.isHeadToHead()) {
      return contextPath
          + "/playoff/remoteMain.jsp";
    } else if (displayInfo.isFinalistSchedule()
        && null != displayInfo.getFinalistScheduleAwardGroup()) {
      return String.format("%s/report/finalist/PublicFinalistDisplaySchedule.jsp?finalistScheduleScroll=true",
                           contextPath);
    } else if (displayInfo.isFinalistTeams()) {
      return contextPath
          + "/report/finalist/FinalistTeams.jsp?finalistTeamsScroll=true";
    } else if (displayInfo.isSpecial()) {
      return contextPath
          + "/" + displayInfo.getSpecialUrl();
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
