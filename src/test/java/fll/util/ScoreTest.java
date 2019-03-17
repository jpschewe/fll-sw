/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;

import fll.TestUtils;
import fll.Utilities;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ComputedGoal;
import fll.xml.PerformanceScoreCategory;

/**
 * Test score computations.
 * 
 * @author jpschewe
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class ScoreTest {

  private static final Logger LOG = LogUtils.getLogger();

  private PerformanceScoreCategory loadDocumentAndGetPerformanceElemnt(final InputStream stream) {
    assertNotNull(stream);
    final Document document = ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
    assertNotNull(document);
    final ChallengeDescription desc = new ChallengeDescription(document.getDocumentElement());
    final PerformanceScoreCategory performanceElement = desc.getPerformance();
    assertNotNull(performanceElement);
    return performanceElement;
  }

  /**
   * Test computed goals. Loads data/test-computed-goal.xml and uses a dummy
   * team score object.
   */
  @Test
  public void testComputedGoals() throws ParseException, IOException {
    try (InputStream stream = ScoreTest.class.getResourceAsStream("data/test-computed-goal.xml")) {
      final PerformanceScoreCategory performanceElement = loadDocumentAndGetPerformanceElemnt(stream);

      final Map<String, Double> simpleGoals = new HashMap<String, Double>();
      final Map<String, String> enumGoals = new HashMap<String, String>();

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 2.0);
      enumGoals.put("robot_type", "rcx");
      DummyTeamScore dummyTeamScore = new DummyTeamScore(0, 1, simpleGoals, enumGoals);
      double score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(260, score, 0);

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 2.0);
      enumGoals.put("robot_type", "nxt");
      dummyTeamScore = new DummyTeamScore(1, 1, simpleGoals, enumGoals);
      score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(100, score, 0);

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 1.0);
      enumGoals.put("robot_type", "rcx");
      dummyTeamScore = new DummyTeamScore(2, 1, simpleGoals, enumGoals);
      score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(230, score, 0);

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 1.0);
      enumGoals.put("robot_type", "nxt");
      dummyTeamScore = new DummyTeamScore(3, 1, simpleGoals, enumGoals);
      score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(85, score, 0);
    }
  }

  /**
   * Test computed goals. Loads data/test-variables.xml and uses a dummy team
   * score object.
   */
  @Test
  public void testVariables() throws ParseException, IOException {
    try (InputStream stream = ScoreTest.class.getResourceAsStream("data/test-variables.xml")) {
      final PerformanceScoreCategory performanceElement = loadDocumentAndGetPerformanceElemnt(stream);

      final Map<String, Double> simpleGoals = new HashMap<String, Double>();
      final Map<String, String> enumGoals = new HashMap<String, String>();

      ComputedGoal computed = null;
      for (final AbstractGoal g : performanceElement.getGoals()) {
        if (g.isComputed()
            && "computed".equals(g.getName())) {
          computed = (ComputedGoal) g;
        }
      }
      assertNotNull(computed);

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 3.0);
      enumGoals.put("robot_type", "rcx");
      DummyTeamScore dummyTeamScore = new DummyTeamScore(0, 1, simpleGoals, enumGoals);
      assertEquals(269, computed.getComputedScore(dummyTeamScore), 0);
      double score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(384, score, 0);

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 3.0);
      enumGoals.put("robot_type", "nxt");
      dummyTeamScore = new DummyTeamScore(1, 1, simpleGoals, enumGoals);
      assertEquals(0, computed.getComputedScore(dummyTeamScore), 0);
      score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(115, score, 0);

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 1.0);
      enumGoals.put("robot_type", "rcx");
      dummyTeamScore = new DummyTeamScore(2, 1, simpleGoals, enumGoals);
      assertEquals(131, computed.getComputedScore(dummyTeamScore), 0);
      score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(216, score, 0);

      simpleGoals.put("pump_station", 1.0);
      simpleGoals.put("flags", 1.0);
      simpleGoals.put("flags_rows", 1.0);
      enumGoals.put("robot_type", "nxt");
      dummyTeamScore = new DummyTeamScore(3, 1, simpleGoals, enumGoals);
      assertEquals(0, computed.getComputedScore(dummyTeamScore), 0);
      score = performanceElement.evaluate(dummyTeamScore);
      assertEquals(85, score, 0);
    }
  }

  /**
   * Test scheduling finalists.
   */
  @Test
  public void testScheduleFinalists() {
    final Map<String, Collection<Integer>> finalists = new HashMap<String, Collection<Integer>>();
    // using data from division 1 of State from teh 2007 season.
    finalists.put("programming", Arrays.asList(2682, 533, 3564, 6162));
    finalists.put("design", Arrays.asList(6162, 2682, 3564, 533));
    finalists.put("teamwork", Arrays.asList(2682, 4706, 5890, 1452));
    finalists.put("research", Arrays.asList(5568, 1452, 2683, 3517));

    final List<Map<String, Integer>> schedule = ScoreUtils.scheduleFinalists(finalists);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Finalists: "
          + finalists
          + " schedule: "
          + schedule);
    }

    // check that each team in finalists is in the schedule
    for (final Map.Entry<String, Collection<Integer>> entry : finalists.entrySet()) {
      final String category = entry.getKey();
      for (final Integer team : entry.getValue()) {

        // find the team in the schedule
        boolean found = false;
        final Iterator<Map<String, Integer>> iter = schedule.iterator();
        while (iter.hasNext()
            && !found) {
          final Map<String, Integer> timeSlot = iter.next();
          if (team.equals(timeSlot.get(category))) {
            found = true;
          }
        }
        assertTrue(found, team
            + " is not scheduled for "
            + category);
      }
    }

    // check that each time slot does not contain the same team twice
    for (final Map<String, Integer> timeSlot : schedule) {
      final Collection<Integer> teamsSeen = new LinkedList<Integer>();
      for (final Integer team : timeSlot.values()) {
        assertFalse(teamsSeen.contains(team), team
            + " appears twice in a time slot in the schedule: "
            + timeSlot);
        teamsSeen.add(team);
      }
    }
  }
}
