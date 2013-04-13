/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.web.ApplicationAttributes;
import fll.web.WebUtils;
import fll.web.playoff.Playoff;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * Support for /report/finalist/load.jsp.
 */
public class FinalistLoad {

  /**
   * The name of the javascript variable that represents the team.
   */
  public static String getTeamVarName(final int teamNumber) {
    return "team"
        + teamNumber;
  }

  /**
   * The name of the javascript variable that represents the category.
   */
  public static String getCategoryVarName(final String categoryName) {
    return categoryName
        + "Category";
  }

  /**
   * Output the variable definition for the specified team.
   * 
   * @param output where to write
   * @param team the team
   * @throws SQLException
   */
  public static void outputTeamVarDefinition(final Formatter output,
                                             final Connection connection,
                                             final TournamentTeam team) throws SQLException {
    final String teamVarName = getTeamVarName(team.getTeamNumber());
    output.format("var %s = $.finalist.lookupTeam(%d);%n", teamVarName, team.getTeamNumber());
    output.format("if(null == %s) {%n", teamVarName);
    output.format("  %s = $.finalist.addTeam(%d, %s, %s, %s);%n", teamVarName, team.getTeamNumber(),
                  WebUtils.quoteJavascriptString(team.getTrimmedTeamName()),
                  WebUtils.quoteJavascriptString(team.getOrganization()),
                  WebUtils.quoteJavascriptString(team.getJudgingStation()));
    output.format("}%n");

    output.format("$.finalist.addTeamToDivision(%s, %s);%n", teamVarName,
                  WebUtils.quoteJavascriptString(team.getEventDivision()));

    for (final String playoffDivision : Playoff.getPlayoffDivisionsForTeam(connection, team.getTeamNumber())) {
      output.format("$.finalist.addTeamToDivision(%s, %s);%n", teamVarName,
                    WebUtils.quoteJavascriptString(playoffDivision));
    }
  }

  public static void outputTeamVariables(final Writer writer,
                                         final ServletContext application) throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final Formatter output = new Formatter(writer);
      for (final TournamentTeam team : Queries.getTournamentTeams(connection).values()) {
        outputTeamVarDefinition(output, connection, team);
      }
    } finally {
      SQLFunctions.close(connection);
    }
  }

  public static void outputDivisions(final Writer writer,
                                     final ServletContext application) throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int tournament = Queries.getCurrentTournament(connection);

      final Formatter output = new Formatter(writer);
      for (final String division : Queries.getEventDivisions(connection, tournament)) {
        output.format("$.finalist.addDivision(%s);%n", WebUtils.quoteJavascriptString(division));
      }

      for (final String playoffDivision : Playoff.getPlayoffDivisions(connection, tournament)) {
        output.format("$.finalist.addDivision(%s);%n", WebUtils.quoteJavascriptString(playoffDivision));
      }

    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * @return the current tournament name as a quoted javascript string
   */
  public static String currentTournament(final ServletContext application) throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final String name = Queries.getCurrentTournamentName(connection);
      return WebUtils.quoteJavascriptString(name);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Output the declarations for the category variables.
   */
  public static void outputCategories(final Writer writer,
                                      final ServletContext application) {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Formatter output = new Formatter(writer);

    for (final ScoreCategory subjectiveElement : description.getSubjectiveCategories()) {
      final String categoryName = subjectiveElement.getName();
      final String categoryTitle = subjectiveElement.getTitle();
      final String quotedCatTitle = WebUtils.quoteJavascriptString(categoryTitle);

      final String catVarName = getCategoryVarName(categoryName);
      output.format("var %s = $.finalist.getCategoryByName(%s);%n", catVarName, quotedCatTitle);
      output.format("if (null == %s) {%n", catVarName);
      output.format("  %s = $.finalist.addCategory(%s, true);%n", catVarName, quotedCatTitle);
      output.format("}%n");
    }
  }

  public static void outputCategoryScores(final Writer writer,
                                          final ServletContext application) throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {

      connection = datasource.getConnection();
      final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
      final int tournament = Queries.getCurrentTournament(connection);
      final Formatter output = new Formatter(writer);

      final TournamentSchedule schedule;
      if (TournamentSchedule.scheduleExistsInDatabase(connection, tournament)) {
        schedule = new TournamentSchedule(connection, tournament);
      } else {
        schedule = null;
      }

      prep = connection.prepareStatement("SELECT * from FinalScores WHERE tournament = ?");
      prep.setInt(1, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int teamNumber = rs.getInt("TeamNumber");

        final String teamVar = getTeamVarName(teamNumber);
        final double overallScore = rs.getDouble("OverallScore");
        output.format("$.finalist.setCategoryScore(%s, championship, %.02f);%n", teamVar, overallScore);

        for (final ScoreCategory subjectiveElement : description.getSubjectiveCategories()) {
          final String categoryName = subjectiveElement.getName();
          final String categoryVar = getCategoryVarName(categoryName);
          final double catScore = rs.getDouble(categoryName);
          output.format("$.finalist.setCategoryScore(%s, %s, %.02f);%n", teamVar, categoryVar, catScore);
        } // foreach category
      }// foreach team
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }
  }

}
