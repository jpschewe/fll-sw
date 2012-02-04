/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Set the current tournament.
 * 
 */
@WebServlet("/admin/CommitTournamentInitialization")
public class CommitTournamentInitialization extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);

    PreparedStatement deletePrep = null;
    PreparedStatement insertPrep = null;
    try {
      final Connection connection = datasource.getConnection();
      
      deletePrep = connection.prepareStatement("DELETE FROM TournamentTeams WHERE TeamNumber IN ( SELECT TeamNumber FROM Teams WHERE Region = ? )");
      insertPrep = connection
                             .prepareStatement("INSERT INTO TournamentTeams (TeamNumber, Tournament, event_division) SELECT Teams.TeamNumber, ?, Teams.Division FROM Teams WHERE Teams.Region = ?");

      final Enumeration<String> paramNames = request.getParameterNames();
      while (paramNames.hasMoreElements()) {
        final String param = paramNames.nextElement().replaceAll("&amp;", "&");                                                                   
        if (!"verified".equals(param)
            && !"submit".equals(param)) {
          final String value = request.getParameter(param).replaceAll("&amp;", "&");
          if (!"nochange".equals(value)) {
            final Tournament tournament = Tournament.findTournamentByName(connection, value);
            final int newTournamentID = tournament.getTournamentID();
            deletePrep.setString(1, param);
            deletePrep.executeUpdate();

            insertPrep.setInt(1, newTournamentID);
            insertPrep.setString(2, param);
            insertPrep.executeUpdate();
          }
        }
      }

      message.append("<p id='success'><i>Successfully initialized tournament for teams based on region.</i></p>");
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(deletePrep);
      SQLFunctions.close(insertPrep);
    }

    session.setAttribute("message", message.toString());

    response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }
}
