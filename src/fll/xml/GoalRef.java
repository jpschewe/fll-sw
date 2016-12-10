/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * A reference to a goal.
 */
public class GoalRef implements Evaluatable, Serializable {

  public static final String TAG_NAME = "goalRef";

  public static final String SCORE_TYPE_ATTRIBUTE = "scoreType";

  public static final String GOAL_ATTRIBUTE = "goal";

  public GoalRef(final Element ele,
                 final GoalScope scope) {
    mScoreType = GoalScoreType.fromString(ele.getAttribute(SCORE_TYPE_ATTRIBUTE));

    mGoalScope = scope;
    mGoalName = ele.getAttribute(GOAL_ATTRIBUTE);
  }

  private final String mGoalName;

  public String getGoalName() {
    return mGoalName;
  }

  private final GoalScope mGoalScope;

  public AbstractGoal getGoal() {
    return mGoalScope.getGoal(mGoalName);
  }

  private final GoalScoreType mScoreType;

  public GoalScoreType getScoreType() {
    return mScoreType;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    double value;
    switch (getScoreType()) {
    case COMPUTED:
      value = getGoal().getComputedScore(teamScore);
      break;
    case RAW:
      value = getGoal().getRawScore(teamScore);
      break;
    default:
      throw new FLLInternalException("Unknown score type: "
          + getScoreType());
    }

    return value;
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(SCORE_TYPE_ATTRIBUTE, mScoreType.toXmlString());
    ele.setAttribute(GOAL_ATTRIBUTE, mGoalName);
    return ele;
  }
}
