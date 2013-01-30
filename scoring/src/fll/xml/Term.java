/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * Term in a polynomial.
 */
public class Term extends AbstractTerm {

  public Term(final Element ele,
              final GoalScope scope) {
    super(ele);
    mScoreType = GoalScoreType.fromString(ele.getAttribute("scoreType"));

    mGoalScope = scope;
    mGoalName = ele.getAttribute("goal");
  }

  private final String mGoalName;

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

    value = value
        * getCoefficient();

    return applyFloatingPointType(value);
  }

}
