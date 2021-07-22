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
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.NonNumericNominees;
import fll.db.NonNumericNominees.Nominee;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.WebUtils;
import fll.web.playoff.Playoff;
import fll.web.report.FinalComputedScores;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.WinnerType;

/**
 * Support for /report/finalist/load.jsp.
 */
public final class FinalistLoad {
  private FinalistLoad() {
  }

  /**
   * The name of the javascript variable that represents the team.
   *
   * @return the variable name to use for the specified team number
   * @param teamNumber the team to get the variable for
   */
  public static String getTeamVarName(final int teamNumber) {
    return "team"
        + teamNumber;
  }

  /**
   * The name of the javascript variable that represents the category.
   *
   * @param categoryName the category to get the variable for
   * @return the variable name to use for the category
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
   * @param connection the database connection
   * @throws SQLException if there is a problem talking to the database
   */
  public static void outputTeamVarDefinition(final Formatter output,
                                             final Connection connection,
                                             final TournamentTeam team)
      throws SQLException {
    final String teamVarName = getTeamVarName(team.getTeamNumber());
    output.format("var %s = $.finalist.lookupTeam(%d);%n", teamVarName, team.getTeamNumber());
    output.format("if(null == %s) {%n", teamVarName);
    output.format("  %s = $.finalist.addTeam(%d, %s, %s, %s);%n", teamVarName, team.getTeamNumber(),
                  WebUtils.quoteJavascriptString(team.getTrimmedTeamName()),
                  WebUtils.quoteJavascriptString(team.getOrganization()),
                  WebUtils.quoteJavascriptString(team.getJudgingGroup()));
    output.format("}%n");

    output.format("$.finalist.addTeamToDivision(%s, %s);%n", teamVarName,
                  WebUtils.quoteJavascriptString(team.getAwardGroup()));

    for (final String playoffDivision : Playoff.getPlayoffBracketsForTeam(connection, team.getTeamNumber())) {
      output.format("$.finalist.addTeamToPlayoffDivision(%s, %s);%n", teamVarName,
                    WebUtils.quoteJavascriptString(playoffDivision));
    }
  }

