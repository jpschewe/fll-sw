/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.diffplug.common.base.Errors;

import fll.Tournament;
import fll.TournamentLevel;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Java code used in /admin/tournaments.jsp.
 */
@WebServlet("/admin/Tournaments")
public final class Tournaments extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String NEW_TOURNAMENT_PREFIX = "new_";

  private static final String KEY_PREFIX = "key_";

  private static final String NAME_PREFIX = "name_";

  private static final String DESCRIPTION_PREFIX = "description_";

  private static final String DATE_PREFIX = "date_";

  private static final String LEVEL_PREFIX = "level_";

  /**
   * Populate page context with variables for /admin/tournaments.jsp.
   *
   * @param application the application context
   * @param pageContext populated with variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection(); Statement stmt = connection.createStatement()) {

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      pageContext.setAttribute("tournaments", tournaments);

      pageContext.setAttribute("NEW_TOURNAMENT_PREFIX", NEW_TOURNAMENT_PREFIX);
      pageContext.setAttribute("KEY_PREFIX", KEY_PREFIX);
      pageContext.setAttribute("NAME_PREFIX", NAME_PREFIX);
      pageContext.setAttribute("DESCRIPTION_PREFIX", DESCRIPTION_PREFIX);
      pageContext.setAttribute("DATE_PREFIX", DATE_PREFIX);
      pageContext.setAttribute("LEVEL_PREFIX", LEVEL_PREFIX);

      final int currentTournament = Queries.getCurrentTournament(connection);
      pageContext.setAttribute("currentTournamentId", currentTournament);

      final Set<TournamentLevel> tournamentLevels = TournamentLevel.getAllLevels(connection);
      pageContext.setAttribute("tournamentLevels", tournamentLevels);

      final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

      final Set<Integer> tournamentsWithScores = tournaments.stream() //
                                                            .filter(Errors.rethrow()
                                                                          .wrapPredicate(t -> t.containsScores(connection,
                                                                                                               description))) //
                                                            .map(Tournament::getTournamentID) //
                                                            .collect(Collectors.toSet());
      pageContext.setAttribute("tournamentsWithScores", tournamentsWithScores);

    } catch (final SQLException sqle) {
      LOGGER.error(sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }
  }

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
    final Collection<Tournament> prevTournaments = Tournament.getTournaments(connection);
    final Set<Integer> tournamentsSeen = new HashSet<>();

    for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      final String parameterName = entry.getKey();
      if (parameterName.startsWith(KEY_PREFIX)) {
        final String idStr = entry.getValue()[0];
        try {
          final int rowIndex = Integer.parseInt(parameterName.substring(KEY_PREFIX.length()));

          final String nameParamName = String.format("%s%d", NAME_PREFIX, rowIndex);
          final String name = request.getParameter(nameParamName);
          if (null == name) {
            throw new FLLInternalException("Received results from editing tournaments with missing tournament name row: "
                + rowIndex);
          }

          final String descriptionParamName = String.format("%s%d", DESCRIPTION_PREFIX, rowIndex);
          final @Nullable String description = request.getParameter(descriptionParamName);

          final String dateParamName = String.format("%s%d", DATE_PREFIX, rowIndex);
          final @Nullable String dateStr = request.getParameter(dateParamName);
          final LocalDate date;
          if (null == dateStr
              || dateStr.trim().isEmpty()) {
            date = null;
          } else {
            date = LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
          }

          final String levelParamName = String.format("%s%d", LEVEL_PREFIX, rowIndex);
          final String levelIdStr = request.getParameter(levelParamName);
          if (null == levelIdStr) {
            throw new FLLInternalException("Received results from editing tournaments with missing level id row: "
                + rowIndex);
          }

          final int levelId = Integer.parseInt(levelIdStr);
          final TournamentLevel level = TournamentLevel.getById(connection, levelId);

          if (idStr.startsWith(NEW_TOURNAMENT_PREFIX)) {
            LOGGER.debug("Adding a new tournament name: {} description: {} date: {} level: {}", name, description, date,
                         level.getName());
            Tournament.createTournament(connection, name, description, date, level);
          } else {
            final int tournamentId = Integer.parseInt(idStr);
            tournamentsSeen.add(tournamentId);

            LOGGER.debug("Updating a tournament {} name: {} description: {} date: {} level: {}", tournamentId, name,
                         description, date, level);
            Tournament.updateTournament(connection, tournamentId, name, description, date, level);
          }
        } catch (final NumberFormatException e) {
          throw new FLLInternalException("Error parsing integer data from editing of tournaments", e);
        }
      }
    }

    // delete any tournaments not sent back in the request
    final Tournament currentTournament = Tournament.getCurrentTournament(connection);
    for (final Tournament tournament : prevTournaments) {
      if (!tournamentsSeen.contains(tournament.getTournamentID())) {

        if (tournament.containsScores(connection, challengeDescription)) {
          message.append("<p class='warning'>Unable to delete tournament '"
              + tournament.getName()
              + "' that contains scores</p>");
        } else if (tournament.equals(currentTournament)) {
          message.append("<p class='warning'>Unable to delete tournament '"
              + tournament.getName()
              + "' because it is the current tournament</p>");
        } else {
          Tournament.deleteTournament(connection, tournament.getTournamentID());
        }
      }
    }

  }

}
