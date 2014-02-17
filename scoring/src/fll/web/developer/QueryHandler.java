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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Handle AJAX query from database query page. Expects the parameter "query" to
 * be set and returns a JSON object of type {@link ResultData}.
 */
@WebServlet("/developer/QueryHandler")
public class QueryHandler extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Parameter that the query is expected to be in.
   */
  public static final String QUERY_PARAMETER = "query";

  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Executing query from user")
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final List<String> columnNames = new LinkedList<String>();
    final List<Map<String, String>> data = new LinkedList<Map<String, String>>();
    String error = null;

    DataSource datasource = ApplicationAttributes.getDataSource(application);
    Statement stmt = null;
    ResultSet rs = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final String query = request.getParameter(QUERY_PARAMETER);
      stmt = connection.createStatement();
      rs = stmt.executeQuery(query);

      ResultSetMetaData meta = rs.getMetaData();
      for (int columnNum = 1; columnNum <= meta.getColumnCount(); ++columnNum) {
        columnNames.add(meta.getColumnName(columnNum).toLowerCase());
      }
      while (rs.next()) {
        final Map<String, String> row = new HashMap<String, String>();
        for (final String columnName : columnNames) {
          final String value = rs.getString(columnName);
          row.put(columnName, value);
        }
        data.add(row);
      }

    } catch (final SQLException e) {
      error = e.getMessage();
      LOGGER.error("Exception doing developer query", e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);
    }

    response.setContentType("application/json");
    response.setCharacterEncoding(Utilities.DEFAULT_CHARSET.name());

    final ResultData result = new ResultData(columnNames, data, error);
    final ObjectMapper jsonMapper = new ObjectMapper();
    final Writer writer = response.getWriter();

    jsonMapper.writeValue(writer, result);
  }

  /**
   * Object that comes back out of the servlet {@link QueryHandler}.
   */
  public static class ResultData {
    public ResultData(@JsonProperty("columnNames") final List<String> columnNames,
                      @JsonProperty("data") final List<Map<String, String>> data,
                      @JsonProperty("error") final String error) {
      this.columnNames.addAll(columnNames);
      this.data.addAll(data);
      this.error = error;
    }

    /**
     * If there is an error, this will be non-null.
     */
    private final String error;

    public String getError() {
      return error;
    }

    private final List<String> columnNames = new LinkedList<String>();

    public List<String> getColumnNames() {
      return columnNames;
    }

    private final List<Map<String, String>> data = new LinkedList<Map<String, String>>();

    public List<Map<String, String>> getData() {
      return data;
    }

  }

}
