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
import fll.db.AwardWinners;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * Delete a team from an award.
 */
@WebServlet("/report/DeleteAwardWinner")
public class DeleteAwardWinnner extends BaseFLLServlet {

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
    final String awardType = WebUtils.getNonNullRequestParameter(request, "awardType");
    final int teamNumber = WebUtils.getIntRequestParameter(request, "teamNumber");

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);

      if ("subjective".equals(awardType)) {
        final @Nullable SubjectiveScoreCategory category = challengeDescription.getSubjectiveCategoryByTitle(categoryTitle);
        if (null == category) {
          throw new FLLInternalException("Cannot find subjective category with title '"
              + categoryTitle
              + "'");
        }

        AwardWinners.deleteSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle, teamNumber);
      } else if ("non-numeric".equals(awardType)) {
        final @Nullable NonNumericCategory category = challengeDescription.getNonNumericCategoryByTitle(categoryTitle);
        if (null == category) {
          throw new FLLInternalException("Cannot find non-numeric category with title '"
              + categoryTitle
              + "'");
        }

        if (category.getPerAwardGroup()) {
          AwardWinners.deleteNonNumericAwardWinner(connection, tournament.getTournamentID(), categoryTitle, teamNumber);
        } else {
          AwardWinners.deleteNonNumericOverallAwardWinner(connection, tournament.getTournamentID(), categoryTitle, teamNumber);
        }

      } else if ("championship".equals(awardType)) {
        AwardWinners.deleteNonNumericAwardWinner(connection, tournament.getTournamentID(), categoryTitle, teamNumber);
      } else {
        throw new FLLInternalException("Unknown award type: '"
            + awardType
            + "'");
      }

      response.sendRedirect("edit-award-winners.jsp");
    } catch (final SQLException e) {
      throw new FLLInternalException("Error deleting award winner", e);
    }
  }

}
