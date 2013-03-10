/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

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
      
      // Convert JSON
      final String schedDataStr = request.getParameter("sched_data");
      if (null == schedDataStr
          || "".equals(schedDataStr)) {
        throw new FLLRuntimeException("Parameter 'sched_data' cannot be null");
      }
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("JSON: "
            + schedDataStr);
      }

      // decode JSON
      Gson gson = new Gson();
      final Type collectionType = new TypeToken<Collection<FinalistDBRow>>() {
      }.getType();
      final Collection<FinalistDBRow> rows = gson.fromJson(schedDataStr, collectionType);
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
      
      final FinalistSchedule schedule = new FinalistSchedule(tournament, rows);
      schedule.store(connection);
      
    } catch (final SQLException e) {
      message.append("<p class='error'>Error saving finalist schedule into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving subjective data into the database", e);
    }

    session.setAttribute("message", message.toString());
    // FIXME response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }

}
