/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Commit the changes made by chooseEventDivision.jsp
 */
@WebServlet("/admin/CommitEventDivision")
public class CommitEventDivision extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
    final DataSource datasource = SessionAttributes.getDataSource(session);

    try {
      final Connection connection = datasource.getConnection();
      
      final String eventDivision = resolveDivision(request);
      final int teamNumber = SessionAttributes.getAttribute(session, GatherTeamData.TEAM_NUMBER, Integer.class);
      
      //FIXME implement this method
      //FIXME changes to judges?
      Queries.setEventDivision(connection, teamNumber, eventDivision);
      
      if (SessionAttributes.getNonNullAttribute(session, GatherTeamData.ADD_TEAM, Boolean.class)) {
        response.sendRedirect(response.encodeRedirectURL("index.jsp"));
      } else {
        response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

  }

  /**
   * Figure out what the division is based on the value of the "division"
   * parameter and possibly the "division_text" parameter.
   */
  private String resolveDivision(final HttpServletRequest request) {
    final String div = request.getParameter("event_division");
    if ("text".equals(div)) {
      return request.getParameter("event_division_text");
    } else {
      return div;
    }
  }
  
}