  /**
   * Output the variables for the teams.
   *
   * @param writer where to write
   * @param application the application context
   * @throws SQLException if a database error occurs
   */
  public static void outputTeamVariables(final Writer writer,
                                         final ServletContext application)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Formatter output = new Formatter(writer);
      for (final TournamentTeam team : Queries.getTournamentTeams(connection).values()) {
        outputTeamVarDefinition(output, connection, team);
      }
    }
  }

  /**
   * Output the award group information.
   *
   * @param writer where to write
   * @param application the application context
   * @throws SQLException if a database errors occurs
   */
  public static void outputDivisions(final Writer writer,
                                     final ServletContext application)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);

      final Formatter output = new Formatter(writer);
      for (final String division : Queries.getAwardGroups(connection, tournament)) {
        output.format("$.finalist.addDivision(%s);%n", WebUtils.quoteJavascriptString(division));
      }

      for (final String playoffDivision : Playoff.getPlayoffBrackets(connection, tournament)) {
        output.format("$.finalist.addPlayoffDivision(%s);%n", WebUtils.quoteJavascriptString(playoffDivision));
      }

    }
  }

  /**
   * @param application the application context to get information from
   * @return the current tournament name as a quoted javascript string
   * @throws SQLException on a database error
   */
  public static String currentTournament(final ServletContext application) throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final String name = Tournament.getCurrentTournament(connection).getName();
      return WebUtils.quoteJavascriptString(name);
    }
  }

  /**
   * Output the declarations for the category variables.
   *
   * @param writer where to write
   * @param application the application context to get information from
   */
  public static void outputCategories(final Writer writer,
                                      final ServletContext application) {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Formatter output = new Formatter(writer);

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      final String categoryName = category.getName();
      final String categoryTitle = category.getTitle();
      final String quotedCatTitle = WebUtils.quoteJavascriptString(categoryTitle);

      final String catVarName = getCategoryVarName(categoryName);
      output.format("var %s = $.finalist.getCategoryByName(%s);%n", catVarName, quotedCatTitle);
      output.format("if (null == %s) {%n", catVarName);
      output.format("  %s = $.finalist.addCategory(%s, true, false);%n", catVarName, quotedCatTitle);
      output.format("}%n");
      // all subjective categories are scheduled
      output.format("$.finalist.setCategoryScheduled(%s, true);%n", catVarName);
    }

    for (final NonNumericCategory category : description.getNonNumericCategories()) {
      final String categoryTitle = category.getTitle();
      final String quotedCatTitle = WebUtils.quoteJavascriptString(categoryTitle);

      output.format("{%n");
      output.format("var category = $.finalist.getCategoryByName(%s);%n", quotedCatTitle);
      output.format("if (null == category) {%n");
      output.format("  category = $.finalist.addCategory(%s, false, %b);%n", quotedCatTitle,
                    !category.getPerAwardGroup());
      output.format("}%n");
      output.format("}%n");
    }

  }

  /**
   * Output javascript to load the subjective nominees into the finalist
   * schedule
   * web application.
   *
   * @param writer where to write
   * @param application the application context
   * @throws SQLException on a database error
   */
  public static void outputNonNumericNominees(final Writer writer,
                                              final ServletContext application)
      throws SQLException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final List<NonNumericCategory> challengeNonNumericCategories = description.getNonNumericCategories();
    final ObjectMapper mapper = new ObjectMapper();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);
      final Formatter output = new Formatter(writer);

      for (final String category : NonNumericNominees.getCategories(connection, tournament)) {
        output.format("{%n"); // scope to simplify variable names
        final String categoryVar = "category";
        final String quotedCatTitle = WebUtils.quoteJavascriptString(category);

        output.format("var %s = $.finalist.getCategoryByName(%s);%n", categoryVar, quotedCatTitle);

        if (!challengeNonNumericCategories.stream().anyMatch(c -> c.getTitle().equals(category))) {
          // 8/22/2020 JPS - this isn't needed other than support for old databases where
          // the non-numeric categories are not in the challenge description. All of these
          // categories are per award group.

          output.format("if (null == %s) {%n", categoryVar);
          output.format("  %s = $.finalist.addCategory(%s, false, false);%n", categoryVar, quotedCatTitle);
          output.format("}%n");
        }

        // clear judges and teams from category as we want to use whatever is coming
        // from the database
        output.format("$.finalist.clearCategoryTeams(%s);%n", categoryVar);
        output.format("$.finalist.clearCategoryNominatingJudges(%s);%n", categoryVar);

        for (final Nominee nominee : NonNumericNominees.getNominees(connection, tournament, category)) {
          output.format("$.finalist.addTeamToCategory(%s, %d);%n", categoryVar, nominee.getTeamNumber());

          final String judgesStr = mapper.writeValueAsString(nominee.getJudges());
          output.format("$.finalist.setNominatingJudges(%s, %d, %s);%n", categoryVar, nominee.getTeamNumber(),
                        judgesStr);
        }
        output.format("}%n");
      } // foreach category
    } catch (final JsonProcessingException e) {
      throw new FLLInternalException("Error converting judges to JSON", e);
    }
  }

  /**
   * Output the category scores.
   *
   * @param writer where to write
   * @param application the application context
   * @throws SQLException if a database error occurs
   */
  public static void outputCategoryScores(final Writer writer,
                                          final ServletContext application)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final Formatter output = new Formatter(writer);

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT team_number, overall_score from overall_scores WHERE tournament = ?")) {
        prep.setInt(1, tournament.getTournamentID());

        try (ResultSet rs = prep.executeQuery()) {
          // defined by load.jsp
          final String championshipCategoryVar = "championship";
          while (rs.next()) {
            final int teamNumber = rs.getInt(1);
            final String teamVar = getTeamVarName(teamNumber);

            final double overallScore = rs.getDouble(2);
            output.format("$.finalist.setCategoryScore(%s, %s, %.02f);%n", teamVar, championshipCategoryVar,
                          overallScore);
          } // foreach team
        } // result set
      } // prepared statement

      final WinnerType winnerCriteria = description.getWinner();
      final List<String> awardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());
      final List<String> judgingStations = Queries.getJudgingStations(connection, tournament.getTournamentID());
      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final String categoryName = category.getName();
        final String categoryVar = getCategoryVarName(categoryName);

        for (final String awardGroup : awardGroups) {
          for (final String judgingStation : judgingStations) {
            FinalComputedScores.iterateOverSubjectiveScores(connection, category, winnerCriteria, tournament,
                                                            awardGroup, judgingStation, (teamNumber,
                                                                                         score,
                                                                                         rank) -> {

                                                              final String teamVar = getTeamVarName(teamNumber);

                                                              output.format("$.finalist.setCategoryScore(%s, %s, %.02f);%n",
                                                                            teamVar, categoryVar, score);

                                                            });
          } // judging station
        } // award group
      } // category

    } // connection

  }

  /**
   * Output the schedules.
   *
   * @param writer where to write
   * @param application the application context
   * @throws SQLException if there is a database error
   */
  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
  public static void outputSchedules(final Writer writer,
                                     final ServletContext application)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);
      final Formatter output = new Formatter(writer);

      for (final String division : FinalistSchedule.getAllDivisions(connection, tournament)) {
        output.format("{%n");

        output.format("var division = '%s';%n", division);

        final FinalistSchedule schedule = new FinalistSchedule(connection, tournament, division);
        for (final Map.Entry<String, @Nullable String> entry : schedule.getRooms().entrySet()) {
          final String categoryTitle = entry.getKey();
          final String quotedCatTitle = WebUtils.quoteJavascriptString(categoryTitle);
          final @Nullable String room = entry.getValue();

          output.format("{%n"); // scope so that variable names are easy

          output.format("  var category = $.finalist.getCategoryByName(%s);%n", quotedCatTitle);
          output.format("  $.finalist.setRoom(category, division, %s);%n", WebUtils.quoteJavascriptString(room));

          // any category in the schedule should be scheduled
          output.format("$.finalist.setCategoryScheduled(category, true);%n");

          output.format("}%n");
        }

        output.format("}%n");

      }
    }

  }

}
