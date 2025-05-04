/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.TournamentData;
import fll.web.WebUtils;
import fll.web.api.AwardsReportSortedGroupsServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Edit the order of the award groups.
 * 
 * @see AwardsReportSortedGroupsServlet
 */
@WebServlet("/report/EditAwardGroupOrder")
public class EditAwardGroupOrder extends BaseFLLServlet {

  /**
   * @param application application variables
   * @param page page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final DataSource datasource = tournamentData.getDataSource();
    try (Connection connection = datasource.getConnection()) {

      final List<String> groups = AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                       tournamentData.getCurrentTournament()
                                                                                                     .getTournamentID());
      page.setAttribute("groups", groups);

    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

  private record GroupSort(String group,
                           int sortValue)
      implements Comparable<GroupSort> {

    @Override
    public int compareTo(final GroupSort other) {
      final int sortValueCompare = Integer.compare(this.sortValue(), other.sortValue());
      if (0 == sortValueCompare) {
        return this.group().compareTo(other.group());
      } else {
        return sortValueCompare;
      }
    }
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final DataSource datasource = tournamentData.getDataSource();
    try (Connection connection = datasource.getConnection()) {

      final List<String> groups = AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                       tournamentData.getCurrentTournament()
                                                                                                     .getTournamentID());
      final List<GroupSort> groupSorts = new LinkedList<>();
      for (final String group : groups) {
        final int sortValue = WebUtils.getIntRequestParameter(request, group);
        final GroupSort groupSort = new GroupSort(group, sortValue);
        groupSorts.add(groupSort);
      }

      final List<String> sortedGroups = groupSorts.stream().sorted().map(GroupSort::group).toList();
      AwardsReportSortedGroupsServlet.setAwardGroupsSort(connection,
                                                         tournamentData.getCurrentTournament().getTournamentID(),
                                                         sortedGroups);

      WebUtils.sendRedirect(response, session);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

}
