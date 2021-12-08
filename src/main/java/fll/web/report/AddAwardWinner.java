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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
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

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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
        if (EditAwardWinners.SUBJECTIVE_AWARD_TYPE.equals(awardType)) {
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
                                                         awardGroup, teamNumber);
        } else if (EditAwardWinners.NON_NUMERIC_AWARD_TYPE.equals(awardType)) {
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
                                                           awardGroup, teamNumber);
          } else {
            winner = AwardWinners.getNonNumericOverallAwardWinner(connection, tournament.getTournamentID(),
                                                                  categoryTitle, teamNumber);
          }
        } else if (EditAwardWinners.CHAMPIONSHIP_AWARD_TYPE.equals(awardType)) {
          if (null == awardGroup) {
            throw new FLLInternalException("Award group cannot be null for Championship award");
          }

          winner = AwardWinners.getNonNumericAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                         awardGroup, teamNumber);
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
    if (!auth.requireRoles(request, response, session, Set.of(UserRole.JUDGE, UserRole.HEAD_JUDGE), false)) {
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

      if (EditAwardWinners.SUBJECTIVE_AWARD_TYPE.equals(awardType)) {
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
          LOGGER.info("Updating team {} in category {} place {}", winner.getTeamNumber(), winner.getName(),
                      winner.getPlace());
          AwardWinners.updateSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
          SessionAttributes.appendToMessage(session,
                                            String.format("<div class='success'>Modified team %d in award %s</div>",
                                                          winner.getTeamNumber(), winner.getName()));
        } else {
          if (null != AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                            awardGroup, teamNumber)) {
            LOGGER.warn("Attempting duplicate add of team {} to category {} place {}", winner.getTeamNumber(),
                        winner.getName(), winner.getPlace());
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                            teamNumber, categoryTitle));
          } else {
            LOGGER.info("Added team {} to category {} place {}", winner.getTeamNumber(), winner.getName(),
                        winner.getPlace());
            AwardWinners.addSubjectiveAwardWinner(connection, tournament.getTournamentID(), winner);
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='success'>Added team %d to award %s</div>",
                                                            winner.getTeamNumber(), winner.getName()));
          }
        }
      } else if (EditAwardWinners.NON_NUMERIC_AWARD_TYPE.equals(awardType)) {
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
            LOGGER.info("Updating team {} in category {} place {}", winner.getTeamNumber(), winner.getName(),
                        winner.getPlace());
            AwardWinners.updateNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='success'>Modified team %d in award %s</div>",
                                                            winner.getTeamNumber(), winner.getName()));
          } else {
            if (null != AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                              awardGroup, teamNumber)) {
              LOGGER.warn("Attempting duplicate add of team {} to category {} place {}", winner.getTeamNumber(),
                          winner.getName(), winner.getPlace());
              SessionAttributes.appendToMessage(session,
                                                String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                              teamNumber, categoryTitle));
            } else {
              LOGGER.info("Added team {} to category {} place {}", winner.getTeamNumber(), winner.getName(),
                          winner.getPlace());
              AwardWinners.addNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
              SessionAttributes.appendToMessage(session,
                                                String.format("<div class='success'>Added team %d to award %s</div>",
                                                              winner.getTeamNumber(), winner.getName()));
            }
          }
        } else {
          final OverallAwardWinner winner = new OverallAwardWinner(categoryTitle, teamNumber, description, place);
          if (edit) {
            LOGGER.info("Updating team {} in category {} place {}", winner.getTeamNumber(), winner.getName(),
                        winner.getPlace());
            AwardWinners.updateNonNumericOverallAwardWinner(connection, tournament.getTournamentID(), winner);
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='success'>Modified team %d in award %s</div>",
                                                            winner.getTeamNumber(), winner.getName()));
          } else {
            if (null != AwardWinners.getNonNumericOverallAwardWinner(connection, tournament.getTournamentID(),
                                                                     categoryTitle, teamNumber)) {
              LOGGER.warn("Attempting duplicate add of team {} to category {} place {}", winner.getTeamNumber(),
                          winner.getName(), winner.getPlace());
              SessionAttributes.appendToMessage(session,
                                                String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                              teamNumber, categoryTitle));
            } else {
              LOGGER.info("Added team {} to category {} place {}", winner.getTeamNumber(), winner.getName(),
                          winner.getPlace());
              AwardWinners.addNonNumericOverallAwardWinner(connection, tournament.getTournamentID(), winner);
              SessionAttributes.appendToMessage(session,
                                                String.format("<div class='success'>Added team %d to award %s</div>",
                                                              winner.getTeamNumber(), winner.getName()));
            }
          }
        }

      } else if (EditAwardWinners.CHAMPIONSHIP_AWARD_TYPE.equals(awardType)) {
        if (null == awardGroup) {
          throw new FLLInternalException("Award group cannot be null for Championship award");
        }

        final AwardWinner winner = new AwardWinner(categoryTitle, awardGroup, teamNumber, description, place);
        if (edit) {
          LOGGER.info("Updating team {} in category {} place {}", winner.getTeamNumber(), winner.getName(),
                      winner.getPlace());
          AwardWinners.updateNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
          SessionAttributes.appendToMessage(session,
                                            String.format("<div class='success'>Modified team %d in award %s</div>",
                                                          winner.getTeamNumber(), winner.getName()));
        } else {
          if (null != AwardWinners.getSubjectiveAwardWinner(connection, tournament.getTournamentID(), categoryTitle,
                                                            awardGroup, teamNumber)) {
            LOGGER.warn("Attempting duplicate add of team {} to category {} place {}", winner.getTeamNumber(),
                        winner.getName(), winner.getPlace());
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='error'>Team %d is already receiving award %s, cannot add a second time.</div>",
                                                            teamNumber, categoryTitle));
          } else {
            LOGGER.info("Added team {} to category {} place {}", winner.getTeamNumber(), winner.getName(),
                        winner.getPlace());
            AwardWinners.addNonNumericAwardWinner(connection, tournament.getTournamentID(), winner);
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='success'>Added team %d to award %s</div>",
                                                            winner.getTeamNumber(), winner.getName()));
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
