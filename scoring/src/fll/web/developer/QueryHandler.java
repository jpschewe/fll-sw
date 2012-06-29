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

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Handle AJAX query from database query page. Expects the parameter "query" to
 * be set and returns a JSON object of type {@link ResultData}.
 */
@WebServlet("/developer/QueryHandler")
public class QueryHandler extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final ResultData result = new ResultData();

    DataSource datasource = SessionAttributes.getDataSource(session);
    Statement stmt = null;
    ResultSet rs = null;
    try {
      Connection connection = datasource.getConnection();
      final String query = request.getParameter("query");
      stmt = connection.createStatement();
      rs = stmt.executeQuery(query);

      ResultSetMetaData meta = rs.getMetaData();
      for (int columnNum = 1; columnNum <= meta.getColumnCount(); ++columnNum) {
        result.columnNames.add(meta.getColumnName(columnNum).toLowerCase());
      }
      while (rs.next()) {
        final Map<String, String> row = new HashMap<String, String>();
        for (final String columnName : result.columnNames) {
          final String value = rs.getString(columnName);
          row.put(columnName, value);
        }
        result.data.add(row);
      }

    } catch (final SQLException e) {
      result.error = e.getMessage();
      LOGGER.error("Exception doing developer query", e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
    final Gson gson = new Gson();
    final String resultJson = gson.toJson(result);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(resultJson);


  }

  public static class ResultData {
    /**
     * If there is an error, this will be non-null.
     */
    public String error = null;
    public final List<String> columnNames = new LinkedList<String>();

    public final List<Map<String, String>> data = new LinkedList<Map<String, String>>();
  }
}
