/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

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
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;

/**
 * Finish a playoff bracket.
 */
@WebServlet("/playoff/FinishBracket")
public class FinishBracket extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);

      final String bracketName = request.getParameter("bracket");
      if (null == bracketName
          || "".equals(bracketName)) {
        SessionAttributes.appendToMessage(session, "<p class='error'>No playoff bracket specified to finish</p>");
        WebUtils.sendRedirect(application, response, "/playoff/index.jsp");
        return;
      }

      final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);

      final boolean result = Playoff.finishBracket(connection, challenge, tournament, bracketName);

      if (!result) {
        LOGGER.warn("Could not finish bracket "
            + bracketName
            + ", must have been a tie");
        SessionAttributes.appendToMessage(session, "<p id='error'>Error finishing playoff bracket "
            + bracketName
            + ". Perhaps it ends in a tie?</p>");
      } else {
        LOGGER.info("Finished playoff bracket "
            + bracketName);
        SessionAttributes.appendToMessage(session, "<p id='success'>Finished playoff bracket "
            + bracketName
            + ".</p>");
      }

      WebUtils.sendRedirect(application, response, "/playoff/index.jsp");
    } catch (SQLException | ParseException e) {
      LOGGER.error(e.getMessage(), e);
      throw new FLLRuntimeException("Database error finishing bracket", e);
    }
  }
}
