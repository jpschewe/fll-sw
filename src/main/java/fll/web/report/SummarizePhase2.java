/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fll.ScoreStandardization;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.ChallengeDescription;

/**
 * Final steps of score summarization.
 */
@WebServlet("/report/SummarizePhase2")
public class SummarizePhase2 extends BaseFLLServlet {

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int currentTournament = Queries.getCurrentTournament(connection);

      ScoreStandardization.updateTeamTotalScores(connection, description, currentTournament);

      final String errorMsg = ScoreStandardization.checkDataConsistency(connection);
      if (null == errorMsg) {
        session.setAttribute(SessionAttributes.MESSAGE, "<p id='success'><i>Successfully summarized scores</i></p>");

        final String redirect = SessionAttributes.getAttribute(session, PromptSummarizeScores.SUMMARY_REDIRECT_KEY,
                                                               String.class);
        if (null == redirect) {
          response.sendRedirect(response.encodeRedirectURL("index.jsp"));
        } else {
          response.sendRedirect(response.encodeRedirectURL(redirect));
        }
      } else {
        session.setAttribute(SessionAttributes.MESSAGE, "<font class='error'>"
            + errorMsg
            + "</font>");
        response.sendRedirect(response.encodeRedirectURL("index.jsp"));
      }
    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }
  }

}
