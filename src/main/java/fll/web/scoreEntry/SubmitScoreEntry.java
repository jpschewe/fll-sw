/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
import fll.scores.MapTeamScore;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.api.ApiResult;
import fll.web.playoff.PerformanceTeamScore;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Submit performance scores.
 * POST: receive JSON dictionary of the form parameters
 * GET: unsupported
 */
@WebServlet("/scoreEntry/SubmitScoreEntry")
public class SubmitScoreEntry extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final TypeReference<Map<String, String>> FORM_DATA_TYPE_REF = new TypeReference<Map<String, String>>() {
  };

  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final ServletContext application = request.getServletContext();

    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.REF), false)) {
      return;
    }

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final Map<String, String> formData;
    if (LOGGER.isTraceEnabled()) {
      final StringWriter debugWriter = new StringWriter();
      request.getReader().transferTo(debugWriter);
      LOGGER.trace("Read data: {}", debugWriter.toString());
      formData = jsonMapper.readValue(debugWriter.toString(), FORM_DATA_TYPE_REF);
    } else {
      formData = jsonMapper.readValue(request.getReader(), FORM_DATA_TYPE_REF);
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final String teamNumberStr = formData.get("TeamNumber");
      if (null == teamNumberStr) {
        throw new RuntimeException("Missing parameter: TeamNumber");
      }
      final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumberStr).intValue();

      final String runNumberStr = formData.get("RunNumber");
      if (null == runNumberStr) {
        throw new RuntimeException("Missing parameter: RunNumber");
      }
      final int runNumber = Utilities.getIntegerNumberFormat().parse(runNumberStr).intValue();
      final RunMetadataFactory runMetadataFactory = tournamentData.getRunMetadataFactory();
      final RunMetadata runMetadata = runMetadataFactory.getRunMetadata(runNumber);

      final Tournament tournament = tournamentData.getCurrentTournament();

      final boolean deleteScore = Boolean.valueOf(formData.get("delete"));
      if (deleteScore) {
        Queries.deletePerformanceScore(tournamentData, connection, teamNumber, runNumber);

        final String message = String.format("<div class='success'>Deleted score for team %d run %s</div>", teamNumber,
                                             runMetadata.getDisplayName());
        final ApiResult result = new ApiResult(true, Optional.of(message));
        jsonMapper.writeValue(writer, result);
      } else if (Boolean.valueOf(formData.get("EditFlag"))) {
        final PerformanceTeamScore teamScore = new MapTeamScore(teamNumber, runNumber, formData);
        final int rowsUpdated = Queries.updatePerformanceScore(runMetadataFactory,
                                                               challengeDescription, connection, datasource, teamScore);
        if (0 == rowsUpdated) {
          throw new FLLInternalException("No rows updated - did the score get deleted?");
        } else if (rowsUpdated > 1) {
          throw new FLLInternalException("Updated multiple rows!");
        }

        final String message = String.format("<div class='success'>Edited score for team %d run %s</div>", teamNumber,
                                             runMetadata.getDisplayName());
        final ApiResult result = new ApiResult(true, Optional.of(message));
        jsonMapper.writeValue(writer, result);
      } else {
        final String noShow = formData.get("NoShow");
        if (null == noShow) {
          throw new RuntimeException("Missing parameter: NoShow");
        }

        final PerformanceTeamScore teamScore = new MapTeamScore(teamNumber, runNumber, formData);

        if (Queries.performanceScoreExists(connection, tournament.getTournamentID(), teamNumber, runNumber)) {
          final String message = String.format("<div class='error'>Someone else has already entered a score for team %s run %s. Check that you selected the correct team and enter the score again.</div>",
                                               teamNumber, runMetadata.getDisplayName());

          final ApiResult result = new ApiResult(false, Optional.of(message));
          jsonMapper.writeValue(writer, result);
        } else {
          Queries.insertPerformanceScore(tournamentData.getRunMetadataFactory(), connection, datasource,
                                         challengeDescription, tournament, teamScore.isVerified(), teamScore);

          final String message = String.format("<div class='success'>Entered score for team number %d %s</div>",
                                               teamNumber, runMetadata.getDisplayName());
          final ApiResult result = new ApiResult(true, Optional.of(message));
          jsonMapper.writeValue(writer, result);
        }
      }
    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new FLLRuntimeException(e);
    } catch (final ParseException e) {
      LOGGER.error(e, e);
      throw new FLLInternalException("Cannot parse request parameters: "
          + e.getMessage(), e);
    }

  }

}
