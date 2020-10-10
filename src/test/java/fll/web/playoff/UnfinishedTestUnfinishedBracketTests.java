/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fll.Team;
import fll.db.GlobalParameters;
import fll.util.DummyTeamScore;
import fll.xml.ChallengeDescription;

/**
 * Non-parameterized unfinished bracket tests.
 */
public final class UnfinishedTestUnfinishedBracketTests extends UnfinishedBaseTest {

  /**
   * Check that a team in an unfinished playoff bracket is properly noted.
   * 
   * @throws SQLException internal test error
   */
  @Test
  public void testTeamInUnfinished() throws SQLException {
    final Team team = Team.getTeamFromDatabase(getConnection(), UnfinishedBaseTest.UNFINISHED_TEAM_NUMBER);
    assertThat(team, notNullValue());

    final List<Integer> teamNumbers = Collections.singletonList(team.getTeamNumber());
    final String errors = Playoff.involvedInUnfinishedPlayoff(getConnection(), getTournament().getTournamentID(),
                                                              teamNumbers);
    assertThat(errors, notNullValue());
  }

  /**
   * Check that {@link Playoff#getUnfinishedPlayoffBrackets(Connection, int)}
   * lists only the unfinished brackets.
   * 
   * @throws SQLException internal test error
   */
  @Test
  public void testUnfinishedByName() throws SQLException {
    final List<String> actual = Playoff.getUnfinishedPlayoffBrackets(getConnection(),
                                                                     getTournament().getTournamentID());

    assertThat(actual, hasSize(UnfinishedBaseTest.UNFINISHED_BRACKET_NAMES.length));

    for (final String bracketName : UnfinishedBaseTest.UNFINISHED_BRACKET_NAMES) {
      assertThat(bracketName, actual, hasItem(bracketName));
    }
  }

  /**
   * Check that the dummy score created by
   * {@link Playoff#populateInitialScoreMaps(ChallengeDescription, Map, Map)} can
   * be evaluated without error.
   * 
   * @throws RuntimeException internal test error
   * @throws SQLException internal test error
   */
  @Test
  public void testDummyScore() throws SQLException, RuntimeException {
    final ChallengeDescription challenge = GlobalParameters.getChallengeDescription(getConnection());
    assertThat(challenge, notNullValue());

    final Map<String, Double> simpleGoals = new HashMap<>();
    final Map<String, String> enumGoals = new HashMap<>();

    Playoff.populateInitialScoreMaps(challenge, simpleGoals, enumGoals);

    final TeamScore teamScore = new DummyTeamScore(UnfinishedBaseTest.UNFINISHED_TEAM_NUMBER, 1, simpleGoals, enumGoals);

    challenge.getPerformance().evaluate(teamScore);
  }

}