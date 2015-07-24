/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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
import fll.db.TournamentParameters;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Change the number of seeding rounds.
 * This should be removed, however TestAJAXBrackets and TestPlayoffs need to be
 * modified first to use the generic edit parameters page before this can go away.
 */
@WebServlet("/admin/ChangeSeedingRounds")
public class ChangeSeedingRounds extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final int tournament = Queries.getCurrentTournament(connection);

      if (Queries.isPlayoffDataInitialized(connection, tournament)) {
        message.append("<p class='error'>You cannot change the number of seeding rounds once the playoffs are initialized</p>");
      } else {
        final String seedingRoundsParam = request.getParameter("seedingRounds");
        if (null != seedingRoundsParam
            && !"".equals(seedingRoundsParam)) {
          final int newSeedingRounds = Integer.parseInt(seedingRoundsParam);
          if (newSeedingRounds < 0) {
            message.append("<p class='error'>Cannot have negative number of seeding rounds</p>");
          } else {
            TournamentParameters.setNumSeedingRounds(connection, tournament, newSeedingRounds);
            message.append(String.format("<p id='success'><i>Changed number of seeing rounds to %s</i></p>",
                                         newSeedingRounds));
          }
        } else {
          message.append("<p class='error'>You must specify the number of seeding rounds, ignoring request</p>");
        }
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute("message", message.toString());

    response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }
}
