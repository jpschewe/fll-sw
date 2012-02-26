/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Formatter;

import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Team;
import fll.db.Queries;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * 
 */
public class FinalistLoad {

  /**
   * The name of the javascript variable that represents the team.
   */
  public static String getTeamVarName(final Team team) {
    return "team"
        + team.getTeamNumber();
  }

  /**
   * Output the variable definition for the specified team.
   * 
   * @param output where to write
   * @param team the team
   */
  public static void outputTeamVarDefinition(final Formatter output,
                                             final Team team) {
    final String teamVarName = getTeamVarName(team);
    output.format("var %s = $.finalist.lookupTeam(%d);%n", teamVarName, team.getTeamNumber());
    output.format("if(null == %s) {%n", teamVarName);
    output.format("  %s = $.finalist.addTeam(%d, %s, %s, %s);%n", teamVarName, team.getTeamNumber(),
                  WebUtils.quoteJavascriptString(team.getDivision()), WebUtils.quoteJavascriptString(team.getTrimmedTeamName()),
                  WebUtils.quoteJavascriptString(team.getOrganization()));
    output.format("}%n");
  }
  
  public static void outputTeamVariables(final Writer writer, final HttpSession session) throws SQLException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Connection connection = datasource.getConnection();
    final Formatter output = new Formatter(writer);
    for(final Team team : Queries.getTournamentTeams(connection).values()) {
      outputTeamVarDefinition(output, team);
    }
  }

  public static void outputDivisions(final Writer writer,
                                     final HttpSession session) throws SQLException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Connection connection = datasource.getConnection();
    final Formatter output = new Formatter(writer);
    for (final String division : Queries.getDivisions(connection)) {
      output.format("$.finalist.addDivision(%s);%n", WebUtils.quoteJavascriptString(division));
    }
  }
  
  /**
   * @return the current tournament name as a quoted javascript string
   */
  public static String currentTournament(final HttpSession session) throws SQLException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Connection connection = datasource.getConnection();
    final String name = Queries.getCurrentTournamentName(connection);
    return WebUtils.quoteJavascriptString(name);
  }

}
