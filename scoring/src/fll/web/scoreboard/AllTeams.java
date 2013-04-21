/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import fll.web.SessionAttributes;

/**
 * Populate the context for allteams.jsp.
 */
public class AllTeams {

  public static void populateContext(final HttpServletRequest request,
                                     final HttpSession session) {
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
