/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import fll.scores.DatabaseSubjectiveTeamScore;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Check that there isn't any subjective data in the database. If there is, make
 * the user confirm before allowing the database to be imported and scores
 * overwritten.
 */
@WebServlet("/admin/CheckSubjectiveEmptyForJudgeImport")
public class CheckSubjectiveEmptyForJudgeImport extends BaseFLLServlet {

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

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      for (final SubjectiveScoreCategory category : challenge.getSubjectiveCategories()) {
        if (DatabaseSubjectiveTeamScore.categoryHasScores(connection, category, tournamentData.getCurrentTournament())) {
          response.sendRedirect(response.encodeRedirectURL("confirm-import-with-subjective-scores.jsp"));
          return;
        }
      }

      // insert into the import workflow after tournament verification
      final String redirect = String.format("%s/developer/importdb/FindMissingTeams", request.getContextPath());
      response.sendRedirect(response.encodeRedirectURL(redirect));
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

}
