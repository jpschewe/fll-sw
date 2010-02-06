/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Servlet to create a tournament.
 * 
 * @author jpschewe
 */
public class CreateTournament extends BaseFLLServlet {

  private static final Logger LOG = Logger.getLogger(CreateTournament.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    try {
      final String answer = (String) request.getAttribute("submit");
      if ("Yes".equals(answer)) {
        createSelectedTournament(session);
      } else {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      }
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

  /**
   * Create the tournament that is in the session variable
   * <code>selectedTournament</code>.
   * 
   * @param session the session
   * @throws SQLException
   */
  private static void createSelectedTournament(final HttpSession session) throws SQLException {
    final StringBuilder message = new StringBuilder();

    Connection sourceConnection = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    Connection destConnection = null;
    try {
      final String tournament = SessionAttributes.getNonNullAttribute(session, "selectedTournament", String.class);
      final DataSource sourceDataSource = SessionAttributes.getNonNullAttribute(session, "dbimport", DataSource.class);
      sourceConnection = sourceDataSource.getConnection();

      final DataSource destDataSource = SessionAttributes.getDataSource(session);
      destConnection = destDataSource.getConnection();

      createTournament(sourceConnection, destConnection, tournament, message, session);

      session.setAttribute("message", message.toString());
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closeConnection(sourceConnection);
      SQLFunctions.closeConnection(destConnection);
    }
  }

  /**
   * Create a tournament from the source in the dest. This recurses on
   * nextTournament if needed.
   * @throws SQLException 
   */
  private static void createTournament(final Connection sourceConnection,
                                final Connection destConnection,
                                final String tournamentName,
                                final StringBuilder message,
                                final HttpSession session) throws SQLException {
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      sourcePrep = sourceConnection.prepareStatement("SELECT Location, NextTournament FROM Tournaments WHERE Name = ?");
      sourcePrep.setString(1, tournamentName);
      sourceRS = sourcePrep.executeQuery();

      if (sourceRS.next()) {
        final String location = sourceRS.getString(1);
        final int nextTournament = sourceRS.getInt(2);
        final boolean nextIsNull = sourceRS.wasNull();
        
        Queries.createTournament(destConnection, tournamentName, location);
        message.append("<p>Created tournament " + tournamentName + "</p>");
        
        if (!nextIsNull) {
          final String nextName = Queries.getTournamentName(destConnection, nextTournament);
          final List<String> knownTournaments = Queries.getTournamentNames(destConnection);
          if (!knownTournaments.contains(nextName)) {
            createTournament(sourceConnection, destConnection, nextName, message, session);
          }
          Queries.setNextTournament(destConnection, tournamentName, nextName);
        }
        session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTournamentExists");
      } else {
        message.append("<p class='error'>Cannot find tournament "
            + tournamentName + " in imported database!</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      }
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
    }
  }

}
