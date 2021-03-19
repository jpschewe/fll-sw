/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;

/**
 * Store data from /admin/tournaments.jsp.
 */
@WebServlet("/admin/StoreTournamentData")
public class StoreTournamentData extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /** This matches the format used by the jquery UI datepicker. */
  public static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.MONTH_OF_YEAR,
                                                                                                    2)
                                                                                       .appendLiteral('/')
                                                                                       .appendValue(ChronoField.DAY_OF_MONTH,
                                                                                                    2)
                                                                                       .appendLiteral('/')
                                                                                       .appendValue(ChronoField.YEAR, 4)
                                                                                       .toFormatter();

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
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {

      createAndInsertTournaments(request, connection, description, message);

      message.append("<p id='success'>Committed tournament changes.</p>");

      SessionAttributes.appendToMessage(session, message.toString());

      // finally redirect to index.jsp
      // out.println("DEBUG: normally you'd be redirected to <a
      // href='index.jsp'>here</a>");
      response.sendRedirect(response.encodeRedirectURL("index.jsp"));

    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database", e);
      throw new FLLInternalException(e);
    }
  }

  private static void createAndInsertTournaments(final HttpServletRequest request,
                                                 final Connection connection,
                                                 final ChallengeDescription challengeDescription,
                                                 final StringBuilder message)
      throws SQLException {
    final int currentTournament = Queries.getCurrentTournament(connection);

    int row = 0;
    String keyStr = request.getParameter("key"
        + row);
    String name = request.getParameter("name"
        + row);
    String description = request.getParameter("description"
        + row);
    String dateStr = request.getParameter("date"
        + row);
    String level = request.getParameter("level"
        + row);
    String nextLevel = request.getParameter("nextLevel"
        + row);
    while (null != keyStr) {
      final int key = Integer.parseInt(keyStr);

      final LocalDate date;
      if (null == dateStr
          || dateStr.trim().isEmpty()) {
        date = null;
      } else {
        date = LocalDate.parse(dateStr, DATE_FORMATTER);
      }

      if (Tournaments.NEW_TOURNAMENT_KEY == key) {
        if (null != name
            && !"".equals(name)) {
          // new tournament
          Tournament.createTournament(connection, name, description, date, level, nextLevel);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Adding a new tournament "
                + " name: "
                + name
                + " description: "
                + description
                + " date: "
                + date);
          }
        }
      } else if (null == name
          || "".equals(name)) {
        if (Tournament.doesTournamentExist(connection, key)) {
          final Tournament tournament = Tournament.findTournamentByID(connection, key);
          if (key == currentTournament) {
            message.append("<p class='warning'>Unable to delete tournament '"
                + tournament.getName()
                + "' because it is the current tournament</p>");
          } else {
            // delete if no name
            if (tournament.containsScores(connection, challengeDescription)) {
              message.append("<p class='warning'>Unable to delete tournament '"
                  + tournament.getName()
                  + "' that contains scores</p>");
            } else {
              Tournament.deleteTournament(connection, key);
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deleting a tournament "
                    + key);
              }
            }
          } // not current tournament
        } // tournament exists
      } else {
        // update with new values

        Tournament.updateTournament(connection, key, name, description, date, level, nextLevel);

        LOGGER.debug("Updating a tournament {} name: {} description: {} date: {} level: {} nextLevel: {}", key, name,
                     description, date, level, nextLevel);
      }

      row++;
      keyStr = request.getParameter("key"
          + row);
      name = request.getParameter("name"
          + row);
      description = request.getParameter("description"
          + row);
      dateStr = request.getParameter("date"
          + row);
      level = request.getParameter("level"
          + row);
      nextLevel = request.getParameter("nextLevel"
          + row);
    }
  }

}
