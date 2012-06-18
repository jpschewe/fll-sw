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
 * Commit the changes made by chooseJudgingStation.jsp
 */
@WebServlet("/admin/CommitJudgingStation")
public class CommitJudgingStation extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    // store in session until final commit
    final String judgingStation = request.getParameter("judging_station");
    session.setAttribute(CheckJudgingStationNeeded.JUDGING_STATION, judgingStation);

    response.sendRedirect(response.encodeRedirectURL("SaveTeamData"));
  }
}
