/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.text.StringEscapeUtils;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
import fll.Tournament;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.playoff.Playoff;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.util.StringUtils;

/**
 * Generate javascript to update the unverified runs in select_team.jsp.
 */
@WebServlet("/scoreEntry/UpdateUnverifiedRuns")
public class UpdateUnverifiedRuns extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.REF), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection();
        PreparedStatement prep = connection.prepareStatement("SELECT"
            + "     Performance.TeamNumber"
            + "    ,Performance.RunNumber"
            + "    ,Teams.TeamName"
            + "     FROM Performance, Teams"
            + "     WHERE Verified != TRUE"
            + "       AND Tournament = ?"
            + "       AND Teams.TeamNumber = Performance.TeamNumber"
            + "       ORDER BY Performance.RunNumber, Teams.TeamNumber")) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final List<String> unfinishedBrackets = Playoff.getUnfinishedPlayoffBrackets(connection,
                                                                                   tournament.getTournamentID());

      prep.setInt(1, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        int index = 0;
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final int runNumber = rs.getInt(2);
          final String name = castNonNull(rs.getString(3));
          final String trimmedName = StringUtils.trimString(name, Team.MAX_TEAM_NAME_LEN);
          final String escapedName = StringEscapeUtils.escapeEcmaScript(trimmedName);

          final String playoffBracketName = Playoff.getPlayoffBracketsForTeam(connection, teamNumber).stream() //
                                                   .filter(b -> unfinishedBrackets.contains(b)) //
                                                   .findFirst() //
                                                   .orElse("");

          final String runNumberDisplay;
          if (!playoffBracketName.isEmpty()) {
            final int playoffRound = Playoff.getPlayoffRound(connection, tournament.getTournamentID(),
                                                             playoffBracketName, runNumber);
            runNumberDisplay = String.format("%s P%d", playoffBracketName, playoffRound);
          } else {
            runNumberDisplay = String.valueOf(runNumber);
          }

          final String displayString;
          displayString = String.format("Run %s - %d [%s]", runNumberDisplay, teamNumber, escapedName);

          response.getWriter()
                  .println(String.format("document.verify.TeamNumber.options[%d]=new Option(\"%s\", \"%d-%d\", true, false);",
                                         index, displayString, teamNumber, runNumber));

          ++index;
        } // foreach result
      } // result set

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

}
