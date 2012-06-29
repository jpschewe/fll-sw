/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;
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

import com.google.gson.Gson;

import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Handle AJAX query from database query page. Expects the parameter "query" to
 * be set and returns a JSON object with the results.
 * 
 * <pre>
 * { 
 *   columnsNames: ["one", "two", ...], 
 *   data: [ {"one" => row0_0, "two" => row0_1, ...}, 
 *           {"one" => row_1_0, "two => row1_1, ...} ...]
 * }
 * </pre>
 */
@WebServlet("/developer/QueryHandler")
public class QueryHandler extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    DataSource datasource = SessionAttributes.getDataSource(session);
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Connection connection = datasource.getConnection();
      final String query = request.getParameter("query");
      stmt = connection.createStatement();
      rs = stmt.executeQuery(query);

      final ResultData result = new ResultData();
      ResultSetMetaData meta = rs.getMetaData();
      for (int columnNum = 1; columnNum <= meta.getColumnCount(); ++columnNum) {
        result.columnNames.add(meta.getColumnName(columnNum));
      }
      while (rs.next()) {
        final Map<String, String> row = new HashMap<String, String>();
        for (final String columnName : result.columnNames) {
          final String value = rs.getString(columnName);
          row.put(columnName, value);
        }
        result.data.add(row);
      }

      final Gson gson = new Gson();
      final String resultJson = gson.toJson(result);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(resultJson);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  private static class ResultData {
    public final List<String> columnNames = new LinkedList<String>();

    public final List<Map<String, String>> data = new LinkedList<Map<String, String>>();
  }
}
