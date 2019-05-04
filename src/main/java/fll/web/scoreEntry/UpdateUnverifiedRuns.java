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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.text.StringEscapeUtils;

import fll.Team;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
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
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (final Connection connection = datasource.getConnection();
        final PreparedStatement prep = connection.prepareStatement("SELECT"
            + "     Performance.TeamNumber"
            + "    ,Performance.RunNumber"
            + "    ,Teams.TeamName"
            + "     FROM Performance, Teams"
            + "     WHERE Verified != TRUE"
            + "       AND Tournament = ?"
            + "       AND Teams.TeamNumber = Performance.TeamNumber"
            + "       ORDER BY Performance.RunNumber, Teams.TeamNumber")) {

      prep.setInt(1, Queries.getCurrentTournament(connection));

      try (final ResultSet rs = prep.executeQuery()) {
        int index = 0;
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final int runNumber = rs.getInt(2);
          final String name = rs.getString(3);
          final String trimmedName = StringUtils.trimString(name, Team.MAX_TEAM_NAME_LEN);
          final String escapedName = StringEscapeUtils.escapeEcmaScript(trimmedName);

          response.getWriter()
                  .println(String.format("document.verify.TeamNumber.options[%d]=new Option(\"Run %d - %d [%s]\", \"%d-%d\", true, false);",
                                         index, teamNumber, runNumber, escapedName, teamNumber, runNumber));

          ++index;
        } // foreach result
      } // result set

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

}
