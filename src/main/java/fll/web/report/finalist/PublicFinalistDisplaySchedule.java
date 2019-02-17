/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;

/**
 * Support for PublicFinalistDisplaySchedule.jsp.
 */
public class PublicFinalistDisplaySchedule {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    String division = request.getParameter("division");
    if (null == division
        || "".equals(division)) {
      // if no parameters, then must have been called from the big screen display
      final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);
      division = displayInfo.getFinalistScheduleAwardGroup();
    }

    if (null == division) {
      throw new FLLRuntimeException("Cannot determine which division to display");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);

      final FinalistSchedule schedule = new FinalistSchedule(connection, tournament, division);
      final Collection<String> publicCategories = new LinkedList<String>();
      final Map<String, List<FinalistDBRow>> publicSchedules = new HashMap<String, List<FinalistDBRow>>();
      for (final Map.Entry<String, Boolean> entry : schedule.getCategories().entrySet()) {
        if (entry.getValue()) {
          publicCategories.add(entry.getKey());

          publicSchedules.put(entry.getKey(), schedule.getScheduleForCategory(entry.getKey()));
        }
      }
      pageContext.setAttribute("publicCategories", publicCategories);
      pageContext.setAttribute("publicSchedules", publicSchedules);
      pageContext.setAttribute("rooms", schedule.getRooms());
      pageContext.setAttribute("division", division);

      final Map<Integer, TournamentTeam> allTeams = Queries.getTournamentTeams(connection, tournament);
      pageContext.setAttribute("allTeams", allTeams);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }
  }
}
