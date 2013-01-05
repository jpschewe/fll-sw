/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Index page for playoffs.
 */
@WebServlet("/playoff/index.jsp")
public class PlayoffIndex extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  public static final String CREATE_NEW_PLAYOFF_DIVISION = "Create Playoff Division...";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }
    
    // cleanup session a bit
    session.removeAttribute(InitializeBrackets.ENABLE_THIRD_PLACE);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    ResultSet rs = null;
    ResultSet rs2 = null;
    Statement stmt = null;
    PreparedStatement prep = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      stmt = connection.createStatement();

      
      final int currentTournamentID = Queries.getCurrentTournament(connection);
      session.setAttribute("currentTournamentID", currentTournamentID);

      final List<String> divisions = Playoff.getPlayoffDivisions(connection, currentTournamentID);
      
      // add the option of creating new event divisions
      divisions.add(CREATE_NEW_PLAYOFF_DIVISION);
      
      session.setAttribute("eventDivisions", divisions);

      
      final List<String> playoffDivisions = Playoff.getPlayoffDivisions(connection, currentTournamentID);
      session.setAttribute("playoffDivisions", playoffDivisions);

      final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection);
      session.setAttribute("numPlayoffRounds", numPlayoffRounds);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(rs2);
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }

    session.setAttribute("servletLoaded", true);

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("playoff-index.jsp"));
  }

}
