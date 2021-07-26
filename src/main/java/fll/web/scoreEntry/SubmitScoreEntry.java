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

import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
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
        final int rowsUpdated = Queries.updatePerformanceScore(challengeDescription, connection, request);
        if (0 == rowsUpdated) {
          throw new FLLInternalException("No rows updated - did the score get deleted?");
        } else if (rowsUpdated > 1) {
          throw new FLLInternalException("Updated multiple rows!");
        }
      } else {
        Queries.insertPerformanceScore(challengeDescription, connection, request);
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
