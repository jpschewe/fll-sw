/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.diffplug.common.base.Errors;
import com.opencsv.CSVWriter;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
import fll.Tournament;
import fll.TournamentLevel;
import fll.db.AdvancingTeam;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.db.CategoriesIgnored;
import fll.db.OverallAwardWinner;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.api.AwardsReportSortedGroupsServlet;
import fll.web.playoff.Playoff;
import fll.web.report.PromptSummarizeScores;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Awards CSV file for shipping awards.
 */
@WebServlet("/report/Awards.csv")
public class AwardsCSV extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    if (PromptSummarizeScores.checkIfSummaryUpdated(request, response, application, session, "/report/Awards.csv")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      response.reset();
      response.setContentType("text/csv");
      response.setHeader("Content-Disposition", "filename=awards.csv");

      try (CSVWriter csv = new CSVWriter(response.getWriter())) {

        writeHeader(csv);
        writeData(connection, description, csv);
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeHeader(final CSVWriter csv) {
    csv.writeNext(new String[] { "team #", "team name", "award" });
  }

  private void writeData(final Connection connection,
                         final ChallengeDescription description,
                         final CSVWriter csv)
      throws SQLException, IOException {
    final Tournament tournament = Tournament.getCurrentTournament(connection);

    final List<String> sortedAwardGroups = AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                                tournament.getTournamentID());

    addSubjectiveExtraWinners(connection, description, csv, tournament, sortedAwardGroups, true);

    addPerformance(connection, csv, description, sortedAwardGroups);

    if (TournamentParameters.getRunningHeadToHead(connection, tournament.getTournamentID())) {
      addHeadToHead(connection, tournament, csv, sortedAwardGroups);
    }

    addSubjectiveChallengeWinners(connection, description, csv, tournament, sortedAwardGroups);
    addVirtualSubjectiveChallengeWinners(connection, description, csv, tournament, sortedAwardGroups);
    addSubjectiveExtraWinners(connection, description, csv, tournament, sortedAwardGroups, false);
    addSubjectiveOverallWinners(connection, description, csv, tournament);

    final List<AdvancingTeam> advancing = AdvancingTeam.loadAdvancingTeams(connection, tournament.getTournamentID());
    if (!advancing.isEmpty()) {
      addAdvancingTeams(advancing, connection, csv, tournament, sortedAwardGroups);
    }
  }

  private static void addHeadToHead(final Connection connection,
                                    final Tournament tournament,
                                    final CSVWriter csv,
                                    final List<String> sortedGroups)
      throws SQLException, IOException {

    final List<String> playoffDivisions = Playoff.getCompletedBrackets(connection, tournament.getTournamentID());
    if (!playoffDivisions.isEmpty()) {
      final List<String> sortedDivisions = new LinkedList<>(sortedGroups);
      playoffDivisions.stream().filter(e -> !sortedGroups.contains(e)).forEach(sortedDivisions::add);

      for (final String division : sortedDivisions) {
        if (playoffDivisions.contains(division)) {
          processDivision(connection, tournament, csv, division);
        }
      }
    }

  }

  private static void processDivision(final Connection connection,
                                      final Tournament tournament,
                                      final CSVWriter csv,
                                      final String division)
      throws SQLException, IOException {

    final String awardName = "Head to head bracket "
        + division;

    final int maxRun = Playoff.getMaxPerformanceRound(connection, tournament.getTournamentID(), division);

    if (maxRun >= 1) {
      try (
          PreparedStatement teamPrep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Organization" //
              + " FROM PlayoffData, Teams" //
              + " WHERE PlayoffData.Tournament = ?" //
              + " AND PlayoffData.event_division = ?" //
              + " AND PlayoffData.run_number = ?" //
              + " AND Teams.TeamNumber = PlayoffData.team"//
              + " ORDER BY PlayoffData.linenumber" //
          )) {
        teamPrep.setInt(1, tournament.getTournamentID());
        teamPrep.setString(2, division);
        teamPrep.setInt(3, maxRun);
        try (ResultSet team2Result = teamPrep.executeQuery()) {
          while (team2Result.next()) {
            final int teamNumber = team2Result.getInt(1);
            final String teamName = castNonNull(team2Result.getString(2));

            final String placeText = String.format("%d - winner", awardName);

            csv.writeNext(new String[] { String.valueOf(teamNumber), teamName, placeText });

          } // foreach result
        } // teamResult
      } // prepared statements
    } // finished playoff
  }

  private void addSubjectiveChallengeWinners(final Connection connection,
                                             final ChallengeDescription description,
                                             final CSVWriter csv,
                                             final Tournament tournament,
                                             final List<String> sortedAwardGroups)
      throws SQLException {
    final List<AwardWinner> winners = AwardWinners.getSubjectiveAwardWinners(connection, tournament.getTournamentID());

    final List<String> categoryOrder = description.getSubjectiveCategories().stream() //
                                                  .map(SubjectiveScoreCategory::getTitle) //
                                                  .collect(Collectors.toList());
    addSubjectiveWinners(connection, description, csv, winners, sortedAwardGroups, categoryOrder, false);
  }

  private void addVirtualSubjectiveChallengeWinners(final Connection connection,
                                                    final ChallengeDescription description,
                                                    final CSVWriter csv,
                                                    final Tournament tournament,
                                                    final List<String> sortedAwardGroups)
      throws SQLException {
    final List<AwardWinner> winners = AwardWinners.getVirtualSubjectiveAwardWinners(connection,
                                                                                    tournament.getTournamentID());

    final List<String> categoryOrder = description.getSubjectiveCategories().stream() //
                                                  .map(SubjectiveScoreCategory::getTitle) //
                                                  .collect(Collectors.toList());
    addSubjectiveWinners(connection, description, csv, winners, sortedAwardGroups, categoryOrder, false);
  }

  private void addSubjectiveExtraWinners(final Connection connection,
                                         final ChallengeDescription description,
                                         final CSVWriter csv,
                                         final Tournament tournament,
                                         final List<String> sortedAwardGroups,
                                         final boolean displayChampionship)
      throws SQLException, IOException {
    final TournamentLevel level = tournament.getLevel();

    final List<AwardWinner> winners = AwardWinners.getNonNumericAwardWinners(connection, tournament.getTournamentID())
                                                  .stream() //
                                                  .filter(Errors.rethrow()
                                                                .wrapPredicate(w -> AwardsReport.isNonNumericAwarded(connection,
                                                                                                                     description,
                                                                                                                     level,
                                                                                                                     w))) //
                                                  .collect(Collectors.toList());

    final List<String> categoryOrder = CategoriesIgnored.getNonNumericCategories(description, connection, tournament)
                                                        .stream() //
                                                        .map(NonNumericCategory::getTitle) //
                                                        .collect(Collectors.toList());

    // show places when showing championship
    addSubjectiveWinners(connection, description, csv, winners, sortedAwardGroups, categoryOrder, displayChampionship);
  }

  /**
   * @param displayChampionship if true, then display only the championship award,
   *          otherwise skip the championship award
   */
  private void addSubjectiveWinners(final Connection connection,
                                    final ChallengeDescription description,
                                    final CSVWriter csv,
                                    final List<AwardWinner> winners,
                                    final List<String> sortedAwardGroups,
                                    final List<String> categoryOrder,
                                    final boolean displayChampionship)
      throws SQLException {
    final Map<String, Map<String, List<AwardWinner>>> organizedWinners = AwardsReport.organizeAwardWinners(winners);

    final List<String> fullOrder = new LinkedList<String>(categoryOrder);
    organizedWinners.keySet().forEach(c -> {
      if (!fullOrder.contains(c)) {
        fullOrder.add(c);
      }
    });
    for (final String categoryName : fullOrder) {
      if ((displayChampionship
          && ChampionshipCategory.CHAMPIONSHIP_AWARD_TITLE.equals(categoryName))
          || (!displayChampionship
              && !ChampionshipCategory.CHAMPIONSHIP_AWARD_TITLE.equals(categoryName))) {
        if (organizedWinners.containsKey(categoryName)) {
          final Map<String, List<AwardWinner>> categoryWinners = organizedWinners.get(categoryName);
          addSubjectiveAwardGroupWinners(connection, description, csv, categoryName, categoryWinners,
                                         sortedAwardGroups);
        }
      }
    }
  }

  private void addSubjectiveOverallWinners(final Connection connection,
                                           final ChallengeDescription description,
                                           final CSVWriter csv,
                                           final Tournament tournament)
      throws SQLException {

    final Map<String, List<OverallAwardWinner>> organizedWinners = AwardsReport.getNonNumericOverallWinners(description,
                                                                                                            connection,
                                                                                                            tournament);

    final List<String> categoryOrder = CategoriesIgnored.getNonNumericCategories(description, connection, tournament)
                                                        .stream() //
                                                        .map(NonNumericCategory::getTitle) //
                                                        .collect(Collectors.toList());
    final List<String> fullOrder = new LinkedList<String>(categoryOrder);
    organizedWinners.keySet().forEach(c -> {
      if (!fullOrder.contains(c)) {
        fullOrder.add(c);
      }
    });
    for (final String categoryName : fullOrder) {
      final List<OverallAwardWinner> categoryWinners = organizedWinners.getOrDefault(categoryName,
                                                                                     Collections.emptyList());
      if (!categoryWinners.isEmpty()) {
        addSubjectiveOverallWinners(connection, description, csv, categoryName, categoryWinners);
      }
    }
  }

  private void addSubjectiveOverallWinners(final Connection connection,
                                           final ChallengeDescription description,
                                           final CSVWriter csv,
                                           final String categoryName,
                                           final List<OverallAwardWinner> categoryWinners)
      throws SQLException {
    final boolean displayPlace = AwardsReport.displayPlace(description, categoryName);
    final String awardName = String.format("%s Award", categoryName);

    for (final OverallAwardWinner winner : categoryWinners) {

      final String placeText;
      if (displayPlace) {
        placeText = String.format("#%d", winner.getPlace());
      } else {
        placeText = "Winner";
      }

      final Team team = Team.getTeamFromDatabase(connection, winner.getTeamNumber());

      csv.writeNext(new String[] { String.valueOf(winner.getTeamNumber()), team.getTeamName(),
                                   String.format("%s - %s", awardName, placeText) });
    } // foreach winner
  }

  private void addSubjectiveAwardGroupWinners(final Connection connection,
                                              final ChallengeDescription description,
                                              final CSVWriter csv,
                                              final String categoryName,
                                              final Map<String, List<AwardWinner>> categoryWinners,
                                              final List<String> sortedGroups)
      throws SQLException {

    final boolean displayPlace = AwardsReport.displayPlace(description, categoryName);

    final String awardName = String.format("%s Award", categoryName);

    final List<String> localSortedGroups = new LinkedList<>(sortedGroups);
    categoryWinners.entrySet().stream().map(Map.Entry::getKey).filter(e -> !localSortedGroups.contains(e))
                   .forEach(localSortedGroups::add);

    for (final String group : localSortedGroups) {
      final String groupText;
      if (localSortedGroups.size() > 1) {
        groupText = String.format("%s ", group);
      } else {
        groupText = "";
      }

      if (categoryWinners.containsKey(group)) {
        final List<AwardWinner> agWinners = categoryWinners.get(group);

        if (!agWinners.isEmpty()) {
          for (final AwardWinner winner : agWinners) {

            final String placeText;
            if (displayPlace) {
              placeText = String.format("%s#%d", groupText, winner.getPlace());
            } else {
              placeText = group;
            }

            final int teamNumber = winner.getTeamNumber();
            final Team team = Team.getTeamFromDatabase(connection, teamNumber);

            csv.writeNext(new String[] { String.valueOf(teamNumber), team.getTeamName(),
                                         String.format("%s - %s", awardName, placeText) });
          } // foreach winner

        } // have winners in award group
      } // group exists
    } // foreach award group
  }

  private void addPerformance(final Connection connection,
                              final CSVWriter csv,
                              final ChallengeDescription description,
                              final List<String> sortedAwardGroups)
      throws SQLException, IOException {

    final String awardName = "Robot Performance Award";

    final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description, true,
                                                                                       false);

    // make sure all groups are in the sort
    final List<String> localSortedAwardGroups = new LinkedList<>(sortedAwardGroups);
    scores.entrySet().stream().map(Map.Entry::getKey).filter(e -> !localSortedAwardGroups.contains(e))
          .forEach(localSortedAwardGroups::add);

    for (final String group : localSortedAwardGroups) {
      if (scores.containsKey(group)) {
        final List<Top10.ScoreEntry> scoreList = scores.get(group);

        final Optional<Top10.ScoreEntry> firstWinner = scoreList.stream().findFirst();
        if (firstWinner.isPresent()) {
          final String topScore = firstWinner.get().getFormattedScore();

          final Collection<Top10.ScoreEntry> allWinners = scoreList.stream()
                                                                   .filter(e -> e.getFormattedScore().equals(topScore))
                                                                   .collect(Collectors.toList());

          for (Top10.ScoreEntry winner : allWinners) {
            final String awardText = String.format("%s - %s %s", awardName, (allWinners.size() > 1 ? "Tie" : "Winner"),
                                                   group);

            csv.writeNext(new String[] { String.valueOf(winner.getTeamNumber()), winner.getTeamName(), awardText,
                                         winner.getFormattedScore() });
          }
        } // have a winner
      } // group has scores
    } // foreach group
  }

  /**
   * Date format for reports.
   */
  public static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.MONTH_OF_YEAR,
                                                                                                    2)
                                                                                       .appendLiteral('/')
                                                                                       .appendValue(ChronoField.DAY_OF_MONTH,
                                                                                                    2)
                                                                                       .appendLiteral('/')
                                                                                       .appendValue(ChronoField.YEAR, 4)
                                                                                       .toFormatter();

  private static void addAdvancingTeams(final List<AdvancingTeam> advancing,
                                        final Connection connection,
                                        final CSVWriter csv,
                                        final Tournament tournament,
                                        final List<String> sortedGroups)
      throws SQLException {

    final String awardName;
    final TournamentLevel tournamentLevel = tournament.getLevel();
    if (TournamentLevel.NO_NEXT_LEVEL_ID != tournamentLevel.getNextLevelId()) {
      final TournamentLevel nextLevel = TournamentLevel.getById(connection, tournamentLevel.getNextLevelId());
      final int numTournamentsAtNextLevel = TournamentLevel.getNumTournamentsAtLevel(connection, nextLevel);
      awardName = String.format("Advancing to the %s tournament%s", nextLevel.getName(),
                                numTournamentsAtNextLevel > 1 ? "s" : "");
    } else {
      awardName = "Advancing to the next tournament";
    }

    final Map<String, List<AdvancingTeam>> organizedAdvancing = new HashMap<>();
    for (final AdvancingTeam advance : advancing) {
      final List<AdvancingTeam> agAdvancing = organizedAdvancing.computeIfAbsent(advance.getGroup(),
                                                                                 k -> new LinkedList<>());
      agAdvancing.add(advance);
    }

    final List<String> localSortedGroups = new LinkedList<>(sortedGroups);
    organizedAdvancing.entrySet().stream().map(Map.Entry::getKey).filter(e -> !localSortedGroups.contains(e))
                      .forEach(localSortedGroups::add);
    for (final String group : localSortedGroups) {
      if (organizedAdvancing.containsKey(group)) {
        final List<AdvancingTeam> groupAdvancing = organizedAdvancing.get(group);
        for (final AdvancingTeam winner : groupAdvancing) {
          final int teamNumber = winner.getTeamNumber();
          final Team team = Team.getTeamFromDatabase(connection, teamNumber);
          csv.writeNext(new String[] { String.valueOf(teamNumber), team.getTeamName(), awardName });
        }
      } // group exists
    } // foreach group
  }

}
