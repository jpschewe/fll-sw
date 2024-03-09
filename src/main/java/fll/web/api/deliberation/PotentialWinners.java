/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api.deliberation;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.deliberations.PotentialWinner;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.api.ApiResult;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Deliberation potential winners.
 */
@WebServlet("/api/deliberation/PotentialWinners/*")
public class PotentialWinners extends HttpServlet {

  /**
   * Used for JSON deserialization.
   */
  private static final class TypeInformation extends TypeReference<Collection<PotentialWinner>> {
    /** single instance. */
    public static final TypeInformation INSTANCE = new TypeInformation();
  }

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isReportGenerator()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Must be ReportGenereator");
      return;
    }

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final Collection<PotentialWinner> v = PotentialWinner.getPotentialWinners(connection, tournament);
      jsonMapper.writeValue(writer, v);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
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

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Collection<PotentialWinner> v = jsonMapper.readValue(request.getReader(), TypeInformation.INSTANCE);
      PotentialWinner.setPotentialWinners(connection, tournament, v);

      jsonMapper.writeValue(writer, new ApiResult(true, Optional.empty()));
    } catch (final SQLException e) {
      jsonMapper.writeValue(writer, new ApiResult(false, Optional.ofNullable(e.getMessage())));
      throw new FLLRuntimeException(e);
    } catch (final JsonProcessingException e) {
      jsonMapper.writeValue(writer, new ApiResult(false, Optional.ofNullable(e.getMessage())));
      throw new FLLRuntimeException(e);
    }
  }

}
