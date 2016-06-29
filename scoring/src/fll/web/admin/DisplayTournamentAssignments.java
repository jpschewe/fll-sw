/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.Tournament;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

@WebServlet("/admin/DisplayTournamentAssignments")
public class DisplayTournamentAssignments extends BaseFLLServlet {

  /**
   * @see fll.web.BaseFLLServlet#processRequest(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext,
   *      javax.servlet.http.HttpSession)
   */
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    PreparedStatement prep = null;
    ResultSet rs = null;
    Connection connection = null;
    try {
      response.setContentType("text/html");

      final Formatter formatter = new Formatter(response.getWriter());
      formatter.format("<html><body>");

      connection = datasource.getConnection();

      prep = connection.prepareStatement("select Teams.TeamNumber"//
          + " ,Teams.TeamName"//
          + " ,TournamentTeams.event_division"//
          + " ,TournamentTeams.judging_station"//
          + " FROM Teams,TournamentTeams"//
          + " WHERE TournamentTeams.Tournament = ?"//
          + " AND TournamentTeams.TeamNumber = Teams.TeamNumber"//
          + " ORDER BY Teams.TeamNumber" //
      );
      for (final Tournament tournament : Tournament.getTournaments(connection)) {
        formatter.format("<h1>%s</h1>", tournament.getName());

        formatter.format("<table border='1'>");
        formatter.format("<tr><th>Number</th><th>Name</th><th>Award Group</th><th>Judging Group</th></tr>");
        prep.setInt(1, tournament.getTournamentID());
        rs = prep.executeQuery();
        while (rs.next()) {
          formatter.format("<tr>");
          final int teamNum = rs.getInt(1);
          formatter.format("<td>%s</td>", teamNum);

          final String teamName = rs.getString(2);
          formatter.format("<td>%s</td>", teamName);

          final String eventDivision = rs.getString(3);
          formatter.format("<td>%s</td>", eventDivision);

          final String judgingGroup = rs.getString(4);
          formatter.format("<td>%s</td>", judgingGroup);

          formatter.format("</tr>");
        }
        formatter.format("</table>");
      }

      formatter.format("</body></html>");
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }

  }

}
