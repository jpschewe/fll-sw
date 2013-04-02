/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.playoff.Playoff;
import fll.web.report.finalist.FinalistSchedule;

/**
 * Context information for remoteControl.jsp.
 */
public class RemoteControl {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();
      final int currentTournament = Queries.getCurrentTournament(connection);

      final List<String> divisions = Playoff.getPlayoffDivisions(connection, currentTournament);
      pageContext.setAttribute("divisions", divisions);

      if (null == application.getAttribute("playoffDivision")
          && !divisions.isEmpty()) {
        application.setAttribute("playoffDivision", divisions.get(0));
      }
      if (null == application.getAttribute("playoffRoundNumber")) {
        application.setAttribute("playoffRoundNumber", new Integer(1));
      }
      if (null == application.getAttribute("slideShowInterval")) {
        application.setAttribute("slideShowInterval", new Integer(10));
      }
      if (null == application.getAttribute("displayPage")) {
        application.setAttribute("displayPage", "welcome");
      }

      pageContext.setAttribute("numPlayoffRounds", Queries.getNumPlayoffRounds(connection));

      final Collection<String> finalistDivisions = FinalistSchedule.getAllDivisions(connection, currentTournament);
      if (null == application.getAttribute("finalistDivision")
          && !finalistDivisions.isEmpty()) {
        application.setAttribute("finalistDivision", finalistDivisions.iterator().next());
      }
      pageContext.setAttribute("finalistDivisions", finalistDivisions);

    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    } finally {
      SQLFunctions.close(connection);
    }
  }
}
