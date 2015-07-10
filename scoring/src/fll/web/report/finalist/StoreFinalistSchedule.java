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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.db.NonNumericNominees;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Store the data from the finalist scheduling.
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

      final String nomineesStr = request.getParameter("non-numeric-nominees_data");
      if (null == nomineesStr
          || "".equals(nomineesStr)) {
        throw new FLLRuntimeException("Parameter 'non-numeric-nominees_data' cannot be null");
      }

      // decode JSON
      final ObjectMapper jsonMapper = new ObjectMapper();

      final Collection<FinalistDBRow> rows = jsonMapper.readValue(schedDataStr,
                                                                  FinalistScheduleTypeInformation.INSTANCE);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Sched Data has "
            + rows.size() + " rows");
        for (final FinalistDBRow row : rows) {
          LOGGER.trace("row category: "
              + row.getCategoryName() + " time: " + row.getTime() + " team: " + row.getTeamNumber());
        }
      }

      final Collection<FinalistCategory> categories = jsonMapper.readValue(categoryDataStr,
                                                                           FinalistCategoriesTypeInformation.INSTANCE);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Category Data has "
            + rows.size() + " rows");
      }

      final FinalistSchedule schedule = new FinalistSchedule(tournament, division, categories, rows);
      schedule.store(connection);

      final Collection<NonNumericNominees> nominees = jsonMapper.readValue(nomineesStr,
                                                                           NonNumericNomineesTypeInformation.INSTANCE);
      for (final NonNumericNominees nominee : nominees) {
        nominee.store(connection, tournament);
      }

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

  private static final class FinalistScheduleTypeInformation extends TypeReference<Collection<FinalistDBRow>> {
    public static final FinalistScheduleTypeInformation INSTANCE = new FinalistScheduleTypeInformation();
  }

  private static final class FinalistCategoriesTypeInformation extends TypeReference<Collection<FinalistCategory>> {
    public static final FinalistCategoriesTypeInformation INSTANCE = new FinalistCategoriesTypeInformation();
  }

  private static final class NonNumericNomineesTypeInformation extends TypeReference<Collection<NonNumericNominees>> {
    public static final NonNumericNomineesTypeInformation INSTANCE = new NonNumericNomineesTypeInformation();
  }

}
