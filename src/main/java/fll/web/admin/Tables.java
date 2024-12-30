/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.db.TableInformation;
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
import jakarta.servlet.jsp.PageContext;

/**
 * Java code used in tables.jsp.
 */
@WebServlet("/admin/Tables")
public final class Tables extends BaseFLLServlet {

  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final List<TableInformation> tables = TableInformation.getTournamentTableInformation(connection, tournament);
      page.setAttribute("tables", tables);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

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

    final Collection<TableInformation> tables = new LinkedList<>();
    final Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
      final String paramName = paramNames.nextElement();
      if (paramName.startsWith("SideA")) {
        try {
          final int id = Integer.parseInt(paramName.substring("SideA".length()));

          parseTableInformation(request, tables, id);
        } catch (final NumberFormatException e) {
          throw new FLLInternalException("Unable to parse ID for table from: "
              + paramName, e);
        }
      }
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (

        Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      TableInformation.saveTournamentTableInformation(connection, tournament, tables);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error saving table information to the database", e);
    }

    SessionAttributes.appendToMessage(session, "<p id='success'><i>Successfully assigned tables</i></p>");
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

  private static void parseTableInformation(final HttpServletRequest request,
                                            final Collection<TableInformation> tables,
                                            final int id) {
    final @Nullable String deleteParam = request.getParameter(String.format("delete%d", id));
    if (null != deleteParam) {
      // delete is checked
      return;
    }

    final String sideA = WebUtils.getNonNullRequestParameter(request, String.format("SideA%d", id)).trim();
    if (StringUtils.isBlank(sideA)) {
      // skip blank cells
      return;
    }

    final String sideB = WebUtils.getNonNullRequestParameter(request, String.format("SideB%d", id)).trim();
    if (StringUtils.isBlank(sideB)) {
      // skip blank cells
      return;
    }

    final int sortOrder = WebUtils.getIntRequestParameter(request, String.format("sortOrder%d", id));
    final TableInformation table = new TableInformation(id, sideA, sideB, sortOrder);
    tables.add(table);
  }

  /**
   * Check if tables are assigned for the specified tournament.
   *
   * @param connection database connection
   * @param tournamentID tournament ID
   * @return true if the tables have been assigned
   * @throws SQLException on a database error
   */
  public static boolean tablesAssigned(final Connection connection,
                                       final int tournamentID)
      throws SQLException {
    boolean tablesAssigned = false;
    try (PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM tablenames WHERE Tournament = ?")) {
      prep.setInt(1, tournamentID);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int count = rs.getInt(1);
          tablesAssigned = count > 0;
        }
      }
      return tablesAssigned;
    }
  }

}
