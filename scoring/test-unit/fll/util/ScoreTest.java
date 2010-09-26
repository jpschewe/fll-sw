/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.xml.ChallengeParser;

/**
 * Test score computations.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public class ScoreTest {

  private static final Logger LOG = LogUtils.getLogger();

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  private Element loadDocumentAndGetPerformanceElemnt(final InputStream stream) {
    Assert.assertNotNull(stream);
    final Document document = ChallengeParser.parse(new InputStreamReader(stream));
    Assert.assertNotNull(document);
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
    Assert.assertNotNull(performanceElement);
    return performanceElement;
  }
  
  /**
   * Test computed goals. Loads data/test-computed-goal.xml and uses a dummy
   * team score object.
   */
  @Test
  public void testComputedGoals() throws ParseException {
    final InputStream stream = ScoreTest.class.getResourceAsStream("data/test-computed-goal.xml");
    final Element performanceElement = loadDocumentAndGetPerformanceElemnt(stream);

    final Map<String, Double> simpleGoals = new HashMap<String, Double>();
    final Map<String, String> enumGoals = new HashMap<String, String>();

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 2.0);
    enumGoals.put("robot_type", "rcx");
    DummyTeamScore dummyTeamScore = new DummyTeamScore(performanceElement, 0, 1, simpleGoals, enumGoals);
    double score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(260, score, 0);

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 2.0);
    enumGoals.put("robot_type", "nxt");
    dummyTeamScore = new DummyTeamScore(performanceElement, 1, 1, simpleGoals, enumGoals);
    score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(100, score, 0);

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 1.0);
    enumGoals.put("robot_type", "rcx");
    dummyTeamScore = new DummyTeamScore(performanceElement, 2, 1, simpleGoals, enumGoals);
    score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(230, score, 0);

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 1.0);
    enumGoals.put("robot_type", "nxt");
    dummyTeamScore = new DummyTeamScore(performanceElement, 3, 1, simpleGoals, enumGoals);
    score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(85, score, 0);
  }

  /**
   * Test computed goals. Loads data/test-variables.xml and uses a dummy team
   * score object.
   */
  @Test
  public void testVariables() throws ParseException {
    final InputStream stream = ScoreTest.class.getResourceAsStream("data/test-variables.xml");
    final Element performanceElement = loadDocumentAndGetPerformanceElemnt(stream);

    final Map<String, Double> simpleGoals = new HashMap<String, Double>();
    final Map<String, String> enumGoals = new HashMap<String, String>();

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 3.0);
    enumGoals.put("robot_type", "rcx");
    DummyTeamScore dummyTeamScore = new DummyTeamScore(performanceElement, 0, 1, simpleGoals, enumGoals);
    Assert.assertEquals(269, dummyTeamScore.getComputedScore("computed"), 0);
    double score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(384, score, 0);

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 3.0);
    enumGoals.put("robot_type", "nxt");
    dummyTeamScore = new DummyTeamScore(performanceElement, 1, 1, simpleGoals, enumGoals);
    Assert.assertEquals(0, dummyTeamScore.getComputedScore("computed"), 0);
    score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(115, score, 0);

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 1.0);
    enumGoals.put("robot_type", "rcx");
    dummyTeamScore = new DummyTeamScore(performanceElement, 2, 1, simpleGoals, enumGoals);
    Assert.assertEquals(131, dummyTeamScore.getComputedScore("computed"), 0);
    score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(216, score, 0);

    simpleGoals.put("pump_station", 1.0);
    simpleGoals.put("flags", 1.0);
    simpleGoals.put("flags_rows", 1.0);
    enumGoals.put("robot_type", "nxt");
    dummyTeamScore = new DummyTeamScore(performanceElement, 3, 1, simpleGoals, enumGoals);
    Assert.assertEquals(0, dummyTeamScore.getComputedScore("computed"), 0);
    score = ScoreUtils.computeTotalScore(dummyTeamScore);
    Assert.assertEquals(85, score, 0);
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
          + finalists + " schedule: " + schedule);
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
        Assert.assertTrue(team
            + " is not scheduled for " + category, found);
      }
    }

    // check that each time slot does not contain the same team twice
    for (final Map<String, Integer> timeSlot : schedule) {
      final Collection<Integer> teamsSeen = new LinkedList<Integer>();
      for (final Integer team : timeSlot.values()) {
        Assert.assertFalse(team
            + " appears twice in a time slot in the schedule: " + timeSlot, teamsSeen.contains(team));
        teamsSeen.add(team);
      }
    }
  }
}
