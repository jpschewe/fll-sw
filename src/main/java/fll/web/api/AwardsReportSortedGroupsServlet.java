/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Collection of names of the groups used in the awards report in sorted order.
 */
@WebServlet("/api/AwardsReportSortedGroups")
public class AwardsReportSortedGroupsServlet extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentId = Queries.getCurrentTournament(connection);

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final List<String> groups = getAwardGroupsSorted(connection, tournamentId);

      // make sure the award groups are in the list
      for (final String ag : Queries.getAwardGroups(connection, tournamentId)) {
        if (!groups.contains(ag)) {
          groups.add(ag);
        }
      }

      jsonMapper.writeValue(writer, groups);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.isAdmin()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    response.reset();
    response.setContentType("application/json");

    final ServletContext application = getServletContext();

    final StringWriter debugWriter = new StringWriter();
    request.getReader().transferTo(debugWriter);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Read data: "
          + debugWriter.toString());
    }

    final Reader reader = new StringReader(debugWriter.toString());
    final PrintWriter writer = response.getWriter();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournamentId = Queries.getCurrentTournament(connection);

      final List<String> groups = jsonMapper.readValue(reader, Utilities.ListOfStringTypeInformation.INSTANCE);

      setAwardGroupsSort(connection, tournamentId, groups);

      final PostResult result = new PostResult(true, Optional.empty());
      jsonMapper.writeValue(writer, result);

    } catch (final SQLException e) {
      final PostResult result = new PostResult(false, Optional.ofNullable(e.getMessage()));
      jsonMapper.writeValue(writer, result);

      throw new FLLRuntimeException(e);
    } catch (final JsonProcessingException e) {
      final PostResult result = new PostResult(false, Optional.ofNullable(e.getMessage()));
      jsonMapper.writeValue(writer, result);

      throw new FLLRuntimeException(e);
    }
  }

  /**
   * Get the list of award groups in the order the user has specified.
   * This can include groups such as "wildcard" used for advancing teams.
   * 
   * @param connection the database connection
   * @param tournamentId the tournament to get the order for
   * @return the award groups sorted by the order and then alphabetically
   * @throws SQLException on a database error
   * @see #setAwardGroupsSort(Connection, int, List)
   */
  public static List<String> getAwardGroupsSorted(final Connection connection,
                                                  final int tournamentId)
      throws SQLException {
    final List<String> result = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT award_group FROM award_group_order" //
        + " WHERE tournament_id = ?" //
        + " ORDER BY sort_order ASC, award_group ASC ")) {
      prep.setInt(1, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String awardGroup = rs.getString("award_group");
          result.add(awardGroup);
        }
      }
    }
    return result;
  }

  /**
   * @param connection the database connection
   * @param tournamentId the tournament to store the data for
   * @param awardGroups the award groups in the desired order
   * @throws SQLException on a database error
   * @see #getAwardGroupsSorted(Connection, int)
   */
  public static void setAwardGroupsSort(final Connection connection,
                                        final int tournamentId,
                                        final List<String> awardGroups)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM award_group_order WHERE tournament_id = ?");
        PreparedStatement prep = connection.prepareStatement("INSERT INTO award_group_order (tournament_id, award_group, sort_order)" //
            + " VALUES(?, ?, ?)")) {
      delete.setInt(1, tournamentId);
      delete.executeUpdate();

      prep.setInt(1, tournamentId);
      int index = 0;
      for (final String group : awardGroups) {
        prep.setString(2, group);
        prep.setInt(3, index);
        prep.executeUpdate();

        ++index;
      }
    }
  }

}
