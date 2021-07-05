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
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.db.OverallAwardWinner;
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
 * Support for add-award-winner.jsp.
 */
@WebServlet("/report/AddAwardWinner")
public class AddAwardWinner extends BaseFLLServlet {

  /**
   * Setup variables for add-award-winner.jsp.
   * 
   * @param request web requst
   * @param response response to redirect on error
   * @param application application variables
   * @param session session variables
   * @param page page variables
   * @throws SQLException on a database error
   */
  public static void populateContext(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final ServletContext application,
                                     final HttpSession session,
                                     final PageContext page)
      throws SQLException {

    final boolean edit = Boolean.valueOf(request.getParameter("edit"));
    if (edit) {
      final String categoryTitle = WebUtils.getNonNullRequestParameter(request, "categoryTitle");
      final @Nullable String awardGroup = WebUtils.getParameterOrNull(request, "awardGroup");
      final String awardType = WebUtils.getNonNullRequestParameter(request, "awardType");
      final int teamNumber = WebUtils.getIntRequestParameter(request, "teamNumber");

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {

        final Tournament tournament = Tournament.getCurrentTournament(connection);

        final @Nullable OverallAwardWinner winner;
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

          winner = AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                         teamNumber);
        } else if ("non-numeric".equals(awardType)) {
          final @Nullable NonNumericCategory category = challengeDescription.getNonNumericCategoryByTitle(categoryTitle);
          if (null == category) {
            throw new FLLInternalException("Cannot find non-numeric category with title '"
                + categoryTitle
                + "'");
          }

          if (category.getPerAwardGroup()) {
            if (null == awardGroup) {
              throw new FLLInternalException("Award group cannot be null for non-numeric award that is per award group");
            }
            winner = AwardWinners.getNonNumericAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                           teamNumber);
          } else {
            winner = AwardWinners.getNonNumericOverallAwardWinner(connection, tournament.getTournamentID(),
                                                                  categoryTitle, teamNumber);
          }
        } else if ("championship".equals(awardType)) {
          if (null == awardGroup) {
            throw new FLLInternalException("Award group cannot be null for Championship award");
          }

          winner = AwardWinners.getNonNumericAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                         teamNumber);
        } else {
          throw new FLLInternalException("Unknown award type: '"
              + awardType
              + "'");
        }

        if (null == winner) {
          SessionAttributes.appendToMessage(session,
                                            String.format("<div class='error'>Team number %d is not currently listed for %s, cannot edit.</div>",
                                                          teamNumber, categoryTitle));
          response.sendRedirect("edit-award-winners.jsp");
        } else {
          page.setAttribute("winner", winner);
        }
      } catch (final SQLException e) {
        throw new FLLInternalException("Error finding award winner", e);
      } catch (final IOException e) {
        throw new FLLInternalException("Error redirecting the response", e);
      }
    }

  }

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
    final @Nullable String awardGroup = WebUtils.getParameterOrNull(request, "awardGroup");
    final String awardType = WebUtils.getNonNullRequestParameter(request, "awardType");
    final @Nullable String description = request.getParameter("description");
    final int place = WebUtils.getIntRequestParameter(request, "place");
    final int teamNumber = WebUtils.getIntRequestParameter(request, "teamNumber");
    final boolean edit = Boolean.valueOf(request.getParameter("edit"));

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
        if (edit) {
          AwardWinners.updateSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
        } else {
          if (null != AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                            teamNumber)) {
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                            teamNumber, categoryTitle));
          } else {
            AwardWinners.addSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
          }
        }
      } else if ("non-numeric".equals(awardType)) {
        final @Nullable NonNumericCategory category = challengeDescription.getNonNumericCategoryByTitle(categoryTitle);
        if (null == category) {
          throw new FLLInternalException("Cannot find non-numeric category with title '"
              + categoryTitle
              + "'");
        }

        if (category.getPerAwardGroup()) {
          if (null == awardGroup) {
            throw new FLLInternalException("Award group cannot be null for non-numeric award that is per award group");
          }
          final AwardWinner winner = new AwardWinner(categoryTitle, awardGroup, teamNumber, description, place);
          if (edit) {
            AwardWinners.updateNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
          } else {
            if (null != AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                              teamNumber)) {
              SessionAttributes.appendToMessage(session,
                                                String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                              teamNumber, categoryTitle));
            } else {
              AwardWinners.addNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
            }
          }
        } else {
          final OverallAwardWinner winner = new OverallAwardWinner(categoryTitle, teamNumber, description, place);
          if (edit) {
            AwardWinners.updateNonNumericOverallAwardWinner(connection, tournament.getTournamentID(), winner);
          } else {
            if (null != AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                              teamNumber)) {
              SessionAttributes.appendToMessage(session,
                                                String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                              teamNumber, categoryTitle));
            } else {
              AwardWinners.addNonNumerciOverallAwardWinner(connection, tournament.getTournamentID(), winner);
            }
          }
        }

      } else if ("championship".equals(awardType)) {
        if (null == awardGroup) {
          throw new FLLInternalException("Award group cannot be null for Championship award");
        }

        final AwardWinner winner = new AwardWinner(categoryTitle, awardGroup, teamNumber, description, place);
        if (edit) {
          AwardWinners.updateNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
        } else {
          if (null != AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                            teamNumber)) {
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                            teamNumber, categoryTitle));
          } else {
            AwardWinners.addNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
          }
        }
      } else {
        throw new FLLInternalException("Unknown award type: '"
            + awardType
            + "'");
      }

      response.sendRedirect("edit-award-winners.jsp");
    } catch (final SQLException e) {
      throw new FLLInternalException("Error adding/editing award winner", e);
    }
  }

}
