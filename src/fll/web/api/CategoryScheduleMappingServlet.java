/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.db.CategoryColumnMapping;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

/**
 * Get returns a Collection of @link{CategoryColumnMapping} objects.
 */
@WebServlet("/api/CategoryScheduleMapping/*")
public class CategoryScheduleMappingServlet extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final ObjectMapper jsonMapper = new ObjectMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final int currentTournament = Queries.getCurrentTournament(connection);

      final Collection<CategoryColumnMapping> mappings = CategoryColumnMapping.load(connection, currentTournament);

      jsonMapper.writeValue(writer, mappings);      
    } catch (final SQLException e) {
      LOGGER.fatal("Database Exception", e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
