/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Support for add-award-winner.jsp.
 */
@WebServlet("/report/AddAwardWinner")
public class AddAwardWinnner extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.requireRoles(request, response, session, Set.of(UserRole.JUDGE), false)) {
      return;
    }

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final String categoryTitle = WebUtils.getNonNullRequestParameter(request, "categoryTitle");
    final @Nullable String awardGroup = request.getParameter("awardGroup");
    final String awardType = WebUtils.getNonNullRequestParameter(request, "awardType");
    final @Nullable String description = request.getParameter("description");
    final int place = WebUtils.getIntRequestParameter(request, "place");
    final int teamNumber = WebUtils.getIntRequestParameter(request, "teamNumber");

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);

      if ("subjective".equals(awardType)) {
        if (null == awardGroup) {
          throw new FLLInternalException("Award group cannot be null for subjective awards");
        }

        final @Nullable SubjectiveScoreCategory category = challengeDescription.getSubjectiveCategoryByTitle(categoryTitle);
        if (null == category) {
          throw new FLLInternalException("Cannot find subjective category with title '"
              + categoryTitle
              + "'");
        }

        final AwardWinner winner = new AwardWinner(categoryTitle, awardGroup, teamNumber, description, place);
        AwardWinners.addChallengeAwardWinner(connection, tournament.getTournamentID(), winner);

        response.sendRedirect("edit-award-winners.jsp");
      } else {
        throw new FLLInternalException("Unknown award type: '"
            + awardType
            + "'");
      }
    } catch (final SQLException e) {
      throw new FLLInternalException("Error storing award winners", e);
    }
  }

}
