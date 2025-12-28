/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import fll.Tournament;
import fll.TournamentTeam;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.db.CategoriesIgnored;
import fll.db.OverallAwardWinner;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.report.awards.AwardsScriptReport;
import fll.web.report.awards.ChampionshipCategory;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Support code for edit-award-winners.jsp.
 */
public final class EditAwardWinners {

  private EditAwardWinners() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Award type for {@link ChampionshipCategory#CHAMPIONSHIP_AWARD_TITLE}.
   */
  public static final String CHAMPIONSHIP_AWARD_TYPE = "championship";

  /**
   * Award type for non-numeric awards.
   */
  public static final String NON_NUMERIC_AWARD_TYPE = "non-numeric";

  /**
   * Award type for subjective challenge awards.
   */
  public static final String SUBJECTIVE_AWARD_TYPE = "subjective";

  /**
   * Award type for virtual subjective challenge awards.
   */
  public static final String VIRTUAL_SUBJECTIVE_AWARD_TYPE = "virtualSubjective";

  /**
   * Setup variables for edit-award-winers.jsp.
   * 
   * @param application application variables
   * @param page page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final List<NonNumericCategory> nonNumericCategories = CategoriesIgnored.getNonNumericCategories(description,
                                                                                                      connection,
                                                                                                      tournament);
      page.setAttribute("nonNumericCategories", nonNumericCategories);

      final List<String> awardGroups = AwardsScriptReport.getAwardGroupOrder(connection, tournament);
      page.setAttribute("awardGroups", awardGroups);

      final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, tournament.getTournamentID());
      page.setAttribute("teams", teams);

      final List<AwardWinner> subjectiveAwardWinnersList = AwardWinners.getSubjectiveAwardWinners(connection,
                                                                                                  tournament.getTournamentID());

      // category -> awardGroup -> winners
      final Map<String, Map<String, List<AwardWinner>>> awardGroupAwardWinners = new HashMap<>();

      for (final AwardWinner winner : subjectiveAwardWinnersList) {
        final Map<String, List<AwardWinner>> awardGroupToWinners = awardGroupAwardWinners.computeIfAbsent(winner.getName(),
                                                                                                          k -> new HashMap<>());
        awardGroupToWinners.computeIfAbsent(winner.getAwardGroup(), k -> new LinkedList<>()).add(winner);
      }

      final List<AwardWinner> virtualSubjectiveAwardWinnersList = AwardWinners.getVirtualSubjectiveAwardWinners(connection,
                                                                                                                tournament.getTournamentID());
      for (final AwardWinner winner : virtualSubjectiveAwardWinnersList) {
        final Map<String, List<AwardWinner>> awardGroupToWinners = awardGroupAwardWinners.computeIfAbsent(winner.getName(),
                                                                                                          k -> new HashMap<>());
        awardGroupToWinners.computeIfAbsent(winner.getAwardGroup(), k -> new LinkedList<>()).add(winner);
      }

      final List<AwardWinner> extraAwardWinnersList = AwardWinners.getNonNumericAwardWinners(connection,
                                                                                             tournament.getTournamentID());
      for (final AwardWinner winner : extraAwardWinnersList) {
        final Map<String, List<AwardWinner>> awardGroupToWinners = awardGroupAwardWinners.computeIfAbsent(winner.getName(),
                                                                                                          k -> new HashMap<>());
        awardGroupToWinners.computeIfAbsent(winner.getAwardGroup(), k -> new LinkedList<>()).add(winner);
      }

      final List<OverallAwardWinner> overallAwardWinnersList = AwardWinners.getNonNumericOverallAwardWinners(connection,
                                                                                                             tournament.getTournamentID());
      // category -> winners
      final Map<String, List<OverallAwardWinner>> overallAwardWinners = new HashMap<>();
      for (final OverallAwardWinner winner : overallAwardWinnersList) {
        overallAwardWinners.computeIfAbsent(winner.getName(), k -> new LinkedList<>()).add(winner);
      }
      page.setAttribute("overallAwardWinners", overallAwardWinners);

      page.setAttribute("awardGroupAwardWinners", awardGroupAwardWinners);

      page.setAttribute("championshipAwardName", ChampionshipCategory.CHAMPIONSHIP_AWARD_TITLE);
      page.setAttribute("championshipAwardType", CHAMPIONSHIP_AWARD_TYPE);
      page.setAttribute("nonNumericAwardType", NON_NUMERIC_AWARD_TYPE);
      page.setAttribute("subjectiveAwardType", SUBJECTIVE_AWARD_TYPE);
      page.setAttribute("virtualSubjectiveAwardType", VIRTUAL_SUBJECTIVE_AWARD_TYPE);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new FLLRuntimeException(e);
    }

  }

}
