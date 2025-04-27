/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api.awardsScript;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.api.ApiResult;
import fll.web.api.awardsScript.AwardWinnerApiUtils.PutData;
import fll.web.api.awardsScript.AwardWinnerApiUtils.PutPathInfo;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Handles both regular and virtual subjective categories.
 * 
 * @see AwardWinners#getSubjectiveAwardWinners(Connection, int)
 * @see AwardWinners#getVirtualSubjectiveAwardWinners(Connection, int)
 */
@WebServlet("/api/AwardsScript/SubjectiveAwardWinners/*")
public class SubjectiveAwardWinners extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final List<AwardWinner> winners = AwardWinners.getSubjectiveAwardWinners(connection,
                                                                               tournament.getTournamentID());
      final List<AwardWinner> virtualWinners = AwardWinners.getVirtualSubjectiveAwardWinners(connection,
                                                                                             tournament.getTournamentID());
      final List<AwardWinner> allWinners = Stream.concat(winners.stream(), virtualWinners.stream()).toList();
      jsonMapper.writeValue(writer, allWinners);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

  /**
   * Add a new or modify an existing winner. This technically should be PATCH,
   * but Apache Tomcat doesn't support PATCH at this time, so PUT is being used
   * instead.
   * Expecting url "{category.name}/{winnerTeamNumber}" and payload
   * of {@link PutData}.
   */
  @Override
  protected final void doPut(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isReportGenerator()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Must be ReportGenereator");
      return;
    }

    final Optional<AwardWinnerApiUtils.PutPathInfo> putPathInfo = AwardWinnerApiUtils.parsePutPathInfo(LOGGER, "doPut",
                                                                                                       request,
                                                                                                       response);
    if (putPathInfo.isEmpty()) {
      return;
    }

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    final PutData data = jsonMapper.readValue(request.getReader(), PutData.class);

    final String awardGroup = data.awardGroup;
    if (null == awardGroup) {
      LOGGER.error("doPut: awardGroup cannot be null");
      response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Award group cannot be null");
      return;
    }

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final boolean virtualCategory = description.getVirtualSubjectiveCategoryByTitle(putPathInfo.get()
                                                                                                 .getCategoryName()) != null;

      final @Nullable AwardWinner existingWinner;
      if (virtualCategory) {
        existingWinner = AwardWinners.getVirtualSubjectiveAwardWinner(connection, tournament.getTournamentID(),
                                                                      putPathInfo.get().getCategoryName(),
                                                                      putPathInfo.get().getTeamNumber());
      } else {
        existingWinner = AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(),
                                                               putPathInfo.get().getCategoryName(),
                                                               putPathInfo.get().getTeamNumber());
      }

      if (null == existingWinner) {
        final AwardWinner winner = new AwardWinner(putPathInfo.get().getCategoryName(), awardGroup,
                                                   putPathInfo.get().getTeamNumber(), data.description, data.place);
        if (virtualCategory) {
          AwardWinners.addVirtualSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
        } else {
          AwardWinners.addSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
        }
      } else {
        final AwardWinner winner = new AwardWinner(putPathInfo.get().getCategoryName(), awardGroup,
                                                   putPathInfo.get().getTeamNumber(),
                                                   data.descriptionSpecified ? data.description
                                                       : existingWinner.getDescription(),
                                                   data.place);
        if (virtualCategory) {
          AwardWinners.updateVirtualSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
        } else {
          AwardWinners.updateSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
        }
      }

      jsonMapper.writeValue(response.getWriter(), new ApiResult(true, Optional.empty()));
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  /**
   * Delete a winner.
   * Expecting url "{category.name}/{winnerTeamNumber}"
   */
  @Override
  protected final void doDelete(final HttpServletRequest request,
                                final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isReportGenerator()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final Optional<PutPathInfo> putPathInfo = AwardWinnerApiUtils.parsePutPathInfo(LOGGER, "doPut", request, response);
    if (putPathInfo.isEmpty()) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      AwardWinners.deleteSubjectiveAwardWinner(connection, tournament.getTournamentID(),
                                               putPathInfo.get().getCategoryName(), putPathInfo.get().getTeamNumber());

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      jsonMapper.writeValue(response.getWriter(), new ApiResult(true, Optional.empty()));
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

}
