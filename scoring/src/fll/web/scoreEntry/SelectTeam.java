/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Team;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

/**
 * Java code for scoreEntry/select_team.jsp.
 */
public class SelectTeam {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = "
          + Queries.getCurrentTournament(connection));
      final int maxRunNumber;
      if (rs.next()) {
        maxRunNumber = rs.getInt(1);
      } else {
        maxRunNumber = 1;
      }
      pageContext.setAttribute("maxRunNumber", maxRunNumber);

      final Collection<? extends Team> tournamentTeams = Queries.getTournamentTeams(connection).values();      
      pageContext.setAttribute("tournamentTeams", tournamentTeams);

    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);
    }

  }

}
