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
import java.util.Set;

import javax.sql.DataSource;

import fll.Tournament;
import fll.db.CategoryColumnMapping;
import fll.db.Queries;
import fll.db.TableInformation;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Commit the schedule in {@link UploadScheduleData#getSchedule()} to the
 * database for the
 * current tournament.
 */
@WebServlet("/schedule/CommitSchedule")
public class CommitSchedule extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final TournamentSchedule schedule = uploadScheduleData.getSchedule();
      if (null == schedule) {
        throw new FLLInternalException("Schedule is not set");
      }

      schedule.storeSchedule(connection, tournament.getTournamentID());
      uploadScheduleData.getSchedParams().save(connection, tournament);

      assignJudgingGroups(connection, tournament.getTournamentID(), schedule);

      final Collection<CategoryColumnMapping> categoryColumnMappings = uploadScheduleData.getCategoryColumnMappings();

      CategoryColumnMapping.store(connection, tournament.getTournamentID(), categoryColumnMappings);

      // store table names
      final Collection<TableInformation> tables = new LinkedList<>();
      int id = 1;
      for (final String color : schedule.getTableColors()) {
        // making the sort index the same for all tables causes the tables to be sorted
        // by table color
        final TableInformation table = new TableInformation(id++, String.format("%s 1", color),
                                                            String.format("%s 2", color), 1);
        tables.add(table);
      }
      TableInformation.saveTournamentTableInformation(connection, tournament, tables);

      SessionAttributes.appendToMessage(session,
                                        "<p id='success' class='success'>Schedule successfully stored in the database</p>");
      WebUtils.sendRedirect(application, response, "/admin/index.jsp");
      return;
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new FLLRuntimeException("There was an error talking to the database", e);
    }
  }

  /**
   * Set judging station and award group to be the info from the schedule
   */
  private void assignJudgingGroups(final Connection connection,
                                   final int tournamentID,
                                   final TournamentSchedule schedule)
      throws SQLException {

    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      final String group = si.getJudgingGroup();
      final int teamNumber = si.getTeamNumber();
      Queries.updateTeamJudgingGroup(connection, teamNumber, tournamentID, group);
      Queries.updateTeamEventDivision(connection, teamNumber, tournamentID, si.getAwardGroup());
      Queries.updateTeamWave(connection, teamNumber, tournamentID, si.getWave());
    }

  }

}
