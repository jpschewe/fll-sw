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

      sourcePrep = sourceConnection.prepareStatement("SELECT Location, NextTournament FROM Tournaments WHERE Name = ?");
      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();

      if (sourceRS.next()) {
        final String location = sourceRS.getString(1);
        final String nextTournament = sourceRS.getString(2);
        if(null != nextTournament) {
          final List<String> knownTournaments = Queries.getTournamentNames(destConnection);
          if(!knownTournaments.contains(nextTournament)) {
            Queries.createTournament(destConnection, nextTournament, null);
            //FIXME need to create next tournament if it doesn't exist
            throw new RuntimeException("Need to create next tournament here also!");
          }
          final int next = Queries.getTournamentID(destConnection, nextTournament);
          Queries.createTournament(destConnection, tournament, location, next);
        } else {
          Queries.createTournament(destConnection, tournament, location);  
        }        
        session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTournamentExists");
      } else {
        message.append("<p class='error'>Cannot find tournament "
            + tournament + " in imported database!</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      }

      session.setAttribute("message", message.toString());
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closeConnection(sourceConnection);
      SQLFunctions.closeConnection(destConnection);
    }
  }
}
