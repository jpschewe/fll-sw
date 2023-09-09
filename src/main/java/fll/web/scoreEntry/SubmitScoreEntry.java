/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.playoff.HttpTeamScore;
import fll.web.playoff.TeamScore;
import fll.xml.ChallengeDescription;

/**
 * Submit performance scores.
 */
@WebServlet("/scoreEntry/SubmitScoreEntry")
public class SubmitScoreEntry extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.REF), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final boolean deleteScore = Boolean.valueOf(request.getParameter("delete"));
      if (deleteScore) {
        final String teamNumberStr = request.getParameter("TeamNumber");
        if (null == teamNumberStr) {
          throw new RuntimeException("Missing parameter: TeamNumber");
        }
        final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumberStr).intValue();

        final String runNumber = request.getParameter("RunNumber");
        if (null == runNumber) {
          throw new RuntimeException("Missing parameter: RunNumber");
        }
        final int irunNumber = Utilities.getIntegerNumberFormat().parse(runNumber).intValue();

        Queries.deletePerformanceScore(connection, teamNumber, irunNumber);
      } else if (Boolean.valueOf(request.getParameter("EditFlag"))) {
        final int rowsUpdated = Queries.updatePerformanceScore(challengeDescription, connection, datasource, request);
        if (0 == rowsUpdated) {
          throw new FLLInternalException("No rows updated - did the score get deleted?");
        } else if (rowsUpdated > 1) {
          throw new FLLInternalException("Updated multiple rows!");
        }
      } else {
        final int currentTournament = Queries.getCurrentTournament(connection);
        final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament);

        final String teamNumberStr = request.getParameter("TeamNumber");
        if (null == teamNumberStr) {
          throw new RuntimeException("Missing parameter: TeamNumber");
        }
        final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumberStr).intValue();

        final String runNumberStr = request.getParameter("RunNumber");
        if (null == runNumberStr) {
          throw new RuntimeException("Missing parameter: RunNumber");
        }
        final int runNumber = Utilities.getIntegerNumberFormat().parse(runNumberStr).intValue();

        final String noShow = request.getParameter("NoShow");
        if (null == noShow) {
          throw new RuntimeException("Missing parameter: NoShow");
        }

        final TeamScore teamScore = new HttpTeamScore(teamNumber, runNumber, request);

        if (Queries.performanceScoreExists(connection, currentTournament, teamNumber, runNumber)) {
          final String message = String.format("<div class='error'>Someone else has already entered a score for team %s run %d. Check that you selected the correct team and enter the score again.</div>",
                                               teamNumber, runNumber);
          SessionAttributes.appendToMessage(session, message);
        } else {
          Queries.insertPerformanceScore(connection, datasource, challengeDescription, tournament,
                                         teamScore.isVerified(), teamScore);
          final String message = String.format("<div class='success'>Entered score for %d run %d</div>", teamNumber,
                                               runNumber);
          SessionAttributes.appendToMessage(session, message);
        }
      }

      response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
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
