/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

/**
 * Dummy team score implementation for testing.
 * 
 * @author jpschewe
 * @version $Revision$
 *
 */
public class DummyTeamScore extends TeamScore {

  public DummyTeamScore(final Element categoryElement, final int teamNumber, final int runNumber, final Map<String, Double> simpleGoals, final Map<String, String> enumGoals) {
    super(categoryElement, teamNumber, runNumber);
     _simpleGoals = new HashMap<String, Double>(simpleGoals);
    _enumGoals = new HashMap<String, String>(enumGoals);
  }
  
  /* (non-Javadoc)
   * @see fll.web.playoff.TeamScore#getEnumRawScore(java.lang.String)
   */
  @Override
  public String getEnumRawScore(final String goalName) {
    if(_enumGoals.containsKey(goalName)) {
      return _enumGoals.get(goalName);
    } else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see fll.web.playoff.TeamScore#getRawScore(java.lang.String)
   */
  @Override
  public Double getRawScore(final String goalName) {
    if(_simpleGoals.containsKey(goalName)) {
      return _simpleGoals.get(goalName);
    } else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see fll.web.playoff.TeamScore#isNoShow()
   */
  @Override
  public boolean isNoShow() {
    return false;
  }

  /* (non-Javadoc)
   * @see fll.web.playoff.TeamScore#scoreExists()
   */
  @Override
  public boolean scoreExists() {
    return true;
  }
  
  private final Map<String, Double> _simpleGoals;
  
  private final Map<String, String> _enumGoals;
}
