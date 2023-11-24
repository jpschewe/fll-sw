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
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.api.PostResult;
import fll.web.playoff.MapTeamScore;
import fll.web.playoff.Playoff;
import fll.web.playoff.TeamScore;
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

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final TypeReference<Map<String, String>> formDataTypeRef = new TypeReference<Map<String, String>>() {
    };
    final Map<String, String> formData;
    if (LOGGER.isTraceEnabled()) {
      final StringWriter debugWriter = new StringWriter();
      request.getReader().transferTo(debugWriter);
      LOGGER.trace("Read data: {}", debugWriter.toString());
      formData = jsonMapper.readValue(debugWriter.toString(), formDataTypeRef);
    } else {
      formData = jsonMapper.readValue(request.getReader(), formDataTypeRef);
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

      final int currentTournament = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament);

      final boolean deleteScore = Boolean.valueOf(formData.get("delete"));
      if (deleteScore) {
        Queries.deletePerformanceScore(connection, teamNumber, runNumber);
      } else if (Boolean.valueOf(formData.get("EditFlag"))) {
        final TeamScore teamScore = new MapTeamScore(teamNumber, runNumber, formData);
        final int rowsUpdated = Queries.updatePerformanceScore(challengeDescription, connection, datasource, teamScore);
        if (0 == rowsUpdated) {
          throw new FLLInternalException("No rows updated - did the score get deleted?");
        } else if (rowsUpdated > 1) {
          throw new FLLInternalException("Updated multiple rows!");
        }
      } else {
        final String noShow = formData.get("NoShow");
        if (null == noShow) {
          throw new RuntimeException("Missing parameter: NoShow");
        }

        final TeamScore teamScore = new MapTeamScore(teamNumber, runNumber, formData);

        if (Queries.performanceScoreExists(connection, currentTournament, teamNumber, runNumber)) {
          final String message = String.format("<div class='error'>Someone else has already entered a score for team %s run %d. Check that you selected the correct team and enter the score again.</div>",
                                               teamNumber, runNumber);
          final PostResult result = new PostResult(false, Optional.of(message));
          jsonMapper.writeValue(writer, result);
        } else {
          final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection,
                                                                                      tournament.getTournamentID());
          final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection,
                                                                                tournament.getTournamentID());
          final boolean regularMatchPlay = runNumber <= numSeedingRounds;

          final String roundText;
          if (runningHeadToHead
              && !regularMatchPlay) {
            final String division = Playoff.getPlayoffDivision(connection, tournament.getTournamentID(), teamNumber,
                                                               runNumber);
            final int playoffRun = Playoff.getPlayoffRound(connection, tournament.getTournamentID(), division,
                                                           runNumber);
            roundText = "playoff round "
                + playoffRun;
          } else {
            roundText = "run Number "
                + runNumber;
          }

          Queries.insertPerformanceScore(connection, datasource, challengeDescription, tournament,
                                         teamScore.isVerified(), teamScore);

          final String message = String.format("<div class='success'>Entered score for team number %d %s</div>",
                                               teamNumber, roundText);
          final PostResult result = new PostResult(true, Optional.of(message));
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
