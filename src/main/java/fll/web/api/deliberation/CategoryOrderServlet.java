/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api.deliberation;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;
import fll.Utilities;
import fll.db.CategoriesIgnored;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.api.ApiResult;
import fll.web.report.awards.ChampionshipCategory;
import fll.web.report.finalist.FinalistSchedule;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.PerformanceScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Access the order of categories on the deliberation page.
 * The award group is the first path element.
 */
@WebServlet("/api/deliberation/CategoryOrder/*")
public class CategoryOrderServlet extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * List of string names in order.
   */
  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isReportGenerator()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Must be ReportGenerator");
      return;
    }

    final Optional<String> awardGroup = getAwardGroup("doGet", request, response);
    if (awardGroup.isEmpty()) {
      return;
    }

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final FinalistSchedule finalistSchedule = new FinalistSchedule(connection, tournament.getTournamentID(),
                                                                     awardGroup.get());
      final Set<String> scheduledCategoryNames = finalistSchedule.getCategoryNames();

      final List<String> categoryOrder = getCategoryOrder(connection, description, tournament, awardGroup.get(),
                                                          scheduledCategoryNames);

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();
      jsonMapper.writeValue(writer, categoryOrder);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Used for JSON deserialization.
   */
  private static final class TypeInformation extends TypeReference<List<String>> {
    /** single instance. */
    public static final TypeInformation INSTANCE = new TypeInformation();
  }

  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isReportGenerator()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Must be ReportGenereator");
      return;
    }

    final Optional<String> awardGroup = getAwardGroup("doPost", request, response);
    if (awardGroup.isEmpty()) {
      return;
    }

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final List<String> v = jsonMapper.readValue(request.getReader(), TypeInformation.INSTANCE);
      storeCategoryOrder(connection, tournament, awardGroup.get(), v);

      jsonMapper.writeValue(writer, new ApiResult(true, Optional.empty()));
    } catch (final SQLException e) {
      jsonMapper.writeValue(writer, new ApiResult(false, Optional.ofNullable(e.getMessage())));
      throw new FLLRuntimeException(e);
    } catch (final JsonProcessingException e) {
      jsonMapper.writeValue(writer, new ApiResult(false, Optional.ofNullable(e.getMessage())));
      throw new FLLRuntimeException(e);
    }
  }

  private void storeCategoryOrder(final Connection connection,
                                  final Tournament tournament,
                                  final String awardGroup,
                                  final List<String> categoryOrder)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM deliberation_category_order WHERE tournament_id = ? AND award_group = ?");
        PreparedStatement insert = connection.prepareStatement("INSERT INTO deliberation_category_order" //
            + " (tournament_id, award_group, category_name, sort_order)" //
            + " VALUES(?, ?, ?, ?)")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.setString(2, awardGroup);
      delete.executeUpdate();

      insert.setInt(1, tournament.getTournamentID());
      insert.setString(2, awardGroup);
      int order = 1;
      for (String name : categoryOrder) {
        insert.setString(3, name);
        insert.setInt(4, order);
        insert.executeUpdate();
        ++order;
      }
    }
  }

  private static List<String> getCategoryOrder(final Connection connection,
                                               final ChallengeDescription description,
                                               final Tournament tournament,
                                               final String awardGroup,
                                               final Set<String> scheduledCategoryNames)
      throws SQLException {
    final List<String> categoryOrder = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT category_name FROM deliberation_category_order" //
        + " WHERE tournament_id = ?" //
        + " AND award_group = ?" //
        + " ORDER BY sort_order ASC")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, awardGroup);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String categoryName = castNonNull(rs.getString(1));
          categoryOrder.add(categoryName);
        }
      }

    }

    // ensure all category names are in the return value
    for (final String name : getAllCategoryNames(description, connection, tournament, scheduledCategoryNames)) {
      if (!categoryOrder.contains(name)) {
        categoryOrder.add(name);
      }
    }

    return categoryOrder;
  }

  /**
   * Create the default list of category names using the default order.
   * Championship, subjective categories, scheduled non-numeric categories,
   * performance, non-scheduled non-numeric categories.
   */
  private static List<String> getAllCategoryNames(final ChallengeDescription description,
                                                  final Connection connection,
                                                  final Tournament tournament,
                                                  final Set<String> scheduledCategoryNames)
      throws SQLException {
    final List<String> categoryNames = new LinkedList<>();
    categoryNames.add(ChampionshipCategory.INSTANCE.getTitle());

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      categoryNames.add(category.getTitle());
    }
    for (final VirtualSubjectiveScoreCategory category : description.getVirtualSubjectiveCategories()) {
      categoryNames.add(category.getTitle());
    }
    for (final NonNumericCategory category : CategoriesIgnored.getNonNumericCategories(description, connection,
                                                                                       tournament)) {
      if (scheduledCategoryNames.contains(category.getTitle())) {
        categoryNames.add(category.getTitle());
      }
    }
    categoryNames.add(PerformanceScoreCategory.CATEGORY_TITLE);

    for (final NonNumericCategory category : CategoriesIgnored.getNonNumericCategories(description, connection,
                                                                                       tournament)) {
      if (!scheduledCategoryNames.contains(category.getTitle())) {
        categoryNames.add(category.getTitle());
      }
    }

    return categoryNames;
  }

  /**
   * @param method the name of the calling method to use for logging
   * @param request the request
   * @param response the response to possibly send back an error
   * @return the parsed information, empty on an error
   * @throws IOException if there is an error sending an error response to the
   *           {code}response{code}
   */
  private static Optional<String> getAwardGroup(final String method,
                                                final HttpServletRequest request,
                                                final HttpServletResponse response)
      throws IOException {
    final @Nullable String pathInfo = request.getPathInfo();
    if (null == pathInfo) {
      LOGGER.error("{}: got null path info", method);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Got null path info");
      return Optional.empty();
    }

    LOGGER.debug("{}: pathInfo: {}", method, pathInfo);
    final String[] pathPieces = pathInfo.split("/");
    if (pathPieces.length != 2) {
      final String msg = String.format("wrong number of pieces in path info '%s'. Expecting 2, but found %d", pathInfo,
                                       pathPieces.length);
      LOGGER.error("{}: {}", method, msg);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      return Optional.empty();
    }

    // pathPieces[0] is always empty because the path starts with a slash
    final String awardGroup = pathPieces[1];
    return Optional.of(awardGroup);
  }

}
