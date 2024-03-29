/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Handle AJAX query from database query page. Expects the parameter "query" to
 * be set and returns a JSON object of type {@link ResultData}.
 */
@WebServlet("/developer/QueryHandler")
public class QueryHandler extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Parameter that the query is expected to be in.
   */
  public static final String QUERY_PARAMETER = "query";

  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Executing query from user")
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

    final List<String> columnNames = new LinkedList<String>();
    final List<Map<String, @Nullable String>> data = new LinkedList<>();
    String error = null;

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection(); Statement stmt = connection.createStatement()) {
      final String query = WebUtils.getNonNullRequestParameter(request, QUERY_PARAMETER);
      LOGGER.debug("Executing query '{}'", query);

      try (ResultSet rs = stmt.executeQuery(query)) {

        ResultSetMetaData meta = rs.getMetaData();
        for (int columnNum = 1; columnNum <= meta.getColumnCount(); ++columnNum) {
          columnNames.add(meta.getColumnName(columnNum).toLowerCase());
        }
        while (rs.next()) {
          final Map<String, @Nullable String> row = new HashMap<>();
          for (final String columnName : columnNames) {
            final String value = rs.getString(columnName);
            row.put(columnName, value);
          }
          data.add(row);
        }
      } // ResultSet
    } catch (final SQLException e) {
      error = e.getMessage();
      LOGGER.error("Exception doing developer query", e);
    }

    response.setContentType("application/json");
    response.setCharacterEncoding(Utilities.DEFAULT_CHARSET.name());

    final ResultData result = new ResultData(columnNames, data, error);
    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    final Writer writer = response.getWriter();

    jsonMapper.writeValue(writer, result);
  }

  /**
   * Object that comes back out of the servlet {@link QueryHandler}.
   */
  public static class ResultData {
    /***
     * @param columnNames {@link #getColumnNames()}
     * @param data {@link #getData()}
     * @param error {@link #getError()}
     */
    @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
    public ResultData(@JsonProperty("columnNames") final List<String> columnNames,
                      @JsonProperty("data") final List<Map<String, @Nullable String>> data,
                      @JsonProperty("error") final @Nullable String error) {
      this.columnNames.addAll(columnNames);
      this.data.addAll(data);
      this.error = error;
    }

    private final @Nullable String error;

    /**
     * @return If there is an error, this will be non-null.
     */
    public @Nullable String getError() {
      return error;
    }

    private final List<String> columnNames = new LinkedList<String>();

    /**
     * @return the column names
     */
    public List<String> getColumnNames() {
      return columnNames;
    }

    private final List<Map<String, @Nullable String>> data = new LinkedList<>();

    /**
     * @return the parsed data list of pairs of column and value
     */
    public List<Map<String, @Nullable String>> getData() {
      return data;
    }

  }

}
