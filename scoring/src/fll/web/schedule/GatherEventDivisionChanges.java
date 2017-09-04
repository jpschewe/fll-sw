/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Get the changes in event divisions.
 */
@WebServlet("/schedule/GatherEventDivisionChanges")
public class GatherEventDivisionChanges extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final TournamentSchedule schedule = uploadScheduleData.getSchedule();
      Objects.requireNonNull(schedule);

      final List<EventDivisionInfo> eventDivisionInfo = new LinkedList<EventDivisionInfo>();
      for (final TeamScheduleInfo si : schedule.getSchedule()) {
        final Team team = Team.getTeamFromDatabase(connection, si.getTeamNumber());
        if (null == team) {
          throw new FLLRuntimeException("Team "
              + si.getTeamNumber()
              + " could not be found in the database");
        }
        final EventDivisionInfo info = new EventDivisionInfo(team.getTeamNumber(), team.getTeamName(),
                                                             si.getAwardGroup());
        eventDivisionInfo.add(info);
      }
      uploadScheduleData.setEventDivisionInfo(eventDivisionInfo);

      WebUtils.sendRedirect(application, response, "/schedule/displayEventDivisionConfirmation.jsp");
      return;
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new FLLRuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }

  }
}
