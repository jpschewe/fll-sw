/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Tournament;
import fll.TournamentLevel;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Servlet to create a tournament.
 *
 * @author jpschewe
 */
@WebServlet("/developer/importdb/CreateTournament")
public class CreateTournament extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

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

    // unset redirect url
    session.setAttribute(SessionAttributes.REDIRECT_URL, null);

    try {
      final String answer = request.getParameter("submit_data");
      if (LOG.isTraceEnabled()) {
        LOG.trace("Submit to CreateTournament: '"
            + answer
            + "'");
      }

      if ("Yes".equals(answer)) {
        createSelectedTournament(message, application, session);
      } else {
        message.append("<p>Canceled request to create tournament</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
      }
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Message is: "
          + message);
      LOG.trace("Redirect URL is: "
          + SessionAttributes.getRedirectURL(session));
    }

    session.setAttribute("message", message.toString());
    WebUtils.sendRedirect(response, session);
  }

  /**
   * Create the tournament that is in the session variable
   * <code>selectedTournament</code>.
   *
   * @param session the session
   * @throws SQLException
   */
  private static void createSelectedTournament(final StringBuilder message,
                                               final ServletContext application,
                                               final HttpSession session)
      throws SQLException {
    final ImportDbSessionInfo sessionInfo = SessionAttributes.getNonNullAttribute(session,
                                                                                  ImportDBDump.IMPORT_DB_SESSION_KEY,
                                                                                  ImportDbSessionInfo.class);
    final String tournament = sessionInfo.getTournamentName();
    if (null == tournament) {
      throw new FLLInternalException("Missing tournament to import");
    }

    final DataSource sourceDataSource = sessionInfo.getImportDataSource();

    final DataSource destDataSource = ApplicationAttributes.getDataSource(application);

    try (Connection sourceConnection = sourceDataSource.getConnection();
        Connection destConnection = destDataSource.getConnection()) {
      createTournament(sourceConnection, destConnection, tournament, message, session);
    }
  }

  /**
   * Create a tournament from the source in the dest. This recurses on
   * nextTournament if needed.
   *
   * @throws SQLException
   */
  private static void createTournament(final Connection sourceConnection,
                                       final Connection destConnection,
                                       final String tournamentName,
                                       final StringBuilder message,
                                       final HttpSession session)
      throws SQLException {
    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournamentName);

    createTournamentLevels(sourceConnection, destConnection, sourceTournament.getTournamentID());

    Tournament.createTournament(destConnection, sourceTournament.getName(), sourceTournament.getDescription(),
                                sourceTournament.getDate(), sourceTournament.getLevel());

    message.append("<p>Created tournament "
        + sourceTournament.getName()
        + "</p>");

    if (null == SessionAttributes.getRedirectURL(session)) {
      session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTournamentExists");
    }

  }

  /**
   * This assumes that the level to next level associations are the same between
   * source and destination. This method just checks that the level for
   * {@code sourceTournamentID} or one of it's next levels exists.
   */
  private static void createTournamentLevels(final Connection sourceConnection,
                                             final Connection destinationConnection,
                                             final int sourceTournamentID)
      throws SQLException {
    try (
        PreparedStatement getLevelId = sourceConnection.prepareStatement("SELECT level_id FROM tournaments WHERE tournament_id = ?")) {
      getLevelId.setInt(1, sourceTournamentID);
      try (ResultSet rs = getLevelId.executeQuery()) {
        if (rs.next()) {
          final int sourceLevelId = rs.getInt(1);
          final TournamentLevel sourceLevel = TournamentLevel.getById(sourceConnection, sourceLevelId);
          findOrCreateTournamentLevel(sourceConnection, sourceLevel, destinationConnection);
        }
        // else nothing to do
      }
    }
  }

  /**
   * @param sourceConnection source database
   * @param sourceLevel source level that needs to exist in the destination
   *          database
   * @param destConnection destination database
   * @return level in the destination database
   * @throws SQLException on a database error
   */
  private static TournamentLevel findOrCreateTournamentLevel(final Connection sourceConnection,
                                                             final TournamentLevel sourceLevel,
                                                             final Connection destConnection)
      throws SQLException {
    final TournamentLevel destLevel;
    if (TournamentLevel.levelExists(destConnection, sourceLevel.getName())) {
      destLevel = TournamentLevel.getByName(destConnection, sourceLevel.getName());
    } else if (TournamentLevel.NO_NEXT_LEVEL_ID == sourceLevel.getNextLevelId()) {
      destLevel = TournamentLevel.createTournamentLevel(sourceConnection, sourceLevel.getName());
    } else {
      final TournamentLevel sourceNextLevel = TournamentLevel.getById(sourceConnection, sourceLevel.getNextLevelId());
      destLevel = findOrCreateTournamentLevel(sourceConnection, sourceNextLevel, destConnection);
    }
    return destLevel;
  }

}
