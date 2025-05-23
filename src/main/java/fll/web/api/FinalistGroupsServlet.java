/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
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
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.FinalistGroup;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Map of finalist group names to {@link FinalistGroup}.
 */
@WebServlet("/api/FinalistGroups")
public class FinalistGroupsServlet extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isJudge()
        && !auth.isRef()
        && !auth.isReportGenerator()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Map<String, FinalistGroup> groups = FinalistGroup.loadFinalistGroups(connection, tournament);

      jsonMapper.writeValue(writer, groups);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isJudge()
        && !auth.isRef()
        && !auth.isReportGenerator()) {
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
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Map<String, FinalistGroup> groups = jsonMapper.readValue(reader, FinalistGroupMapTypeInformation.INSTANCE);

      FinalistGroup.storeFinalistGroups(connection, tournament, groups);

      final ApiResult result = new ApiResult(true, Optional.empty());
      jsonMapper.writeValue(writer, result);

    } catch (final SQLException e) {
      final ApiResult result = new ApiResult(false, Optional.ofNullable(e.getMessage()));
      jsonMapper.writeValue(writer, result);

      throw new FLLRuntimeException(e);
    } catch (final JsonProcessingException e) {
      final ApiResult result = new ApiResult(false, Optional.ofNullable(e.getMessage()));
      jsonMapper.writeValue(writer, result);

      throw new FLLRuntimeException(e);
    }

  }

  /**
   * Used for JSON deserialization.
   */
  private static final class FinalistGroupMapTypeInformation extends TypeReference<Map<String, FinalistGroup>> {
    /** single instance. */
    public static final FinalistGroupMapTypeInformation INSTANCE = new FinalistGroupMapTypeInformation();
  }

}
