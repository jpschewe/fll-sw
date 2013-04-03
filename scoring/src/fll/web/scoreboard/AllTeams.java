/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

import fll.util.LogUtils;
import fll.web.SessionAttributes;

/**
 * Populate the context for allteams.jsp.
 */
public class AllTeams {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    storeScroll(request, session);
  }
 
  /**
   * Get the scroll value and store it in the session.
   * 
   * @param request
   * @param session
   * @return
   */
  private static void storeScroll(final HttpServletRequest request,
                                  final HttpSession session) {
    String scrollStr = request.getParameter("allTeamsScroll");
    final boolean scroll;
    if (null == scrollStr
        || "".equals(scrollStr)) {
      final Boolean value = SessionAttributes.getAttribute(session, "allTeamsScroll", Boolean.class);
      if (null == value) {
        scroll = false;
      } else {
        scroll = value;
      }
    } else {
      scroll = Boolean.valueOf(scrollStr);
    }
    
    session.setAttribute("allTeamsScroll", scroll);
  }
}
