/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
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

import org.apache.log4j.Logger;

import fll.db.CategoryColumnMapping;
import fll.db.Queries;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;
import fll.web.admin.Tables;
import net.mtu.eggplant.util.Pair;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Commit the schedule in {@link UploadScheduleData#getSchedule()} to the
 * database for the
 * current tournament.
 */
@WebServlet("/schedule/CommitSchedule")
public class CommitSchedule extends BaseFLLServlet {

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
      final int tournamentID = Queries.getCurrentTournament(connection);

      final TournamentSchedule schedule = uploadScheduleData.getSchedule();
      Objects.requireNonNull(schedule);

      schedule.storeSchedule(connection, tournamentID);

      assignJudgingGroups(connection, tournamentID, schedule);

      final Collection<CategoryColumnMapping> categoryColumnMappings = uploadScheduleData.getCategoryColumnMappings();

      CategoryColumnMapping.store(connection, tournamentID, categoryColumnMappings);

      // store table names
      final List<Pair<String, String>> tables = new LinkedList<>();
      for (final String color : schedule.getTableColors()) {
        tables.add(new Pair<>(color
            + " 1", color
                + " 2"));
      }
      Tables.replaceTablesForTournament(connection, tournamentID, tables);

      session.setAttribute(SessionAttributes.MESSAGE,
                           "<p id='success' class='success'>Schedule successfully stored in the database</p>");
      WebUtils.sendRedirect(application, response, "/admin/index.jsp");
      return;
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new FLLRuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Set judging_station to be the info from the schedule
   */
  private void assignJudgingGroups(final Connection connection,
                                   final int tournamentID,
                                   final TournamentSchedule schedule)
      throws SQLException {

    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      final String group = si.getJudgingGroup();
      final int teamNumber = si.getTeamNumber();
      Queries.updateTeamJudgingGroups(connection, teamNumber, tournamentID, group);
    }

  }

}
