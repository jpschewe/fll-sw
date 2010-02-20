/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.ImportDB;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Servlet to do the actual import.
 * 
 * @author jpschewe
 * @web.servlet name="ExecuteImport"
 * @web.servlet-mapping url-pattern="/developer/importdb/ExecuteImport"
 */
public class ExecuteImport extends BaseFLLServlet {

  private static final Logger LOG = Logger.getLogger(ExecuteImport.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    Connection sourceConnection = null;
    Connection destConnection = null;
    try {
      final String tournament = SessionAttributes.getNonNullAttribute(session, "selectedTournament", String.class);
      final DataSource sourceDataSource = SessionAttributes.getNonNullAttribute(session, "dbimport", DataSource.class);
      sourceConnection = sourceDataSource.getConnection();
      
      final DataSource destDataSource = SessionAttributes.getDataSource(session);
      destConnection = destDataSource.getConnection();

      final boolean differences = ImportDB.checkForDifferences(sourceConnection, destConnection, tournament);
      if(differences) {
        message.append("<p class='error'>Error, there are still differences, cannot import. Try starting the workflow again.</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      } else {
        ImportDB.importDatabase(sourceConnection, destConnection, tournament);
        
        message.append(String.format("<p>Import of tournament %s successful. You may now optionally select another tournament to import.</p>", tournament));
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      }
      
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
