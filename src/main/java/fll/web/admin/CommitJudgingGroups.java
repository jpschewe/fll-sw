/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.TournamentTeam;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Commit the changes from edit_judging_groups.jsp.
 */
@WebServlet("/admin/CommitJudgingGroups")
public class CommitJudgingGroups extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Put the needed variables into the page context for the page to use.
   * 
   * @param application used for application variables
   * @param session used for session variables
   * @param pageContext used to set variables for the page
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final StringBuilder message = new StringBuilder();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int currentTournamentID = Queries.getCurrentTournament(connection);

      pageContext.setAttribute("judgingGroups", Queries.getJudgingStations(connection, currentTournamentID));

      final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, currentTournamentID);
      pageContext.setAttribute("teams", teams.values());

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());

  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final StringBuilder message = new StringBuilder();

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int currentTournamentID = Queries.getCurrentTournament(connection);

      if ("Commit".equals(request.getParameter("submit_data"))) {

        final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, currentTournamentID);
        for (final Map.Entry<Integer, TournamentTeam> entry : teams.entrySet()) {
          final int teamNumber = entry.getValue().getTeamNumber();
          final String teamNumberStr = String.valueOf(teamNumber);
          String newJudgingGroup = request.getParameter(teamNumberStr);
          if (null != newJudgingGroup
              && !newJudgingGroup.isEmpty()) {
            if ("text".equals(newJudgingGroup)) {
              // get from text box
              newJudgingGroup = request.getParameter(teamNumberStr
                  + "_text");
            }
            if (null != newJudgingGroup
                && !newJudgingGroup.trim().isEmpty()) {
              newJudgingGroup = newJudgingGroup.trim();

              final String currentJudgingGroup = Queries.getJudgingGroup(connection, teamNumber, currentTournamentID);
              if (!newJudgingGroup.equals(currentJudgingGroup)) {
                // clear out scores for this team first
                for (final SubjectiveScoreCategory category : challengeDescription.getSubjectiveCategories()) {
                  Queries.deleteSubjectiveScores(connection, category.getName(), teamNumber, currentTournamentID);
                }

                Queries.updateTeamJudgingGroups(connection, teamNumber, currentTournamentID, newJudgingGroup);
              }
            }
          }
        }
      }

      message.append("<p class='success' id='success'>Judging group changes saved</p>");

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving judging group data into the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());

    // send back to the admin index
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }

}
