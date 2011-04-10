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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Change the number of seeding rounds.
 * 
 * @web.servlet name="ChangeSeedingRounds"
 * @web.servlet-mapping url-pattern="/admin/ChangeSeedingRounds"
 */
public class ChangeSeedingRounds extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);

    try {
      final Connection connection = datasource.getConnection();

      final String seedingRoundsParam = request.getParameter("seedingRounds");
      if (null != seedingRoundsParam
          && !"".equals(seedingRoundsParam)) {
        final int newSeedingRounds = Integer.valueOf(seedingRoundsParam);
        if (newSeedingRounds < 0) {
          message.append("<p class='error'>Cannot have negative number of seeding rounds</p>");
        } else {
          final int tournament = Queries.getCurrentTournament(connection);
          Queries.setNumSeedingRounds(connection, tournament, newSeedingRounds);
          message.append(String.format("<p id='success'><i>Changed number of seeing rounds to %s</i></p>", newSeedingRounds));
        }
      } else {
        message.append("<p class='error'>You must specify the number of seeding rounds, ignoring request</p>");
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

    session.setAttribute("message", message.toString());

    response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }
}
