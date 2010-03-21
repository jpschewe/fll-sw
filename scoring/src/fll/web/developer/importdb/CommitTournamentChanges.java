/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
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

import fll.Tournament;
import fll.db.Queries;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Commit changes made on resolveMissingTeams.jsp.
 * 
 * @author jpschewe
 * @web.servlet name="CommitTournamentChanges"
 * @web.servlet-mapping url-pattern="/developer/importdb/CommitTournamentChanges"
 */
public class CommitTournamentChanges extends BaseFLLServlet {

  private static final Logger LOG = Logger.getLogger(CommitTournamentChanges.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    Connection sourceConnection = null;
    Connection destConnection = null;
    try {
      final DataSource sourceDataSource = SessionAttributes.getNonNullAttribute(session, "dbimport", DataSource.class);
      sourceConnection = sourceDataSource.getConnection();

      final DataSource destDataSource = SessionAttributes.getDataSource(session);
      destConnection = destDataSource.getConnection();

      @SuppressWarnings(value = "unchecked")
      final List<TournamentDifference> tournamentDifferences = SessionAttributes.getNonNullAttribute(session, "tournamentDifferences", List.class);
      for(int idx=0; idx<tournamentDifferences.size(); ++idx) {
        final TournamentDifference difference = tournamentDifferences.get(idx);
        final String userChoice = request.getParameter(String.valueOf(idx));
        if(null == userChoice) {
          throw new RuntimeException("Missing paramter '" + idx + "' when committing tournament change");
        } else if("source".equals(userChoice)) {
          final String sourceName = difference.getSourceTournament();
          final Tournament destTournament = Tournament.findTournamentByName(destConnection, sourceName);
          final int tournamentID = destTournament.getTournamentID();
          Queries.changeTeamCurrentTournament(destConnection, difference.getTeamNumber(), tournamentID);
        } else if("dest".equals(userChoice)) {
          final String name = difference.getDestTournament();
          final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, name);
          final int tournamentID = sourceTournament.getTournamentID();
          Queries.changeTeamCurrentTournament(sourceConnection, difference.getTeamNumber(), tournamentID);
        } else {
          throw new RuntimeException(String.format("Unknown value '%s' for choice of parameter '%d'", userChoice, idx));
        }                  
      }
      
      session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTournamentTeams");
      
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.closeConnection(sourceConnection);
      SQLFunctions.closeConnection(destConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

}
