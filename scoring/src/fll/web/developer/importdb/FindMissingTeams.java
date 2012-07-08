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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Team;
import fll.db.ImportDB;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Servlet to find teams that are missing via
 * {@link ImportDB#findMissingTeams(Connection, Connection, String)}.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/FindMissingTeams")
public class FindMissingTeams extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

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

      final DataSource destDataSource = ApplicationAttributes.getDataSource(application);
      destConnection = destDataSource.getConnection();

      final List<Team> missingTeams = ImportDB.findMissingTeams(sourceConnection, destConnection, tournament);
      if (missingTeams.isEmpty()) {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTeamInfo");
      } else {
        session.setAttribute("missingTeams", missingTeams);
        session.setAttribute(SessionAttributes.REDIRECT_URL, "promptCreateMissingTeams.jsp");
      }
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.close(sourceConnection);
      SQLFunctions.close(destConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getNonNullAttribute(session, "redirect_url",
                                                                                           String.class)));
  }

}
