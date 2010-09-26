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

import fll.db.Queries;
import fll.db.TeamPropertyDifference;
import fll.db.TeamPropertyDifference.TeamProperty;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Commit changes made on resolveMissingTeams.jsp.
 * 
 * @author jpschewe
 * @web.servlet name="CommitTeamChanges"
 * @web.servlet-mapping url-pattern="/developer/importdb/CommitTeamChanges"
 */
public class CommitTeamChanges extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

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
      final List<TeamPropertyDifference> teamDifferences = SessionAttributes.getNonNullAttribute(session, "teamDifferences", List.class);
      for(int idx=0; idx<teamDifferences.size(); ++idx) {
        final TeamPropertyDifference difference = teamDifferences.get(idx);
        final String userChoice = request.getParameter(String.valueOf(idx));
        if(null == userChoice) {
          throw new RuntimeException("Missing paramter '" + idx + "' when committing team change");
        } else if("source".equals(userChoice)) {
          applyDifference(destConnection, difference.getTeamNumber(), difference.getProperty(), difference.getSourceValue());
        } else if("dest".equals(userChoice)) {
          applyDifference(sourceConnection, difference.getTeamNumber(), difference.getProperty(), difference.getDestValue());
        } else {
          throw new RuntimeException(String.format("Unknown value '%s' for choice of parameter '%d'", userChoice, idx));
        }
                  
      }
      
      session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTeamInfo");
      
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.close(sourceConnection);
      SQLFunctions.close(destConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

  /**
   * Apply a change to a database.
   * 
   * @param connection the database to change
   * @param teamNumber the team to change
   * @param property the property on the team to change
   * @param value the new value
   */
  private void applyDifference(final Connection connection, final int teamNumber, final TeamProperty property, final String value) throws SQLException {
    switch(property) {
    case NAME:
      Queries.updateTeamName(connection, teamNumber, value);
      break;
    case ORGANIZATION:
      Queries.updateTeamOrganization(connection, teamNumber, value);
      break;
    case REGION:
      Queries.updateTeamRegion(connection, teamNumber, value);
      break;
    case DIVISION:
      Queries.updateTeamDivision(connection, teamNumber, value);
      break;
    default:
      throw new IllegalArgumentException("Unknown property " + property);
    }
    
  }
}
