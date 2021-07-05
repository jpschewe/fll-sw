/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.TournamentTeam;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.db.OverallAwardWinner;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;

/**
 * Support code for edit-award-winners.jsp.
 */
public final class EditAwardWinners {

  private EditAwardWinners() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  public static void populateContext(final ServletContext application,
                                     final PageContext page) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Collection<String> awardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());
      page.setAttribute("awardGroups", awardGroups);

      final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, tournament.getTournamentID());
      page.setAttribute("teams", teams);

      final List<AwardWinner> subjectiveAwardWinnersList = AwardWinners.getSubjectiveAwardWinners(connection,
                                                                                                 tournament.getTournamentID());
      // category -> awardGroup -> winners
      final Map<String, Map<String, List<AwardWinner>>> subjectiveAwardWinners = new HashMap<>();
      for (final AwardWinner winner : subjectiveAwardWinnersList) {
        final Map<String, List<AwardWinner>> awardGroupToWinners = subjectiveAwardWinners.computeIfAbsent(winner.getName(),
                                                                                                          k -> new HashMap<>());
        awardGroupToWinners.computeIfAbsent(winner.getAwardGroup(), k -> new LinkedList<>()).add(winner);
      }
      page.setAttribute("subjectiveAwardWinners", subjectiveAwardWinners);

      final List<AwardWinner> extraAwardWinnersList = AwardWinners.getNonNumericAwardWinners(connection,
                                                                                        tournament.getTournamentID());
      // category -> awardGroup -> winners
      final Map<String, Map<String, List<AwardWinner>>> extraAwardWinners = new HashMap<>();
      for (final AwardWinner winner : extraAwardWinnersList) {
        final Map<String, List<AwardWinner>> awardGroupToWinners = extraAwardWinners.computeIfAbsent(winner.getName(),
                                                                                                     k -> new HashMap<>());
        awardGroupToWinners.computeIfAbsent(winner.getAwardGroup(), k -> new LinkedList<>()).add(winner);
      }
      page.setAttribute("extraAwardWinners", extraAwardWinners);

      final List<OverallAwardWinner> overallAwardWinnersList = AwardWinners.getNonNumericOverallAwardWinners(connection,
                                                                                                   tournament.getTournamentID());
      // category -> winners
      final Map<String, List<OverallAwardWinner>> overallAwardWinners = new HashMap<>();
      for (final OverallAwardWinner winner : overallAwardWinnersList) {
        overallAwardWinners.computeIfAbsent(winner.getName(), k -> new LinkedList<>()).add(winner);
      }
      page.setAttribute("overallAwardWinners", overallAwardWinners);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new FLLRuntimeException(e);
    }

  }

}
