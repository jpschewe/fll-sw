/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
 
package fll.web.ajax;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.util.JsonUtilities;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Send big screen display data via JavaScript, to avoid network-timeout induced freezes
 */
@WebServlet("/ajax/DisplayQuery")
public class DisplayQueryServlet extends BaseFLLServlet {
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    String localDisplayPage;
    String localDisplayURL;
    String displayName = SessionAttributes.getAttribute(session, "displayName", String.class);
    if (displayName != null) {
      String myDisplayPage = ApplicationAttributes.getAttribute(application, displayName + "_displayPage", String.class);
      String myDisplayURL = ApplicationAttributes.getAttribute(application, displayName + "_displayURL", String.class);
      localDisplayPage = myDisplayPage != null ? myDisplayPage : ApplicationAttributes.getAttribute(application, "displayPage", String.class);
      localDisplayURL = myDisplayURL != null ? myDisplayURL : ApplicationAttributes.getAttribute(application, "displayURL", String.class);
    } else {
      localDisplayPage = ApplicationAttributes.getAttribute(application, "displayPage", String.class);
      localDisplayURL = ApplicationAttributes.getAttribute(application, "displayURL", String.class);
    }
    response.getOutputStream().print(JsonUtilities.generateDisplayResponse(pickURL(localDisplayPage, localDisplayURL)));
  }
  private String pickURL(final String displayPage, final String displayURL) {
    if (displayPage == null) {
      return "/fll-sw/welcome.jsp";
    } else if (displayPage.equals("scoreboard")) {
      return "/fll-sw/scoreboard/main.jsp";
    } else if (displayPage.equals("slideshow")) {
      return "/fll-sw/slideshow/index.jsp";
    } else if (displayPage.equals("playoffs")) {
      return "/fll-sw/playoff/remoteMain.jsp";
    } else if (displayPage.equals("special")) {
      return "/fll-sw/" + displayURL;
    } else {
      return "/fll-sw/welcome.jsp";
    }
  }
}