/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.web.BaseFLLServlet;

/**
 * Commit the changes made by chooseEventDivision.jsp
 */
@WebServlet("/admin/CommitEventDivision")
public class CommitEventDivision extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    // store in session until final commit
    final String eventDivision = request.getParameter("event_division");
    session.setAttribute(CommitTeam.EVENT_DIVISION, eventDivision);

    // FIXME redirect to judging station check
    response.sendRedirect(response.encodeRedirectURL("SaveTeamData"));

  }
}
