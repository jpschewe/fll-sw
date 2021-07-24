/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.TournamentTeam;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Determine what changes there are in the schedule vs. the team information in
 * the database
 */
@WebServlet("/schedule/GatherTeamInformationChanges")
public class GatherTeamInformationChanges extends BaseFLLServlet {

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

    final Collection<TeamNameDifference> nameDifferences = new LinkedList<>();
    final Collection<TeamOrganizationDifference> organizationDifferences = new LinkedList<>();
    try (Connection connection = datasource.getConnection()) {
      final int tournamentID = Queries.getCurrentTournament(connection);

      final Map<Integer, TournamentTeam> dbTeams = Queries.getTournamentTeams(connection, tournamentID);

      final TournamentSchedule schedule = uploadScheduleData.getSchedule();
      if (null == schedule) {
        throw new FLLInternalException("schedule is not set");
      }
      schedule.getSchedule().stream().forEach(schedInfo -> {
        final TournamentTeam dbTeam = dbTeams.get(schedInfo.getTeamNumber());
        if (null == dbTeam) {
          throw new FLLInternalException("Team "
              + schedInfo.getTeamNumber()
              + " is in the schedule, but not in the TournamentTeams table in the database");
        }

        if (!schedInfo.getTeamName().equals(dbTeam.getTeamName())) {
          nameDifferences.add(new TeamNameDifference(schedInfo.getTeamNumber(), dbTeam.getTeamName(),
                                                     schedInfo.getTeamName()));
        }

        if (!schedInfo.getOrganization().equals(dbTeam.getOrganization())) {
          organizationDifferences.add(new TeamOrganizationDifference(schedInfo.getTeamNumber(),
                                                                     dbTeam.getOrganization(),
                                                                     schedInfo.getOrganization()));
        }
      });

      uploadScheduleData.setNameDifferences(nameDifferences);
      uploadScheduleData.setOrganizationDifferences(organizationDifferences);

    } catch (final SQLException e) {
      throw new FLLInternalException("Error talking to the database", e);
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }

    if (nameDifferences.isEmpty()
        && organizationDifferences.isEmpty()) {
      WebUtils.sendRedirect(application, response, "/schedule/CommitSchedule");
    } else {
      WebUtils.sendRedirect(application, response, "/schedule/promptTeamInformationChanges.jsp");
    }

  }

  /**
   * Helper class for tracking team name differences.
   */
  public static final class TeamNameDifference implements Serializable {

    /**
     * @param number see {@link #getNumber()}
     * @param oldName see {@link #getOldName()}
     * @param newName see {@link #getNewName()}
     */
    public TeamNameDifference(final int number,
                              final String oldName,
                              final String newName) {
      this.number = number;
      this.oldName = oldName;
      this.newName = newName;
    }

    private final int number;

    /**
     * @return team number
     */
    public int getNumber() {
      return number;
    }

    private final String oldName;

    /**
     * @return old name for team
     */
    public String getOldName() {
      return oldName;
    }

    private final String newName;

    /**
     * @return new name for team
     */
    public String getNewName() {
      return newName;
    }
  }

  /**
   * Helper class for tracking team organization differences.
   */
  public static final class TeamOrganizationDifference implements Serializable {
    /**
     * @param number see {@link #getNumber()}
     * @param oldOrganization see {@link #getOldOrganization()}
     * @param newOrganization see {@link #getNewOrganization()}
     */
    public TeamOrganizationDifference(final int number,
                                      final @Nullable String oldOrganization,
                                      final @Nullable String newOrganization) {
      this.number = number;
      this.oldOrganization = oldOrganization;
      this.newOrganization = newOrganization;
    }

    private final int number;

    /**
     * @return team number
     */
    public int getNumber() {
      return number;
    }

    private final @Nullable String oldOrganization;

    /**
     * @return old organization for team
     */
    public @Nullable String getOldOrganization() {
      return oldOrganization;
    }

    private final @Nullable String newOrganization;

    /**
     * @return new organization for team
     */
    public @Nullable String getNewOrganization() {
      return newOrganization;
    }
  }

}
