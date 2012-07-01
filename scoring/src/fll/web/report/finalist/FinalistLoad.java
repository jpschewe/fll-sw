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
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.db.Queries;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.web.ApplicationAttributes;
import fll.web.WebUtils;

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
   */
  public static void outputTeamVarDefinition(final Formatter output,
                                             final Team team) {
    final String teamVarName = getTeamVarName(team.getTeamNumber());
    output.format("var %s = $.finalist.lookupTeam(%d);%n", teamVarName, team.getTeamNumber());
    output.format("if(null == %s) {%n", teamVarName);
    output.format("  %s = $.finalist.addTeam(%d, %s, %s, %s);%n", teamVarName, team.getTeamNumber(),
                  WebUtils.quoteJavascriptString(team.getDivision()),
                  WebUtils.quoteJavascriptString(team.getTrimmedTeamName()),
                  WebUtils.quoteJavascriptString(team.getOrganization()));
    output.format("}%n");
  }

  public static void outputTeamVariables(final Writer writer,
                                         final ServletContext application) throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final Formatter output = new Formatter(writer);
      for (final Team team : Queries.getTournamentTeams(connection).values()) {
        outputTeamVarDefinition(output, team);
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
      final Formatter output = new Formatter(writer);
      for (final String division : Queries.getEventDivisions(connection)) {
        output.format("$.finalist.addDivision(%s);%n", WebUtils.quoteJavascriptString(division));
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
    final Document document = ApplicationAttributes.getChallengeDocument(application);
    final Element rootElement = document.getDocumentElement();
    final Formatter output = new Formatter(writer);

    for (final Element subjectiveElement : new NodelistElementCollectionAdapter(
                                                                                rootElement.getElementsByTagName("subjectiveCategory"))) {
      final String categoryName = subjectiveElement.getAttribute("name");
      final String categoryTitle = subjectiveElement.getAttribute("title");
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
      final Document document = ApplicationAttributes.getChallengeDocument(application);
      final Element rootElement = document.getDocumentElement();
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
        final String overallGroup;
        if (null == schedule) {
          overallGroup = "Unknown";
        } else {
          final TeamScheduleInfo si = schedule.getSchedInfoForTeam(teamNumber);
          overallGroup = si.getJudgingStation();
        }
        output.format("$.finalist.setCategoryScore(%s, championship, %.02f, %s);%n", teamVar, overallScore,
                      WebUtils.quoteJavascriptString(overallGroup));

        for (final Element subjectiveElement : new NodelistElementCollectionAdapter(
                                                                                    rootElement.getElementsByTagName("subjectiveCategory"))) {
          final String categoryName = subjectiveElement.getAttribute("name");
          final String categoryVar = getCategoryVarName(categoryName);
          final double catScore = rs.getDouble(categoryName);
          final String group;
          if (null == schedule) {
            group = Queries.computeScoreGroupForTeam(connection, tournament, categoryName, teamNumber);
          } else {
            final TeamScheduleInfo si = schedule.getSchedInfoForTeam(teamNumber);
            group = si.getJudgingStation();
          }

          output.format("$.finalist.setCategoryScore(%s, %s, %.02f, %s);%n", teamVar, categoryVar, catScore,
                        WebUtils.quoteJavascriptString(group));
        } // foreach category
      }// foreach team
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }
  }

}
