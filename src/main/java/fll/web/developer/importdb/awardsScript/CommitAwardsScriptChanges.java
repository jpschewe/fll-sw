/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Set;

import javax.sql.DataSource;

import fll.Tournament;
import fll.db.AwardsScript;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.web.developer.importdb.ImportDBDump;
import fll.web.developer.importdb.ImportDbSessionInfo;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Commit changes for awards script differences.
 */
@WebServlet("/developer/importdb/awardsScript/CommitAwardsScriptChanges")
public class CommitAwardsScriptChanges extends BaseFLLServlet {

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

    final ImportDbSessionInfo sessionInfo = SessionAttributes.getNonNullAttribute(session,
                                                                                  ImportDBDump.IMPORT_DB_SESSION_KEY,
                                                                                  ImportDbSessionInfo.class);

    final DataSource sourceDataSource = sessionInfo.getImportDataSource();
    final DataSource destDataSource = ApplicationAttributes.getDataSource(application);

    final String tournamentName = sessionInfo.getTournamentName();
    if (null == tournamentName) {
      throw new FLLInternalException("Missing tournament to import");
    }

    try (Connection sourceConnection = sourceDataSource.getConnection();
        Connection destConnection = destDataSource.getConnection()) {

      final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournamentName);
      final Tournament destTournament = Tournament.findTournamentByName(destConnection, tournamentName);

      for (final ListIterator<AwardsScriptDifference> iter = sessionInfo.getAwardsScriptDifferences()
                                                                        .listIterator(); iter.hasNext();) {
        final int index = iter.nextIndex();
        final AwardsScriptDifference difference = iter.next();
        final String actionStr = WebUtils.getNonNullRequestParameter(request, String.format("difference_%d", index));
        final AwardsScriptDifferenceAction action = AwardsScriptDifferenceAction.valueOf(actionStr);

        if (difference instanceof SponsorsDifference) {
          final SponsorsDifference sd = (SponsorsDifference) difference;
          applySponsorDifference(sourceConnection, sourceTournament, destConnection, destTournament, sd, action);
        } else {
          throw new FLLInternalException(String.format("Unknown data type for awards script difference: %s",
                                                       difference.getClass().getName()));
        }
      }
    } catch (final SQLException sqle) {
      LOGGER.error(sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute(SessionAttributes.REDIRECT_URL, "/developer/importdb/awardsScript/CheckAwardsScriptInfo");
    WebUtils.sendRedirect(response, session);
  }

  private void applySponsorDifference(final Connection sourceConnection,
                                      final Tournament sourceTournament,
                                      final Connection destConnection,
                                      final Tournament destTournament,
                                      final SponsorsDifference sd,
                                      final AwardsScriptDifferenceAction action)
      throws SQLException {
    switch (action) {
    case KEEP_DESTINATION:
      AwardsScript.updateSponsorsForTournament(sourceConnection, sourceTournament, sd.getDestSponsors());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT:
      AwardsScript.updateSponsorsForTournament(destConnection, destTournament, sd.getSourceSponsors());
      break;
    case KEEP_SOURCE_AS_TOURNAMENT_LEVEL:
      AwardsScript.updateSponsorsForTournamentLevel(destConnection, destTournament.getLevel(), sd.getSourceSponsors());
      break;
    case KEEP_SOURCE_AS_SEASON:
      AwardsScript.updateSponsorsForSeason(destConnection, sd.getSourceSponsors());
      break;
    default:
      throw new FLLInternalException(String.format("Unknown enum value for %s: %s", action.getClass().getName(),
                                                   action.getName()));
    }

  }

}
