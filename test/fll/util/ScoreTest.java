/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.xml.ChallengeParser;

/**
 * Test score computations.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class ScoreTest extends TestCase {

  /**
   * Test computed goals. Loads data/test-computed-goal.xml and uses a dummy
   * team score object.
   */
  @Test
  public void testComputedGoals() throws ParseException {
    final InputStream stream = ScoreTest.class.getResourceAsStream("data/test-computed-goal.xml");
    Assert.assertNotNull(stream);
    final Document document = ChallengeParser.parse(new InputStreamReader(stream));
    Assert.assertNotNull(document);
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    Assert.assertNotNull(performanceElement);
    
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
   * Test computed goals. Loads data/test-variables.xml and uses a dummy
   * team score object.
   */
  @Test
  public void testVariables() throws ParseException {
    final InputStream stream = ScoreTest.class.getResourceAsStream("data/test-variables.xml");
    Assert.assertNotNull(stream);
    final Document document = ChallengeParser.parse(new InputStreamReader(stream));
    Assert.assertNotNull(document);
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    Assert.assertNotNull(performanceElement);
    
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
}
