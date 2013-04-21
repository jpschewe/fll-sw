/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * 
 */
@WebServlet("/report/finalist/StoreFinalistSchedule")
public class StoreFinalistSchedule extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();

    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final int tournament = Queries.getCurrentTournament(connection);

      // get parameters
      final String schedDataStr = request.getParameter("sched_data");
      if (null == schedDataStr
          || "".equals(schedDataStr)) {
        throw new FLLRuntimeException("Parameter 'sched_data' cannot be null");
      }

      final String categoryDataStr = request.getParameter("category_data");
      if (null == categoryDataStr
          || "".equals(categoryDataStr)) {
        throw new FLLRuntimeException("Parameter 'category_data' cannot be null");
      }

      final String division = request.getParameter("division_data");
      if (null == division
          || "".equals(division)) {
        throw new FLLRuntimeException("Parameter 'division_data' cannot be null");
      }

      // decode JSON
      Gson gson = new Gson();
      final Collection<FinalistDBRow> rows = gson.fromJson(schedDataStr, FinalistDBRowDeserialize.INSTANCE.getType());
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Sched Data has "
            + rows.size() + " rows");
      }
      for (final FinalistDBRow row : rows) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("row category: "
              + row.getCategoryName() + " time: " + row.getTime() + " team: " + row.getTeamNumber());
        }
      }

      final Collection<FinalistCategoryRow> categories = gson.fromJson(categoryDataStr,
                                                                       FinalistCategoryRowDeserialize.INSTANCE.getType());
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Category Data has "
            + rows.size() + " rows");
      }
      final Map<String, Boolean> categoryMap = new HashMap<String, Boolean>();
      for (final FinalistCategoryRow cat : categories) {
        categoryMap.put(cat.getCategoryName(), cat.isPublic());
      }

      final FinalistSchedule schedule = new FinalistSchedule(tournament, division, categoryMap, rows);
      schedule.store(connection);

      message.append("<p id='success'>Finalist schedule saved to the database</p>");

    } catch (final SQLException e) {
      message.append("<p class='error'>Error saving finalist schedule into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving subjective data into the database", e);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL("schedule-saved.jsp"));

  }

  private static final class FinalistDBRowDeserialize extends TypeToken<Collection<FinalistDBRow>> {
    public static final FinalistDBRowDeserialize INSTANCE = new FinalistDBRowDeserialize();
  }

  private static final class FinalistCategoryRowDeserialize extends TypeToken<Collection<FinalistCategoryRow>> {
    public static final FinalistCategoryRowDeserialize INSTANCE = new FinalistCategoryRowDeserialize();
  }
}
